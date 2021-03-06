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


function [status, message] = alg2_mixed_pixel_spatial_model(xmlDocument)
%% Algorithm 2: Mixed-pixel spatial model
% Inputs
%-- Cropped Z-stacks (cell and scaffold channels)
%-- P(cell) and P(scaffold) from Kmeans
%
% Outputs
%-- P_contact: Contact point probability

status = 0;
message = [];

algorithm_method = 2;

%% Read XML file

% Import the XPath classes
import javax.xml.xpath.*

% Create an XPath expression.
factory = XPathFactory.newInstance;
xPath = factory.newXPath;

% Read paths
output_directory = xPath.compile('/ContactPointEstimation/output/directory').evaluate(xmlDocument, XPathConstants.STRING);

input_imageDirectory = xPath.compile('/ContactPointEstimation/input/imageDirectory').evaluate(xmlDocument, XPathConstants.STRING);
input_imagePatternCell = xPath.compile('/ContactPointEstimation/input/imagePatternCell').evaluate(xmlDocument, XPathConstants.STRING);
input_imagePatternScaffold = xPath.compile('/ContactPointEstimation/input/imagePatternScaffold').evaluate(xmlDocument, XPathConstants.STRING);
input_imageBGPatternCell = xPath.compile('/ContactPointEstimation/input/imageBGPatternCell').evaluate(xmlDocument, XPathConstants.STRING);
input_imageBGPatternScaffold = xPath.compile('/ContactPointEstimation/input/imageBGPatternScaffold').evaluate(xmlDocument, XPathConstants.STRING);

input_probDirectory = xPath.compile('/ContactPointEstimation/input/probDirectory').evaluate(xmlDocument, XPathConstants.STRING);
input_probPatternCell = xPath.compile('/ContactPointEstimation/input/probPatternCell').evaluate(xmlDocument, XPathConstants.STRING);
input_probPatternScaffold = xPath.compile('/ContactPointEstimation/input/probPatternScaffold').evaluate(xmlDocument, XPathConstants.STRING);

% Read parameters
parameters_neighborhoodSize = xPath.compile('/ContactPointEstimation/parameters/neighborhoodSize').evaluate(xmlDocument, XPathConstants.NUMBER);
parameters_contactPointProbMethod = xPath.compile('/ContactPointEstimation/parameters/contactPointProbMethod').evaluate(xmlDocument, XPathConstants.STRING); % Methods for computing contact point probability: {Max, Mean, Mul}
parameters_flatFieldCorrectionThreshold = xPath.compile('/ContactPointEstimation/parameters/flatFieldCorrectionThreshold').evaluate(xmlDocument, XPathConstants.NUMBER); % Threshold for z-stacks after flat-field correction

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

if isempty(input_imageBGPatternCell)
    status = 1;
    message = 'Error: input_imageBGPatternCell is missing.';
    return
end

if isempty(input_imageBGPatternScaffold)
    status = 1;
    message = 'Error: input_imageBGPatternScaffold is missing.';
    return
end

if isempty(input_probDirectory)
    status = 1;
    message = 'Error: input_probDirectory is missing.';
    return
end

if isempty(input_probPatternCell)
    status = 1;
    message = 'Error: input_probPatternCell is missing.';
    return
end

if isempty(input_probPatternScaffold)
    status = 1;
    message = 'Error: input_probPatternScaffold is missing.';
    return
end

if isnan(parameters_neighborhoodSize)
    status = 1;
    message = 'Error: parameters_neighborhoodSize is missing.';
    return
end

if isempty(parameters_contactPointProbMethod)
    status = 1;
    message = 'Error: parameters_contactPointProb is missing.';
    return
end

if isempty(parameters_flatFieldCorrectionThreshold)
    status = 1;
    message = 'Error: parameters_flatFieldCorrectionThreshold is missing.';
    return
end


%% Estimate contact point probability

disp(['Estimate contact point probability using Algorithm ' num2str(algorithm_method) '...']);

% Check if cropping z-stacks has been done
if ~exist([input_imageDirectory filesep 'segmentation.csv'], 'file')
    status = 1;
    message = 'Error: No cropped z-stacks available. Run ''CropZStacks'' first.';
    return
end

T = readtable([input_imageDirectory filesep 'segmentation.csv']);
nD = size(T,1);

if nD < 1
    message = 'Error: No files to be processed. End algorithm.';
    return
end

% Check if cropping background images has been done
list = dir([input_imageDirectory filesep 'BackgroundImages' filesep input_imageBGPatternCell]);
if isempty(list)
    status = 1;
    message = 'Error: No cropped background images available. Run ''CropZStacks'' first.';
    return
end

% Check if Kmeans clustering has been done
list = dir([input_probDirectory filesep input_probPatternCell]);
if isempty(list)
    status = 1;
    message = 'Error: Run ''KmeansClustering'' first.';
    return
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

[~,~,imageBGExt] = fileparts(input_imageBGPatternCell);

if ~exist(output_directory, 'dir')
    mkdir(output_directory);
end
 
for kd = 1:nD
    dirname = T.ImageName{kd};
    disp(['[Alg' num2str(algorithm_method) '] -- ' num2str(kd) '/' num2str(nD) ': ' dirname]);

    % Load background images for cell and scaffold channels
    bg_img_cell = imread([input_imageDirectory filesep 'BackgroundImages' filesep dirname strPatternCell imageBGExt]);
    bg_img_scaffold = imread([input_imageDirectory filesep 'BackgroundImages' filesep dirname strPatternScaffold imageBGExt]);
        
    bg_img_cell = double(bg_img_cell);
    bg_img_scaffold = double(bg_img_scaffold);
                
    % Z-stacks
    Z_cell_fits = fitsread([input_imageDirectory filesep 'FITS' filesep dirname strPatternCell '.fits']);
    Z_cell = convert_fits2mat(Z_cell_fits, 'int16', 'uint16');
    
    Z_scaffold_fits = fitsread([input_imageDirectory filesep 'FITS' filesep dirname strPatternScaffold '.fits']);
    Z_scaffold = convert_fits2mat(Z_scaffold_fits, 'int16', 'uint16');

    Z_cell = double(Z_cell);
    Z_scaffold = double(Z_scaffold);
    
    clear Z_cell_fits Z_scaffold_fits
    
    % Flat-field correction
    [FFC_cell, FFC_scaffold] = compute_FFC(Z_cell, Z_scaffold, bg_img_cell, bg_img_scaffold);
    clear Z_cell Z_scaffold
        
    % Compute probability for each channel
    P_cell = FFC_cell / max(FFC_cell(:));
    P_scaffold = FFC_scaffold / max(FFC_scaffold(:));
    clear FFC_cell FFC_scaffold
        
%     % Thresholding: If P > th, assign foreground
%     BW_cell = (P_cell > parameters_flatFieldCorrectionThreshold);
%     BW_scaffold = (P_scaffold > parameters_flatFieldCorrectionThreshold);
% 
%     % Label assignment
%     [Label_cell, Label_scaffold, Label_contact] = assign_labels(BW_cell, BW_scaffold, parameters_neighborhoodSize);
%     clear BW_cell BW_scaffold Label_cell Label_scaffold Label_contact

    % Contact point probability
    P_Kmeans_cell_fits = fitsread([input_probDirectory filesep dirname strPatternCell '.fits']);
    P_Kmeans_cell = convert_fits2mat(P_Kmeans_cell_fits, 'double', 'double');
    
    P_Kmeans_scaffold_fits = fitsread([input_probDirectory filesep dirname strPatternScaffold '.fits']);
    P_Kmeans_scaffold = convert_fits2mat(P_Kmeans_scaffold_fits, 'double', 'double');
    
    P_contact = compute_contact_point_probability(P_cell, P_scaffold, P_Kmeans_cell, P_Kmeans_scaffold, ...
        parameters_neighborhoodSize, parameters_contactPointProbMethod);
    
    % Save contact point probability
    P_contact_fits = convert_mat2fits(P_contact, 'probability', 'int16');
    fitswrite(P_contact_fits, [output_directory filesep dirname '.fits']);
end