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


function [status, message] = ShapeDescription_PCA(xml_doc_path)
%% Estimate centroid and principal axes of binarized P(cell) and P(contact)
%-- Input data
%   (1) BW(P(cell)) or BW(P(contact)) in FITS format (uint8): 255 for
%       foreground and 0 for background
%-- Output data
%   (1) Centroid and principal axes information in csv file (in physical
%       unit)

status = 0;
message = [];

%% Read XML file
% Import the XPath classes
import javax.xml.xpath.*

% Construct the DOM.
xmlDocument = xmlread(xml_doc_path);

% Create an XPath expression.
factory = XPathFactory.newInstance;
xPath = factory.newXPath;

% Read paths
output_filename = xPath.compile('/ShapeDescription_PCA/output/filename').evaluate(xmlDocument, XPathConstants.STRING);
input_binaryDirectory = xPath.compile('/ShapeDescription_PCA/input/binaryDirectory').evaluate(xmlDocument, XPathConstants.STRING);
input_binaryPattern = xPath.compile('/ShapeDescription_PCA/input/binaryPattern').evaluate(xmlDocument, XPathConstants.STRING);

% Parameters
parameters_CDELT1 = xPath.compile('/ShapeDescription_PCA/parameters/CDELT1').evaluate(xmlDocument, XPathConstants.NUMBER); % length of each pixel along axis 1
parameters_CDELT2 = xPath.compile('/ShapeDescription_PCA/parameters/CDELT2').evaluate(xmlDocument, XPathConstants.NUMBER); % length of each pixel along axis 2
parameters_CDELT3 = xPath.compile('/ShapeDescription_PCA/parameters/CDELT3').evaluate(xmlDocument, XPathConstants.NUMBER); % length of each pixel along axis 3
parameters_unit = xPath.compile('/ShapeDescription_PCA/parameters/unit').evaluate(xmlDocument, XPathConstants.STRING); % unit

% Check if all necessary inputs are available
if isempty(output_filename)
    status = 1;
    message = 'Error: output_filename is missing.';
    return
end

if isempty(input_binaryDirectory)
    status = 1;
    message = 'Error: input_binaryDirectory is missing.';
    return
end

if isempty(input_binaryPattern)
    status = 1;
    message = 'Error: input_binaryPattern is missing.';
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

list = dir([input_binaryDirectory filesep input_binaryPattern]);
nD = size(list,1);

% Output directory
[output_directory,~,~] = fileparts(output_filename);

if ~exist(output_directory, 'dir')
    mkdir(output_directory);
end

Shape = struct('ImageName',cell(nD,1), 'CentroidX',zeros(nD,1), 'CentroidY',zeros(nD,1), 'CentroidZ',zeros(nD,1), ...
        'PrincipalAxesEigenValue1',zeros(nD,1), 'PrincipalAxesEigenValue2',zeros(nD,1), 'PrincipalAxesEigenValue3',zeros(nD,1), ...
        'PrincipalAxesEigenVector1X',zeros(nD,1), 'PrincipalAxesEigenVector1Y',zeros(nD,1), 'PrincipalAxesEigenVector1Z',zeros(nD,1), ...
        'PrincipalAxesEigenVector2X',zeros(nD,1), 'PrincipalAxesEigenVector2Y',zeros(nD,1), 'PrincipalAxesEigenVector2Z',zeros(nD,1), ...
        'PrincipalAxesEigenVector3X',zeros(nD,1), 'PrincipalAxesEigenVector3Y',zeros(nD,1), 'PrincipalAxesEigenVector3Z',zeros(nD,1), ...
        'Unit',cell(nD,1));

for kd = 1:nD
    fname = list(kd).name;
    [~,dirname,~] = fileparts(fname);
    
    disp(['-- ' num2str(kd) '/' num2str(nD) ': ' dirname]);

    % Load binary mask
    BW = fitsread([input_binaryDirectory filesep fname]);
    BW = convert_fits2mat(BW, 'uint8', 'binary');
  
    [L,H,W] = size(BW);
        
    % Centroid
    indPos = find(BW > 0);
    [z,y,x] = ind2sub([L H W], indPos);

    x = x * parameters_CDELT1;
    y = y * parameters_CDELT2;
    z = z * parameters_CDELT3;
            
    ctr_x = mean(x);
    ctr_y = mean(y);
    ctr_z = mean(z);

    % Principal axes
    x = x - ctr_x;
    y = y - ctr_y;
    z = z - ctr_z;

    [V, ~, E] = pca([x y z], 'Centered', false);

    % Shape description        
    Shape(kd).ImageName = dirname;
    Shape(kd).CentroidX = ctr_x;
    Shape(kd).CentroidY = ctr_y;
    Shape(kd).CentroidZ = ctr_z;
    Shape(kd).PrincipalAxesEigenValue1 = E(1);
    Shape(kd).PrincipalAxesEigenValue2 = E(2);
    Shape(kd).PrincipalAxesEigenValue3 = E(3);
    Shape(kd).PrincipalAxesEigenVector1X = V(1,1);
    Shape(kd).PrincipalAxesEigenVector1Y = V(2,1);
    Shape(kd).PrincipalAxesEigenVector1Z = V(3,1);
    Shape(kd).PrincipalAxesEigenVector2X = V(1,2);
    Shape(kd).PrincipalAxesEigenVector2Y = V(2,2);
    Shape(kd).PrincipalAxesEigenVector2Z = V(3,2);
    Shape(kd).PrincipalAxesEigenVector3X = V(1,3);
    Shape(kd).PrincipalAxesEigenVector3Y = V(2,3);
    Shape(kd).PrincipalAxesEigenVector3Z = V(3,3);
    Shape(kd).Unit = parameters_unit;
end

Shape_table = struct2table(Shape);
writetable(Shape_table, output_filename);