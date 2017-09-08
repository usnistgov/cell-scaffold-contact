% This software was developed by employees of the National Institute of
% Standards and Technology (NIST), an agency of the Federal Government.
% Pursuant to title 17 United States Code Section 105, works of NIST
% employees are not subject to copyright protection in the United States
% and are considered to be in the public domain. Permission to freely use,
% copy, modify, and distribute this software and its documentation without
% fee is hereby granted, provided that this notice and disclaimer of
% warranty appears in all copies.
% THE SOFTWARE IS PROVIDED 'AS IS' WITHOUT ANY WARRANTY OF ANY KIND, EITHER
% EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT LIMITED TO, ANY
% WARRANTY THAT THE SOFTWARE WILL CONFORM TO SPECIFICATIONS, ANY IMPLIED
% WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND
% FREEDOM FROM INFRINGEMENT, AND ANY WARRANTY THAT THE DOCUMENTATION WILL
% CONFORM TO THE SOFTWARE, OR ANY WARRANTY THAT THE SOFTWARE WILL BE ERROR
% FREE. IN NO EVENT SHALL NIST BE LIABLE FOR ANY DAMAGES, INCLUDING, BUT
% NOT LIMITED TO, DIRECT, INDIRECT, SPECIAL OR CONSEQUENTIAL DAMAGES,
% ARISING OUT OF, RESULTING FROM, OR IN ANY WAY CONNECTED WITH THIS
% SOFTWARE, WHETHER OR NOT BASED UPON WARRANTY, CONTRACT, TORT, OR
% OTHERWISE, WHETHER OR NOT INJURY WAS SUSTAINED BY PERSONS OR PROPERTY OR
% OTHERWISE, AND WHETHER OR NOT LOSS WAS SUSTAINED FROM, OR AROSE OUT OF
% THE RESULTS OF, OR USE OF, THE SOFTWARE OR SERVICES PROVIDED HEREUNDER.


function [status, message] = ShapeDescription_WeightedInertia(xml_doc_path)
%% Estimate weighted centroid and principal axes of P(cell) and P(contact) from weighted inertia
%-- Input data
%   (1) P(cell) or P(contact) in FITS format (int16)
%-- Output data
%   (1) Centroid and principal axes information in csv file

status = 0;
message = [];

addpath('common');

%% Read XML file
% Import the XPath classes
import javax.xml.xpath.*

% Construct the DOM.
xmlDocument = xmlread(which(xml_doc_path));

% Create an XPath expression.
factory = XPathFactory.newInstance;
xPath = factory.newXPath;

% Read paths
output_filename = xPath.compile('/ShapeDescription_WeightedInertia/output/filename').evaluate(xmlDocument, XPathConstants.STRING);
input_probDirectory = xPath.compile('/ShapeDescription_WeightedInertia/input/probDirectory').evaluate(xmlDocument, XPathConstants.STRING);
input_probPattern = xPath.compile('/ShapeDescription_WeightedInertia/input/probPattern').evaluate(xmlDocument, XPathConstants.STRING);

% Parameters
parameters_CDELT1 = xPath.compile('/ShapeDescription_WeightedInertia/parameters/CDELT1').evaluate(xmlDocument, XPathConstants.NUMBER); % length of each pixel along axis 1
parameters_CDELT2 = xPath.compile('/ShapeDescription_WeightedInertia/parameters/CDELT2').evaluate(xmlDocument, XPathConstants.NUMBER); % length of each pixel along axis 2
parameters_CDELT3 = xPath.compile('/ShapeDescription_WeightedInertia/parameters/CDELT3').evaluate(xmlDocument, XPathConstants.NUMBER); % length of each pixel along axis 3
parameters_unit = xPath.compile('/ShapeDescription_WeightedInertia/parameters/unit').evaluate(xmlDocument, XPathConstants.STRING); % unit

% Check if all necessary inputs are available
if isempty(output_filename)
    status = 1;
    message = 'Error: output_filename is missing.';
    return
end

if isempty(input_probDirectory)
    status = 1;
    message = 'Error: input_probDirectory is missing.';
    return
end

if isempty(input_probPattern)
    status = 1;
    message = 'Error: input_probPattern is missing.';
    return
end

if isnan(parameters_CDELT1)
    status = 1;
    message = 'Error: parameters_CDELT1 is missing.';
    return
end

if isnan(parameters_CDELT2)
    status = 1;
    message = 'Error: parameters_CDELT2 is missing.';
    return
end

if isnan(parameters_CDELT3)
    status = 1;
    message = 'Error: parameters_CDELT3 is missing.';
    return
end

if isempty(parameters_unit)
    status = 1;
    message = 'Error: parameters_unit is missing.';
    return
end


%% Estimate centroid and principal axes of probability cloud
disp('Estimating centroid and principal axes...');

list = dir([input_probDirectory filesep input_probPattern]);
nD = size(list,1);

% Output directory
[output_directory,~,~] = fileparts(output_filename);

if ~exist(output_directory, 'dir')
    mkdir(output_directory);
end

Shape = struct('ImageName',cell(nD,1), 'CentroidX',zeros(nD,1), 'CentroidY',zeros(nD,1), 'CentroidZ',zeros(nD,1), ...
        'EigenValue1',zeros(nD,1), 'EigenValue2',zeros(nD,1), 'EigenValue3',zeros(nD,1), ...
        'EigenVector1X',zeros(nD,1), 'EigenVector1Y',zeros(nD,1), 'EigenVector1Z',zeros(nD,1), ...
        'EigenVector2X',zeros(nD,1), 'EigenVector2Y',zeros(nD,1), 'EigenVector2Z',zeros(nD,1), ...
        'EigenVector3X',zeros(nD,1), 'EigenVector3Y',zeros(nD,1), 'EigenVector3Z',zeros(nD,1), ...
        'Unit',cell(nD,1));

for kd = 1:nD
    fname = list(kd).name;
    [~,dirname,~] = fileparts(fname);
    
    disp(['-- ' num2str(kd) '/' num2str(nD) ': ' dirname]);

    % Load probability
    P = fitsread([input_probDirectory filesep fname]);
    P = convert_fits2mat(P, 'int16', 'probability');
  
    [L,H,W] = size(P);
    
    % Centroid
    indPos = find(P > 0);
    [z,y,x] = ind2sub([L H W], indPos);
    P = P(indPos);
            
    x = x * parameters_CDELT1;
    y = y * parameters_CDELT2;
    z = z * parameters_CDELT3;

    S = sum(P);
    ctr_x = sum(P .* x) / S;
    ctr_y = sum(P .* y) / S;
    ctr_z = sum(P .* z) / S;

    % Inertia
    x = x - ctr_x;
    y = y - ctr_y;
    z = z - ctr_z;

    Ixx = sum( P .* (y.^2 + z.^2) );
    Ixy = -sum( P .* (x .* y) );
    Ixz = -sum( P .* (x .* z) );

    Iyx = -sum( P .* (y .* x) );
    Iyy = sum( P .* (x.^2 + z.^2) );
    Iyz = -sum( P .* (y .* z) );

    Izx = -sum( P .* (z .* x) );
    Izy = -sum( P .* (z .* y) );
    Izz = sum( P .* (x.^2 + y.^2) );

    It = [Ixx Ixy Ixz; Iyx Iyy Iyz; Izx Izy Izz];
    It = It / length(indPos);

    [V, D] = eig(It);
    E = diag(D);
    
    % Shape description        
    Shape(kd).ImageName = dirname;
    Shape(kd).CentroidX = ctr_x;
    Shape(kd).CentroidY = ctr_y;
    Shape(kd).CentroidZ = ctr_z;
    Shape(kd).EigenValue1 = E(1);
    Shape(kd).EigenValue2 = E(2);
    Shape(kd).EigenValue3 = E(3);
    Shape(kd).EigenVector1X = V(1,1);
    Shape(kd).EigenVector1Y = V(2,1);
    Shape(kd).EigenVector1Z = V(3,1);
    Shape(kd).EigenVector2X = V(1,2);
    Shape(kd).EigenVector2Y = V(2,2);
    Shape(kd).EigenVector2Z = V(3,2);
    Shape(kd).EigenVector3X = V(1,3);
    Shape(kd).EigenVector3Y = V(2,3);
    Shape(kd).EigenVector3Z = V(3,3);
    Shape(kd).Unit = parameters_unit;
end

Shape_table = struct2table(Shape);
writetable(Shape_table, output_filename);