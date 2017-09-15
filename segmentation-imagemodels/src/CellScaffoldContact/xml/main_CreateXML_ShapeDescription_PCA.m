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

%% Create XML file for "ShapeDescription_PCA.m"

% XML file name
fname = 'test_shapeDescription_PCA';
fpath = '.'; % path to save the xml file

% Paths
output_filename = '../test_data/ContactPointEstimation/Alg2/SpunCoat-07072015_ZProfile/Shape/PhysicalUnit/PCA.csv';
% output_filename = '../test_data/ContactPointEstimation/Alg2/SpunCoat-07072015_ZProfile/Shape/ImageUnit/PCA.csv';
input_binaryDirectory = '../test_data/ContactPointEstimation/Alg2/SpunCoat-07072015_ZProfile/BinaryContact';
input_binaryPattern = '*.fits';

% Parameters
parameters_CDELT1 = 0.120; % length of each pixel along axis 1
parameters_CDELT2 = 0.120; % length of each pixel along axis 2
parameters_CDELT3 = 0.462; % length of each pixel along axis 3
parameters_unit = 'Micrometer'; % Unit of physical dimensions

% parameters_CDELT1 = 1; % length of each pixel along axis 1
% parameters_CDELT2 = 1; % length of each pixel along axis 2
% parameters_CDELT3 = 1; % length of each pixel along axis 3
% parameters_unit = 'Pixel'; % Unit of physical dimensions

%% Write XML file
% Create the document node and root element
xmldoc = com.mathworks.xml.XMLUtils.createDocument('ShapeDescription_PCA');

% Identify the root element
root = xmldoc.getDocumentElement;

%% Paths
elm = xmldoc.createElement('output');
root.appendChild(elm);

    subelm = xmldoc.createElement('filename');
    subelm.appendChild(xmldoc.createTextNode(output_filename));
    elm.appendChild(subelm);

elm = xmldoc.createElement('input');
root.appendChild(elm);

    subelm = xmldoc.createElement('binaryDirectory');
    subelm.appendChild(xmldoc.createTextNode(input_binaryDirectory));
    elm.appendChild(subelm);

    subelm = xmldoc.createElement('binaryPattern');
    subelm.appendChild(xmldoc.createTextNode(input_binaryPattern));
    elm.appendChild(subelm);
    
%% Parameters
elm = xmldoc.createElement('parameters');
root.appendChild(elm);

    subelm = xmldoc.createElement('CDELT1');
    subelm.appendChild(xmldoc.createTextNode(num2str(parameters_CDELT1)));
    elm.appendChild(subelm);
    
    subelm = xmldoc.createElement('CDELT2');
    subelm.appendChild(xmldoc.createTextNode(num2str(parameters_CDELT2)));
    elm.appendChild(subelm);
    
    subelm = xmldoc.createElement('CDELT3');
    subelm.appendChild(xmldoc.createTextNode(num2str(parameters_CDELT3)));
    elm.appendChild(subelm);
    
    subelm = xmldoc.createElement('unit');
    subelm.appendChild(xmldoc.createTextNode(parameters_unit));
    elm.appendChild(subelm);

%% Export the DOM node to xml file
xmlwrite([fpath filesep fname '.xml'], xmldoc);
