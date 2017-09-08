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


function [status, message] = BinaryAggregation(xml_doc_path)
%% Aggregate BW(P(cell)) or BW(P(contact)) based on principal axes from PCA
%-- Input data
%   (1) BW(P(cell)) or BW(P(contact)) in FITS format (uint8)
%   (2) Centroid and principal axes information in csv file
%-- Output data
%   (1) Aggregated BW(P(cell)) or BW(P(contact))

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
output_directory = xPath.compile('/BinaryAggregation/output/directory').evaluate(xmlDocument, XPathConstants.STRING);

input_probDirectory = [];
k = 1;
xml_line = xPath.compile(['/BinaryAggregation/input/probDirectory' num2str(k)]).evaluate(xmlDocument, XPathConstants.STRING);
while ~isempty(xml_line)
    input_probDirectory = [input_probDirectory; {xml_line}];
    
    k = k+1;
    xml_line = xPath.compile(['/BinaryAggregation/input/probDirectory' num2str(k)]).evaluate(xmlDocument, XPathConstants.STRING);
end

input_probExtension = xPath.compile('/BinaryAggregation/input/probPattern').evaluate(xmlDocument, XPathConstants.STRING);

% Shape information (centroid and principal axes)
shapeInfo_file = [];
k = 1;
xml_line = xPath.compile(['/BinaryAggregation/shapeInfo/file' num2str(k)]).evaluate(xmlDocument, XPathConstants.STRING);
while ~isempty(xml_line)
    shapeInfo_file = [shapeInfo_file; {xml_line}];
    
    k = k+1;
    xml_line = xPath.compile(['/BinaryAggregation/shapeInfo/file' num2str(k)]).evaluate(xmlDocument, XPathConstants.STRING);
end

% Parameters
parameters_scaffoldType = xPath.compile('/BinaryAggregation/parameters/scaffoldType').evaluate(xmlDocument, XPathConstants.STRING); % scaffold type
parameters_sizeWidth = xPath.compile('/BinaryAggregation/parameters/sizeWidth').evaluate(xmlDocument, XPathConstants.NUMBER); % Aggregated map size (width; dimension 3)
parameters_sizeHeight = xPath.compile('/BinaryAggregation/parameters/sizeHeight').evaluate(xmlDocument, XPathConstants.NUMBER); % Aggregated map size (height; dimension 2)
parameters_sizeLayer = xPath.compile('/BinaryAggregation/parameters/sizeLayer').evaluate(xmlDocument, XPathConstants.NUMBER); % Aggregated map size (# of layers; dimension 1)


% Check if all necessary inputs are available
if isempty(output_directory)
    status = 1;
    message = 'Error: output_directory is missing.';
    return
end

if isempty(input_probDirectory)
    status = 1;
    message = 'Error: input_probDirectory is missing.';
    return
end

if isempty(input_probExtension)
    status = 1;
    message = 'Error: input_probExtension is missing.';
    return
end

if isempty(shapeInfo_file)
    status = 1;
    message = 'Error: shapeInfo_file is missing.';
    return
end

if isempty(parameters_scaffoldType)
    status = 1;
    message = 'Error: parameters_scaffoldType is missing.';
    return
end

if isnan(parameters_sizeWidth)
    status = 1;
    message = 'Error: parameters_sizeWidth is missing.';
    return
end

if isnan(parameters_sizeHeight)
    status = 1;
    message = 'Error: parameters_sizeHeight is missing.';
    return
end

if isnan(parameters_sizeLayer)
    status = 1;
    message = 'Error: parameters_sizeLayer is missing.';
    return
end

if length(input_probDirectory) ~= length(shapeInfo_file)
    status = 1;
    message = 'Error: The number of input_probDirectory does not match the number of shapeInfo_file.';
    return
end


%% Aggregate probability clouds
disp('Aggregating binarized probability maps...');

% Output directory
if ~exist(output_directory, 'dir')
    mkdir(output_directory);
end

% Aggregate data (occurrence)
P_total = zeros(parameters_sizeLayer, parameters_sizeHeight, parameters_sizeWidth, 'uint16');

% Position of the origin
Ctr = struct('x', round(parameters_sizeWidth/2), 'y', round(parameters_sizeHeight/2), 'z', round(parameters_sizeLayer/2));

nD = 0;
for k = 1:length(input_probDirectory)
    Shape = readtable(shapeInfo_file{k});
    N = size(Shape,1);
    nD = nD + N;

    for kd = 1:N
        dirname = Shape.ImageName{kd};
        disp(['-- [DB ' num2str(k) '] ' num2str(kd) '/' num2str(N) ': ' dirname]);

        % Load binary data
        BW = fitsread([input_probDirectory{k} filesep dirname input_probExtension]);
        BW = convert_fits2mat(BW, 'uint8', 'binary');

        [L,H,W] = size(BW);

        % Convert coordinates (find the corresponding voxel from the original data)
        %--- find a rough location of foreground of BW in P_total
        V = [Shape.PrincipalAxesEigenVector1X(kd) Shape.PrincipalAxesEigenVector2X(kd) Shape.PrincipalAxesEigenVector3X(kd);
             Shape.PrincipalAxesEigenVector1Y(kd) Shape.PrincipalAxesEigenVector2Y(kd) Shape.PrincipalAxesEigenVector3Y(kd);
             Shape.PrincipalAxesEigenVector1Z(kd) Shape.PrincipalAxesEigenVector2Z(kd) Shape.PrincipalAxesEigenVector3Z(kd)];

        indPos = find(BW > 0);
        [z,y,x] = ind2sub([L H W], indPos);

        x = x - Shape.CentroidX(kd);
        y = y - Shape.CentroidY(kd);
        z = z - Shape.CentroidZ(kd);

        X = [x y z] * V;

        X(:,1) = X(:,1) + Ctr.x;
        X(:,2) = X(:,2) + Ctr.y;
        X(:,3) = X(:,3) + Ctr.z;
        X = round(X);            

        bound_x = [max(min(X(:,1))-10,1) min(max(X(:,1))+10,parameters_sizeWidth)];
        bound_y = [max(min(X(:,2))-10,1) min(max(X(:,2))+10,parameters_sizeHeight)];
        bound_z = [max(min(X(:,3))-2,1) min(max(X(:,3))+2,parameters_sizeLayer)];
            
        %--- find the corresponding BW voxels
        [y,z,x] = meshgrid(bound_y(1):bound_y(2), bound_z(1):bound_z(2), bound_x(1):bound_x(2));
        x = x(:);
        y = y(:);
        z = z(:);
        indTotal = sub2ind([parameters_sizeLayer parameters_sizeHeight parameters_sizeWidth], z, y, x);
            
        x = x - Ctr.x;
        y = y - Ctr.y;
        z = z - Ctr.z;

        X = [x y z] * inv(V);
            
        x = X(:,1) + Shape.CentroidX(kd);
        y = X(:,2) + Shape.CentroidY(kd);
        z = X(:,3) + Shape.CentroidZ(kd);
            
        x = round(x);
        y = round(y);
        z = round(z);
            
        ind = find(x < 1 | x > W | y < 1 | y > H | z < 1 | z > L);
        x(ind) = [];
        y(ind) = [];
        z(ind) = [];
        indTotal(ind) = [];
            
        indBW = sub2ind([L H W], z, y, x);

        tmp_P_total = zeros(parameters_sizeLayer,parameters_sizeHeight,parameters_sizeWidth, 'uint16');
        tmp_P_total(indTotal) = BW(indBW);

        P_total = P_total + tmp_P_total;
    end
end

disp('Saving...');
save([output_directory filesep parameters_scaffoldType '.mat'], 'P_total', 'nD', 'Ctr', '-v7.3');
disp('Done!');