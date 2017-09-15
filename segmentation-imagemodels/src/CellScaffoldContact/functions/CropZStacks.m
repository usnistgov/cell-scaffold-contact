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


function [status, message] = CropZStacks(xml_doc_path)
%% Crop z-stacks
%-- Input data
%   (1) A set of TIF images for cell and scaffold channels
%   (2) Cell segmentation results (Segmentations/*-offset2000_MinError): FITS files and *_segmentation.csv
%-- Output data
%   (1) Cropped z-stacks (cell and scaffold channels) in FITS format (int16)
%   (2) Bounding box information: segmentation.csv

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
output_directory = xPath.compile('/CropZStacks/output/directory').evaluate(xmlDocument, XPathConstants.STRING);
input_imageDirectory = xPath.compile('/CropZStacks/input/imageDirectory').evaluate(xmlDocument, XPathConstants.STRING);
input_imagePatternCell = xPath.compile('/CropZStacks/input/imagePatternCell').evaluate(xmlDocument, XPathConstants.STRING);
input_imagePatternScaffold = xPath.compile('/CropZStacks/input/imagePatternScaffold').evaluate(xmlDocument, XPathConstants.STRING);

% Read cell segmentation information file
cellSegmenationInfo_directory = xPath.compile('/CropZStacks/cellSegmentationInfo/directory').evaluate(xmlDocument, XPathConstants.STRING);
cellSegmenationInfo_file = xPath.compile('/CropZStacks/cellSegmentationInfo/file').evaluate(xmlDocument, XPathConstants.STRING);

% Read background image information
backgroundImageInfo_file = xPath.compile('/CropZStacks/backgroundImageInfo/file').evaluate(xmlDocument, XPathConstants.STRING);

% Parameters
parameters_xyMargin = xPath.compile('/CropZStacks/parameters/xyMargin').evaluate(xmlDocument, XPathConstants.NUMBER); % 100*r% margin for x and y bounds
parameters_smoothFilterSize = xPath.compile('/CropZStacks/parameters/smoothFilterSize').evaluate(xmlDocument, XPathConstants.NUMBER); % Gaussian filter with size 21
parameters_smoothFilterSTD = xPath.compile('/CropZStacks/parameters/smoothFilterSTD').evaluate(xmlDocument, XPathConstants.NUMBER); % Standard deviation of the Gaussian filter

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

if isempty(cellSegmenationInfo_directory)
    status = 1;
    message = 'Error: cellSegmenationInfo_directory is missing.';
    return
end

if isempty(cellSegmenationInfo_file)
    status = 1;
    message = 'Error: cellSegmenationInfo_file is missing.';
    return
end

if isempty(backgroundImageInfo_file)
    status = 1;
    message = 'Error: backgroundImageInfo_file is missing.';
    return
end

if isnan(parameters_xyMargin)
    status = 1;
    message = 'Error: parameters_xyMargin is missing.';
    return
end

if isnan(parameters_smoothFilterSize)
    status = 1;
    message = 'Error: parameters_smoothFilterSize is missing.';
    return
end

if isnan(parameters_smoothFilterSTD)
    status = 1;
    message = 'Error: parameters_smoothFilterSTD is missing.';
    return
end

%% Crop Z-stacks using cell segmentation

disp('Crop z-stacks from cell segmentation results...');

% Smooth filter for z profile
smooth_filter = normpdf(-round(parameters_smoothFilterSize/2)+[1:parameters_smoothFilterSize], 0, parameters_smoothFilterSTD);
smooth_filter = smooth_filter / sum(smooth_filter);

% Cell segmentation information
T = readtable(cellSegmenationInfo_file);
nD = size(T,1);

% Create output directories
if ~exist([output_directory filesep 'FITS'], 'dir')
    mkdir([output_directory filesep 'FITS']);
end

if ~exist([output_directory filesep 'BackgroundImages'], 'dir')
    mkdir([output_directory filesep 'BackgroundImages']);
end

% File names for background images (first or last slice of z-stacks)
fid = fopen(backgroundImageInfo_file, 'rt');
fnames_bg = textscan(fid, '%s\t%s'); % cell, scaffold
fclose(fid);

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

[~,~,imageExt] = fileparts(input_imagePatternCell);

fgbox = zeros(nD,6); % bounding box for cropping (x1, x2, y1, y2, z1, z2)

for kd = 1:nD
    dirname = T.ImageName{kd};        
    disp(['-- ' num2str(kd) '/' num2str(nD) ': ' dirname]);
    
    % Load FITS data (binary mask from cell segmentation); '127' for foreground and '-128' for background
    BW_cell = fitsread([cellSegmenationInfo_directory filesep dirname '.fits']);

    % Find tight bounding box around cell from cell segmentation result
    box_cell = find_bounding_box_cell(T(kd,:), BW_cell);

    % Load Z-stacks
    flist = dir([input_imageDirectory filesep dirname filesep input_imagePatternCell]);
    L = length(flist);

    if L < 1
        status = 1;
        message = ['Error: input_imageDirectory does not contain ' input_imagePatternCell ' images.'];
        return
    end
    
    I = imread([input_imageDirectory filesep dirname filesep flist(1).name]);
    [H,W] = size(I);

    Z_cell = zeros(L,H,W);
    Z_scaffold = zeros(L,H,W);
    for kl = 1:L
        % Cell
        I = imread([input_imageDirectory filesep dirname filesep flist(kl).name]);
        Z_cell(kl,:,:) = I;

        % Scaffold
        fn_scaffold = strrep(flist(kl).name, strPatternCell, strPatternScaffold);
        I = imread([input_imageDirectory filesep dirname filesep fn_scaffold]);
        Z_scaffold(kl,:,:) = I;
    end
    
    % Find bounding box for cropping
    box_cell_scaffold = find_bounding_box_cropping(box_cell, Z_scaffold, parameters_xyMargin, smooth_filter);
    fgbox(kd,:) = [box_cell_scaffold.x1, box_cell_scaffold.x2, box_cell_scaffold.y1, box_cell_scaffold.y2, box_cell_scaffold.z1, box_cell_scaffold.z2];

    % Crop Z-stacks
    Z_cell_cropped = Z_cell(box_cell_scaffold.z1:box_cell_scaffold.z2, box_cell_scaffold.y1:box_cell_scaffold.y2, box_cell_scaffold.x1:box_cell_scaffold.x2);
    Z_scaffold_cropped = Z_scaffold(box_cell_scaffold.z1:box_cell_scaffold.z2, box_cell_scaffold.y1:box_cell_scaffold.y2, box_cell_scaffold.x1:box_cell_scaffold.x2);

    % Save cropped z-stacks in FITS (int16)
    Z_cell_cropped_fits = convert_mat2fits(Z_cell_cropped, 'uint16', 'int16');
    fitswrite(Z_cell_cropped_fits, [output_directory filesep 'FITS' filesep dirname strPatternCell '.fits']);

    Z_scaffold_cropped_fits = convert_mat2fits(Z_scaffold_cropped, 'uint16', 'int16');
    fitswrite(Z_scaffold_cropped_fits, [output_directory filesep 'FITS' filesep dirname strPatternScaffold '.fits']);
    
    % Crop background images
    fname_bg_cell = fnames_bg{1}{kd};
    fname_bg_scaffold = fnames_bg{2}{kd};
                
    BG_cell = imread([input_imageDirectory filesep dirname filesep fname_bg_cell imageExt]);
    BG_scaffold = imread([input_imageDirectory filesep dirname filesep fname_bg_scaffold imageExt]);

    BG_cell_cropped = BG_cell(box_cell_scaffold.y1:box_cell_scaffold.y2, box_cell_scaffold.x1:box_cell_scaffold.x2);
    BG_scaffold_cropped = BG_scaffold(box_cell_scaffold.y1:box_cell_scaffold.y2, box_cell_scaffold.x1:box_cell_scaffold.x2);
        
    % Save cropped background images
    imwrite(BG_cell_cropped, [output_directory filesep 'BackgroundImages' filesep dirname strPatternCell imageExt]);
    imwrite(BG_scaffold_cropped, [output_directory filesep 'BackgroundImages' filesep dirname strPatternScaffold imageExt]);
    
    % Save bounding box information; '0'-based index
    T_new = [T.ImageName, array2table(fgbox-1)];
    T_new.Properties.VariableNames = {'ImageName', 'xStart', 'xEnd', 'yStart', 'yEnd', 'zStart', 'zEnd'};

    writetable(T_new, [output_directory filesep 'segmentation.csv']);
end