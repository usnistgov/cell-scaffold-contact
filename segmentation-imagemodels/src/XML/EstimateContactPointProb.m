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


function [status, message] = EstimateContactPointProb(xml_doc_path)
%% Estimate contact point probability
%-- Input data
%   (1) Cropped cell and scaffold z-stacks in FITS format (int16)
%   (2) P(cell) and P(scaffold) in FITS format (double precision)
%-- Output data
%   (1) P(contact) in FITS format (int16)

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

% Read the algorithm information
algorithm_method = xPath.compile('/ContactPointEstimation/algorithm/method').evaluate(xmlDocument, XPathConstants.NUMBER);

if isempty(algorithm_method)
    status = 1;
    message = 'Error: algorithm_method is missing.';
    return
end

switch algorithm_method
    case 1
        [status, message] = alg1_single_pixel_model(xmlDocument);
    case 2
        [status, message] = alg2_mixed_pixel_spatial_model(xmlDocument);
    case 3
        [status, message] = alg3_mixed_pixel_channel_model(xmlDocument);
    case 4
        [status, message] = alg4_additive_gaussian_model(xmlDocument);
    case 5
        [status, message] = alg5_mrf_probability_model(xmlDocument);
    otherwise
        status = 1;
        message = ['Error: Algorithm ' num2str(algorithm_method) ' does not exist.'];
        return
end
