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

%% Create XML file for "FitPlane_CellIntersection.m"

% XML file name
fname = 'test_fitPlane_CellIntersection';
fpath = '.'; % path to save the xml file

% Paths
output_directory = '../test_data/PlanarGeoModel/CellIntersection';
input_imageDirectory = '../test_data/PlanarGeoModel/CellBW'; % binarized cell z-stack
input_imageExtension = '.fits';
input_planarCoefficientFile = '../test_data/PlanarGeoModel/Hyperplane_Coefficients.csv';

% Parameters
parameters_ez = 1/sqrt(2); % a voxel in cell z-stack is determined to be an intersection point if the distance to plane is smaller than 'ez'

%% Write XML file
% Create the document node and root element
xmldoc = com.mathworks.xml.XMLUtils.createDocument('FitPlane_CellIntersection');

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
    
    subelm = xmldoc.createElement('imageExtension');
    subelm.appendChild(xmldoc.createTextNode(input_imageExtension));
    elm.appendChild(subelm);

    subelm = xmldoc.createElement('planarCoefficientFile');
    subelm.appendChild(xmldoc.createTextNode(input_planarCoefficientFile));
    elm.appendChild(subelm);
            
%% Parameters

elm = xmldoc.createElement('parameters');
root.appendChild(elm);

    subelm = xmldoc.createElement('ez');
    subelm.appendChild(xmldoc.createTextNode(num2str(parameters_ez)));
    elm.appendChild(subelm);

%% Export the DOM node to xml file
xmlwrite([fpath filesep fname '.xml'], xmldoc);
