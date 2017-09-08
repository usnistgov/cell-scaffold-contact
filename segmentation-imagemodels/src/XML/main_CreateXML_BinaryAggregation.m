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

%% Create XML file for "BinaryAggregation.m"

% XML file name
fname = 'test_binaryAggregation';
fpath = '.'; % path to save the xml file

% Paths
output_directory = './test_data/ContactPointEstimation/Alg2/Aggregated';
input_probDirectory{1} = './test_data/ContactPointEstimation/Alg2/SpunCoat-06252015_ZProfile/BinaryContact';
input_probDirectory{2} = './test_data/ContactPointEstimation/Alg2/SpunCoat-07072015_ZProfile/BinaryContact';
input_probExtension = '.fits';

% Shape information (centroid and principal axes)
shapeInfo_file{1} = './test_data/ContactPointEstimation/Alg2/SpunCoat-06252015_ZProfile/Shape/ImageUnit/PCA.csv';
shapeInfo_file{2} = './test_data/ContactPointEstimation/Alg2/SpunCoat-07072015_ZProfile/Shape/ImageUnit/PCA.csv';

% Scaffold type
parameters_scaffoldType = 'SpunCoat';
parameters_sizeWidth = 3001; % Aggregated map size (width; dimension 3)
parameters_sizeHeight = 2201; % Aggregated map size (height; dimension 2)
parameters_sizeLayer = 211; % Aggregated map size (# of layers; dimension 1)


%% Write XML file
% Create the document node and root element
xmldoc = com.mathworks.xml.XMLUtils.createDocument('BinaryAggregation');

% Identify the root element
root = xmldoc.getDocumentElement;

%% Paths
elm = xmldoc.createElement('output');
root.appendChild(elm);

    subelm = xmldoc.createElement('directory');
    subelm.appendChild(xmldoc.createTextNode(output_directory));
    elm.appendChild(subelm);

elm = xmldoc.createElement('input');
root.appendChild(elm);

    for k = 1:length(input_probDirectory)
        subelm = xmldoc.createElement(['probDirectory' num2str(k)]);
        subelm.appendChild(xmldoc.createTextNode(input_probDirectory{k}));
        elm.appendChild(subelm);
    end

    subelm = xmldoc.createElement('probPattern');
    subelm.appendChild(xmldoc.createTextNode(input_probExtension));
    elm.appendChild(subelm);
    
elm = xmldoc.createElement('shapeInfo');
root.appendChild(elm);

    for k = 1:length(input_probDirectory)
        subelm = xmldoc.createElement(['file' num2str(k)]);
        subelm.appendChild(xmldoc.createTextNode(shapeInfo_file{k}));
        elm.appendChild(subelm);
    end

%% Parameters
elm = xmldoc.createElement('parameters');
root.appendChild(elm);

    subelm = xmldoc.createElement('scaffoldType');
    subelm.appendChild(xmldoc.createTextNode(parameters_scaffoldType));
    elm.appendChild(subelm);
    
    subelm = xmldoc.createElement('sizeWidth');
    subelm.appendChild(xmldoc.createTextNode(num2str(parameters_sizeWidth)));
    elm.appendChild(subelm);
    
    subelm = xmldoc.createElement('sizeHeight');
    subelm.appendChild(xmldoc.createTextNode(num2str(parameters_sizeHeight)));
    elm.appendChild(subelm);
    
    subelm = xmldoc.createElement('sizeLayer');
    subelm.appendChild(xmldoc.createTextNode(num2str(parameters_sizeLayer)));
    elm.appendChild(subelm);
    
%% Export the DOM node to xml file
xmlwrite([fpath filesep fname '.xml'], xmldoc);
