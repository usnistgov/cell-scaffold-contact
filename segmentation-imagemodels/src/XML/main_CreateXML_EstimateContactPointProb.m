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


clearvars; close all; clc;

%% Create XML file for "EstimateContactPointProb.m"

% XML file name
fname = 'test_contact';
fpath = '.'; % path to save the xml file

% Algorithm (Uncomment the desired 'algorithm_method' and 'algorithm_method_name'
algorithm_method = 1;
algorithm_method_name = 'SinglePixelModel'; % optional
% algorithm_method = 2;
% algorithm_method_name = 'MixedPixelSpatialModel'; % optional
% algorithm_method = 3;
% algorithm_method_name = 'MixedPixelChannelModel'; % optional
% algorithm_method = 4;
% algorithm_method_name = 'AdditiveGaussianModel'; % optional
% algorithm_method = 5;
% algorithm_method_name = 'MRFProbabilityModel'; % optional

% Paths
output_directory = ['./test_data/ContactPointEstimation/Alg' num2str(algorithm_method) '/SpunCoat-06252015_ZProfile'];

input_imageDirectory = './test_data/Segmentations/SpunCoat-06252015_ZProfile';
input_imagePatternCell = '*_ch00.fits';
input_imagePatternScaffold = '*_ch01.fits';
input_imageBGPatternCell = '*_ch00.tif';
input_imageBGPatternScaffold = '*_ch01.tif';

input_probDirectory = './test_data/ContactPointEstimation/Kmeans/SpunCoat-06252015_ZProfile/FITS';
input_probPatternCell = '*_ch00.fits';
input_probPatternScaffold = '*_ch01.fits';

% Parameters
parameters_threshold_percentile = 0.975; % (p*100)-th percentile for thresholding
parameters_threshold_method = 'PercentileThresholding'; % Method for obtaining threshold value: PercentileThresholding, NormalThresholding
parameters_threshold_morphType = 'square'; % Morphological processing (open) on binarized images
parameters_threshold_morphSize = 3;

parameters_neighborhoodSize = 3;

parameters_contactPointProbMethod = 'Max'; % Methods for computing contact point probability: Max, Mean, Mul

parameters_flatFieldCorrectionThreshold = 0.5; % Threshold for z-stacks after flat-field correction

%% Write XML file
% Create the document node and root element
xmldoc = com.mathworks.xml.XMLUtils.createDocument('ContactPointEstimation');

% Identify the root element
root = xmldoc.getDocumentElement;

%% Algorithm
% Add an element node
elm = xmldoc.createElement('algorithm');
root.appendChild(elm);

    % Add sub-element nodes
    subelm = xmldoc.createElement('method');
    subelm.appendChild(xmldoc.createTextNode(num2str(algorithm_method)));
    subelm.appendChild(xmldoc.createComment(algorithm_method_name)); % Add a comment
    elm.appendChild(subelm);

%% Paths
elm = xmldoc.createElement('output');
root.appendChild(elm);

    subelm = xmldoc.createElement('directory');
    subelm.appendChild(xmldoc.createTextNode(output_directory));
    elm.appendChild(subelm);

elm = xmldoc.createElement('input');
root.appendChild(elm);

    subelm = xmldoc.createElement('imageDirectory');
    subelm.appendChild(xmldoc.createTextNode(input_imageDirectory));
    elm.appendChild(subelm);
    
    subelm = xmldoc.createElement('imagePatternCell');
    subelm.appendChild(xmldoc.createTextNode(input_imagePatternCell));
    elm.appendChild(subelm);

    subelm = xmldoc.createElement('imagePatternScaffold');
    subelm.appendChild(xmldoc.createTextNode(input_imagePatternScaffold));
    elm.appendChild(subelm);
    
    subelm = xmldoc.createElement('imageBGPatternCell');
    subelm.appendChild(xmldoc.createTextNode(input_imageBGPatternCell));
    elm.appendChild(subelm);

    subelm = xmldoc.createElement('imageBGPatternScaffold');
    subelm.appendChild(xmldoc.createTextNode(input_imageBGPatternScaffold));
    elm.appendChild(subelm);
    
    subelm = xmldoc.createElement('probDirectory');
    subelm.appendChild(xmldoc.createTextNode(input_probDirectory));
    elm.appendChild(subelm);
    
    subelm = xmldoc.createElement('probPatternCell');
    subelm.appendChild(xmldoc.createTextNode(input_probPatternCell));
    elm.appendChild(subelm);

    subelm = xmldoc.createElement('probPatternScaffold');
    subelm.appendChild(xmldoc.createTextNode(input_probPatternScaffold));
    elm.appendChild(subelm);
    

%% Parameters
elm = xmldoc.createElement('parameters');
root.appendChild(elm);

    subelm = xmldoc.createElement('threshold');
    elm.appendChild(subelm);

        subsubelm = xmldoc.createElement('percentile');
        subsubelm.appendChild(xmldoc.createTextNode(num2str(parameters_threshold_percentile)));
        subelm.appendChild(subsubelm);

        subsubelm = xmldoc.createElement('method');
        subsubelm.appendChild(xmldoc.createTextNode(parameters_threshold_method));
        subelm.appendChild(subsubelm);
        
        subsubelm = xmldoc.createElement('morphType');
        subsubelm.appendChild(xmldoc.createTextNode(parameters_threshold_morphType));
        subelm.appendChild(subsubelm);
        
        subsubelm = xmldoc.createElement('morphSize');
        subsubelm.appendChild(xmldoc.createTextNode(num2str(parameters_threshold_morphSize)));
        subelm.appendChild(subsubelm);
            
    subelm = xmldoc.createElement('neighborhoodSize');
    subelm.appendChild(xmldoc.createTextNode(num2str(parameters_neighborhoodSize)));
    elm.appendChild(subelm);
    
    subelm = xmldoc.createElement('contactPointProbMethod');
    subelm.appendChild(xmldoc.createTextNode(parameters_contactPointProbMethod));
    elm.appendChild(subelm);
    
    subelm = xmldoc.createElement('flatFieldCorrectionThreshold');
    subelm.appendChild(xmldoc.createTextNode(num2str(parameters_flatFieldCorrectionThreshold)));
    elm.appendChild(subelm);


%% Export the DOM node to xml file
xmlwrite([fpath filesep fname '.xml'], xmldoc);
