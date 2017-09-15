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
addpath('./functions','./common');

%% Cell-scaffold contact point estimation: Statistical models

% xml_crop = './xml/test_crop.xml';
% [status, message] = CropZStacks(xml_crop);
% if status > 0
%     disp(message);
% end

xml_kmeans = './xml/test_kmeans.xml';
[status, message] = KmeansClustering(xml_kmeans);
if status > 0
    disp(message);
end

xml_doc_path = './xml/test_contact.xml';
[status, message] = EstimateContactPointProb(xml_doc_path);
if status > 0
    disp(message);
end

xml_doc_path = './xml/test_shapeDescription_PCA.xml';
[status, message] = ShapeDescription_PCA(xml_doc_path);
if status > 0
    disp(message);
end

xml_doc_path = './xml/test_shapeDescription_WeightedInertia.xml';
[status, message] = ShapeDescription_WeightedInertia(xml_doc_path);
if status > 0
    disp(message);
end

xml_doc_path = './xml/test_binaryAggregation.xml';
[status, message] = BinaryAggregation(xml_doc_path);
if status > 0
    disp(message);
end
