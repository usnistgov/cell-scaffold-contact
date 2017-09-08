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


function [status, message] = KmeansClustering(xml_doc_path)
%% Kmeans clustering for P(cell), P(scaffold), and P(background)
%-- Input data
%   (1) Cropped cell and scaffold z-stacks in FITS format (int16)
%-- Output data
%   (1) P(cell) and P(scaffold) in FITS format (double precision)

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
output_directory = xPath.compile('/KmeansClustering/output/directory').evaluate(xmlDocument, XPathConstants.STRING);
input_imageDirectory = xPath.compile('/KmeansClustering/input/imageDirectory').evaluate(xmlDocument, XPathConstants.STRING);
input_imagePatternCell = xPath.compile('/KmeansClustering/input/imagePatternCell').evaluate(xmlDocument, XPathConstants.STRING);
input_imagePatternScaffold = xPath.compile('/KmeansClustering/input/imagePatternScaffold').evaluate(xmlDocument, XPathConstants.STRING);

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

if isempty(input_imagePatternCell)
    status = 1;
    message = 'Error: input_imagePatternCell is missing.';
    return
end

if isempty(input_imagePatternScaffold)
    status = 1;
    message = 'Error: input_imagePatternScaffold is missing.';
    return
end


%% Kmeans clustering to obtain P_cell, P_scaffold, and P_background

disp('Kmeans clustering for P_cell, P_scaffold, and P_background...');

% Parameters
nCluster = 3;

% Bounding box information
if ~exist([input_imageDirectory filesep 'segmentation.csv'], 'file')
    status = 1;
    message = 'Error: Run ''CropZStacks'' first.';
    return
end

T = readtable([input_imageDirectory filesep 'segmentation.csv']);
nD = size(T,1);

% Output directory
if ~exist(output_directory, 'dir')
    mkdir(output_directory);
end

% File name pattern difference
strPatternCell = strsplit(input_imagePatternCell, {'*','.'});
strPatternScaffold = strsplit(input_imagePatternScaffold, {'*','.'});
bDifferent = false(1,length(strPatternCell));
for j = 1:length(strPatternCell)
    if ~isequal(strPatternCell{j}, strPatternScaffold{j})
        bDifferent(j) = true;
    end
end
strPatternCell = cell2mat(strPatternCell(bDifferent));
strPatternScaffold = cell2mat(strPatternScaffold(bDifferent));

for kd = 1:nD
    dirname = T.ImageName{kd};
    disp(['-- ' num2str(kd) '/' num2str(nD) ': ' dirname]);

    % Load Z-stacks
    Z_cell_fits = fitsread([input_imageDirectory filesep dirname strPatternCell '.fits']);
    Z_cell = convert_fits2mat(Z_cell_fits, 'int16', 'uint16');
    
    Z_scaffold_fits = fitsread([input_imageDirectory filesep dirname strPatternScaffold '.fits']);
    Z_scaffold = convert_fits2mat(Z_scaffold_fits, 'int16', 'uint16');

    Z_cell = double(Z_cell);
    Z_scaffold = double(Z_scaffold);
    
    clear Z_cell_fits Z_scaffold_fits

    % K-means clustering
    [P_cell, P_scaffold] = compute_prob_kmeans(Z_cell, Z_scaffold, nCluster);
    
    % Save probabilities
    P_cell_fits = convert_mat2fits(P_cell, 'double', 'double');
    fitswrite(P_cell_fits, [output_directory filesep dirname strPatternCell '.fits']);

    P_scaffold_fits = convert_mat2fits(P_scaffold, 'double', 'double');
    fitswrite(P_scaffold_fits, [output_directory filesep dirname strPatternScaffold '.fits']);
end


