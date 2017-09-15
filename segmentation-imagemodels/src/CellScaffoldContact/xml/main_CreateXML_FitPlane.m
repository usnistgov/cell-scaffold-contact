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

%% Create XML file for "FitPlane.m"

% XML file name
fname = 'test_fitPlane';
fpath = '.'; % path to save the xml file

% Paths
output_directory = '../test_data/PlanarGeoModel';
input_imageDirectory = '../test_data/Segmentations/SpunCoat-06252015_ZProfile/FITS'; % (.fits)
input_imagePatternScaffold = '_ch01.fits';

% Cell segmentation information
cellSegmenationInfo_directory = '../test_data/Segmentations/SpunCoat-06252015_ZProfile';
cellSegmenationInfo_file = 'segmentation.csv'; % (.csv)

% Parameters
parameters_dx = 0.120; % length of each pixel along axis 1
parameters_dy = 0.120; % length of each pixel along axis 2
parameters_dz = 0.462; % length of each pixel along axis 3
parameters_du = 0.462; % quantization unit
parameters_DoF = 3; % degree of freedom to get relative standard deviation of planar fitting result
parameters_unit = 'pixel'; % unit of measurements; image unit
% parameters_unit = 'micrometer'; % unit of measurements; physical unit


%% Write XML file
% Create the document node and root element
xmldoc = com.mathworks.xml.XMLUtils.createDocument('FitPlane');

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

    subelm = xmldoc.createElement('imageDirectory');
    subelm.appendChild(xmldoc.createTextNode(input_imageDirectory));
    elm.appendChild(subelm);

    subelm = xmldoc.createElement('imagePatternScaffold');
    subelm.appendChild(xmldoc.createTextNode(input_imagePatternScaffold));
    elm.appendChild(subelm);
    
elm = xmldoc.createElement('cellSegmentationInfo');
root.appendChild(elm);

    subelm = xmldoc.createElement('directory');
    subelm.appendChild(xmldoc.createTextNode(cellSegmenationInfo_directory));
    elm.appendChild(subelm);
    
    subelm = xmldoc.createElement('file');
    subelm.appendChild(xmldoc.createTextNode(cellSegmenationInfo_file));
    elm.appendChild(subelm);
        
%% Parameters

elm = xmldoc.createElement('parameters');
root.appendChild(elm);

    subelm = xmldoc.createElement('dx');
    subelm.appendChild(xmldoc.createTextNode(num2str(parameters_dx)));
    elm.appendChild(subelm);

    subelm = xmldoc.createElement('dy');
    subelm.appendChild(xmldoc.createTextNode(num2str(parameters_dy)));
    elm.appendChild(subelm);
    
    subelm = xmldoc.createElement('dz');
    subelm.appendChild(xmldoc.createTextNode(num2str(parameters_dz)));
    elm.appendChild(subelm);
    
    subelm = xmldoc.createElement('du');
    subelm.appendChild(xmldoc.createTextNode(num2str(parameters_du)));
    elm.appendChild(subelm);

    subelm = xmldoc.createElement('DoF');
    subelm.appendChild(xmldoc.createTextNode(num2str(parameters_DoF)));
    elm.appendChild(subelm);
    
    subelm = xmldoc.createElement('unit');
    subelm.appendChild(xmldoc.createTextNode(parameters_unit));
    elm.appendChild(subelm);


%% Export the DOM node to xml file
xmlwrite([fpath filesep fname '.xml'], xmldoc);
