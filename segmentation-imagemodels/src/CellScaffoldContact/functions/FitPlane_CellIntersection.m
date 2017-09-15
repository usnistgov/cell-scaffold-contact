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


function [status, message] = FitPlane_CellIntersection(xml_doc_path)
%% Find intersection between binary cell z-stack and a 3D plane to spun coat type scaffolds
%-- Input data
%   (1) Binary cell z-stack in FITS format (uint8)
%   (2) Coefficient of hyperplane (ax + by + cz + d = 0),
%       upper and lower z-intercepts of spun coat (z_lb and z_ub), thickness of spun coat,
%       and statistics in CSV file
%-- Output data
%   (1) Binary cell-scaffold intersection in FITS format (uint8)

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
output_directory = xPath.compile('/FitPlane_CellIntersection/output/directory').evaluate(xmlDocument, XPathConstants.STRING);
input_imageDirectory = xPath.compile('/FitPlane_CellIntersection/input/imageDirectory').evaluate(xmlDocument, XPathConstants.STRING);
input_imageExtension = xPath.compile('/FitPlane_CellIntersection/input/imageExtension').evaluate(xmlDocument, XPathConstants.STRING);
input_planarCoefficientFile = xPath.compile('/FitPlane_CellIntersection/input/planarCoefficientFile').evaluate(xmlDocument, XPathConstants.STRING);

% Parameters
parameters_ez = xPath.compile('/FitPlane_CellIntersection/parameters/ez').evaluate(xmlDocument, XPathConstants.NUMBER); % distance from the 3D plane to determine cell-scaffold intersection voxel

% Check if all necessary inputs are available
if isempty(output_directory)
    status = 1;
    message = 'Error: output_directory is missing.';
    return
end

if isempty(input_imageDirectory)
    status = 1;
    message = 'Error: input_imageDirectory is missing.';
    return
end

if isempty(input_imageExtension)
    status = 1;
    message = 'Error: input_imageExtension is missing.';
    return
end

if isempty(input_planarCoefficientFile)
    status = 1;
    message = 'Error: input_planarCoefficientFile is missing.';
    return
end

if isnan(parameters_ez)
    status = 1;
    message = 'Error: parameters_ez is missing.';
    return
end


%% Fit planar geometrical model to spun coat scaffold

disp('Find intersection between cell and scaffold model (planar geometrical model)...');

% Coefficient of hyperplane (ax + by + cz + d = 0; z_lb, z_ub, thickness)
Coeff = readtable(input_planarCoefficientFile);
nD = height(Coeff);

% Create output directories
if ~isdir(output_directory)
    mkdir(output_directory);
end

for kd = 1:nD
    dirname = Coeff.ImageName{kd};
    disp(['-- ' num2str(kd) '/' num2str(nD) ': ' dirname]);

    % Load z-stacks (cell binary mask)        
    Z = fitsread(fullfile(input_imageDirectory,[dirname input_imageExtension]));
    Z = convert_fits2mat(Z, 'uint8', 'binary');

    % Cell mask
    [L,H,W] = size(Z);

    ind = find(Z > 0);
    [z,y,x] = ind2sub([L,H,W], ind);

    % Find intersection
    z_est =  -(Coeff.a(kd)*x + Coeff.b(kd)*y + Coeff.d(kd)) / Coeff.c(kd) - Coeff.z_lb(kd);
    ind_intersect = find( abs(z - z_est) < parameters_ez );

    Z_intersect = false(L,H,W);
    Z_intersect(ind(ind_intersect)) = true;
        
    Z_intersect = convert_mat2fits(Z_intersect, 'binary', 'uint8');
    fitswrite(Z_intersect, fullfile(output_directory,[dirname '.fits']));
end

