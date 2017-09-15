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


function [status, message] = FitPlane(xml_doc_path)
%% Fit a 3D plane to spun coat type scaffolds
%-- Input data
%   (1) Cropped scaffold z-stack in FITS format (int16)
%-- Output data
%   (1) Coefficient of hyperplane (ax + by + cz + d = 0),
%       upper and lower z-intercepts of spun coat (z_lb and z_ub), thickness of spun coat,
%       and statistics in CSV file

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
output_directory = xPath.compile('/FitPlane/output/directory').evaluate(xmlDocument, XPathConstants.STRING);
input_imageDirectory = xPath.compile('/FitPlane/input/imageDirectory').evaluate(xmlDocument, XPathConstants.STRING);
input_imagePatternScaffold = xPath.compile('/FitPlane/input/imagePatternScaffold').evaluate(xmlDocument, XPathConstants.STRING);

% Read cell segmentation information file
cellSegmenationInfo_directory = xPath.compile('/FitPlane/cellSegmentationInfo/directory').evaluate(xmlDocument, XPathConstants.STRING);
cellSegmenationInfo_file = xPath.compile('/FitPlane/cellSegmentationInfo/file').evaluate(xmlDocument, XPathConstants.STRING);

% Parameters
parameters_dx = xPath.compile('/FitPlane/parameters/dx').evaluate(xmlDocument, XPathConstants.NUMBER); % length of each pixel along axis 1
parameters_dy = xPath.compile('/FitPlane/parameters/dy').evaluate(xmlDocument, XPathConstants.NUMBER); % length of each pixel along axis 2
parameters_dz = xPath.compile('/FitPlane/parameters/dz').evaluate(xmlDocument, XPathConstants.NUMBER); % length of each pixel along axis 3
parameters_du = xPath.compile('/FitPlane/parameters/du').evaluate(xmlDocument, XPathConstants.NUMBER); % quantization unit
parameters_DoF = xPath.compile('/FitPlane/parameters/DoF').evaluate(xmlDocument, XPathConstants.NUMBER); % degree of freedom to get relative standard deviation of planar fitting result
parameters_unit = xPath.compile('/FitPlane/parameters/unit').evaluate(xmlDocument, XPathConstants.STRING); % unit of measurements


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

if ~strcmp(parameters_unit,'pixel') && isnan(parameters_dx)
    status = 1;
    message = 'Error: parameters_dx is missing.';
    return
end

if ~strcmp(parameters_unit,'pixel') && isnan(parameters_dy)
    status = 1;
    message = 'Error: parameters_dy is missing.';
    return
end

if ~strcmp(parameters_unit,'pixel') && isnan(parameters_dz)
    status = 1;
    message = 'Error: parameters_dz is missing.';
    return
end

if ~strcmp(parameters_unit,'pixel') && isnan(parameters_du)
    status = 1;
    message = 'Error: parameters_du is missing.';
    return
end

if isnan(parameters_DoF)
    status = 1;
    message = 'Error: parameters_DoF is missing.';
    return
end

if isempty(parameters_unit)
    status = 1;
    message = 'Error: parameters_unit is missing.';
    return
end

%% Fit planar geometrical model to spun coat scaffold

disp('Fit planar geometrical model to spun coat scaffold data...');

% Segmentation information
T = readtable(fullfile(cellSegmenationInfo_directory,cellSegmenationInfo_file));
nD = size(T,1);

% Create output directories
if ~isdir(output_directory)
    mkdir(output_directory);
end


% Coefficient of hyperplane (ax + by + cz + d = 0)
Coeff = struct('ImageName',{cell(nD,1)}, 'a',zeros(nD,1), 'b',zeros(nD,1), 'c',zeros(nD,1), 'd',zeros(nD,1), ...
    'z_lb',zeros(nD,1), 'z_ub',zeros(nD,1), 'thickness',zeros(nD,1), ...
    'STD_NUM',zeros(nD,1), 'STD_DEN',zeros(nD,1), 'STD_REL',zeros(nD,1), 'N',zeros(nD,1), 'STD_POOLED',zeros(nD,1), 'Unit',{cell(nD,1)});
    
for kd = 1:nD
    dirname = T.ImageName{kd};
    disp(['-- ' num2str(kd) '/' num2str(nD) ': ' dirname]);
    
    Coeff.ImageName{kd} = dirname;
        
    % Load z-stacks        
    P = fitsread(fullfile(input_imageDirectory,[dirname input_imagePatternScaffold]));
    P = convert_fits2mat(P,'int16','uint16');
    P = double(P) / (2^16-1);

    [L,H,W] = size(P);
    ind = find(P > 0);
    [z,y,x] = ind2sub([L H W], ind);
    w = P(ind).^4;

    if ~strcmp(parameters_unit,'pixel')
        x = x * parameters_dx;
        y = y * parameters_dy;
        z = z * parameters_dz;
    end

    % Plane fitting 
    [V,D] = eig( [x y z ones(length(ind),1)]' * ((w*ones(1,4)) .* [x y z ones(length(ind),1)]) );
        
    [m, mid] = min(diag(D));
    p = V(:,mid);

    Coeff.a(kd) = p(1);
    Coeff.b(kd) = p(2);
    Coeff.c(kd) = p(3);
    Coeff.d(kd) = p(4);

    % Plane fitting evaluation
    STD_NUM = sum(w.*(Coeff.a(kd)*x+Coeff.b(kd)*y+Coeff.c(kd)*z+Coeff.d(kd)).^2);
    STD_DEN = sum(w) - mean(w)*parameters_DoF;
    STD_REL = sqrt(STD_NUM/STD_DEN);

    Coeff.STD_NUM(kd) = STD_NUM;
    Coeff.STD_DEN(kd) = STD_DEN;
    Coeff.STD_REL(kd) = STD_REL;
    Coeff.N(kd) = length(ind);

    % Coordinate transformation
    x1 = 1; y1 = 1; z1 = -(p(1)*x1 + p(2)*y1 + p(4)) / p(3);
    x2 = W; y2 = 1; z2 = -(p(1)*x2 + p(2)*y2 + p(4)) / p(3);
    v1 = [x2 y2 z2] - [x1 y1 z1];
    v1 = v1 / norm(v1);
    v1 = v1';

    x2 = 1; y2 = H; z2 = -(p(1)*x2 + p(2)*y2 + p(4)) / p(3);
    v2 = [x2 y2 z2] - [x1 y1 z1];
    v2 = v2 / norm(v2);
    v2 = v2';

    v3 = -p(1:3) / norm(p(1:3));

    y_new = [x y z] * v2;
    z_new = [x y z] * v3;
    
    if strcmp(parameters_unit,'pixel')
        y_new = round(y_new);
        z_new = round(z_new);
        
        y0_new = min(y_new);
        z0_new = min(z_new);
        
        y_new = y_new - y0_new + 1;
        z_new = z_new - z0_new + 1;
        
        H_new = max(y_new);
        L_new = max(z_new);
        
        % Projection on new Y-axis
        P_new = zeros(L_new, H_new);
        nP_new = zeros(L_new, H_new);
        maxP_new = zeros(L_new, H_new);
        for j = 1:length(y_new)
            P_new(z_new(j), y_new(j)) = P_new(z_new(j), y_new(j)) + P(ind(j));
            nP_new(z_new(j), y_new(j)) = nP_new(z_new(j), y_new(j)) + 1;
            
            if P(ind(j)) > maxP_new(z_new(j), y_new(j))
                maxP_new(z_new(j), y_new(j)) = P(ind(j));
            end
        end
    else
        % Quantization
        y_quantize = floor(min(y_new)/parameters_du)*parameters_du:parameters_du:ceil(max(y_new)/parameters_du)*parameters_du;
        z_quantize = floor(min(z_new)/parameters_du)*parameters_du:parameters_du:ceil(max(z_new)/parameters_du)*parameters_du;

        min_y_quantize = min(y_quantize);
        min_z_quantize = min(z_quantize);

        H_new = length(y_quantize)-1;
        L_new = length(z_quantize)-1;

        % Projection on new Y-axis
        P_new = zeros(L_new, H_new);
        nP_new = zeros(L_new, H_new);
        maxP_new = zeros(L_new, H_new);

        for ki = 1:length(ind)
            yi = floor((y_new(ki)-min_y_quantize)/parameters_du) + 1;
            zi = floor((z_new(ki)-min_z_quantize)/parameters_du) + 1;

            P_new(zi,yi) = P_new(zi,yi) + P(ind(ki));
            nP_new(zi,yi) = nP_new(zi,yi) + 1;

            if P(ind(ki)) > maxP_new(zi,yi)
                maxP_new(zi,yi) = P(ind(ki));
            end
        end
    end
    P_new(nP_new>0) = P_new(nP_new>0) ./ nP_new(nP_new>0);
    
    % Smooth filter for z profile
    smooth_filter = normpdf([-5:5], 0, 1);
    smooth_filter = smooth_filter / sum(smooth_filter);

    % Find peaks from smoothed z-profile
    prof_z = max(P_new.^4, [], 2);
    prof_z_smooth = conv(prof_z, smooth_filter, 'same');
    [pks, pks_locs] = findpeaks(prof_z_smooth);

    % Find peaks from smoothed second derivative of z-profile
    prof_z_secdev = del2(prof_z_smooth);
    prof_z_secdev_smooth = conv(prof_z_secdev, smooth_filter, 'same');
    [pks_secdev, pks_locs_secdev] = findpeaks(prof_z_secdev_smooth);

    % Scaffold location in z-axis
    [pks_max, pks_ind] = max(pks);
    pks_locs_max = pks_locs(pks_ind);

    % Top scaffold layer in z-axis
    ix = find(pks_locs_secdev < pks_locs_max);
    if isempty(ix)
        pks_locs_lb = 1;
    else
        [pks_max_lb, max_ind] = max(pks_secdev(ix));
        pks_locs_lb = pks_locs_secdev(ix(max_ind));
    end

    % Bottom scaffold layer in z-axis
    ix = find(pks_locs_secdev > pks_locs_max);
    if isempty(ix)
        pks_locs_ub = L_new;
    else
        [pks_max_lb, max_ind] = max(pks_secdev(ix));
        pks_locs_ub = pks_locs_secdev(ix(max_ind));
    end

    % Find intersects in z-axis
    if strcmp(parameters_unit,'pixel')
        z_delta_lb = (pks_locs_lb + z0_new - 1) / v3(3);
        z_delta_ub = (pks_locs_ub + z0_new - 1) / v3(3);
        
        x1 = 1; y1 = 1; z1 = -(p(1)*x1 + p(2)*y1 + p(4)) / p(3);
        z_lb = z1 - z_delta_lb;
        z_ub = z_delta_ub - z1;
        thickness = pks_locs_ub - pks_locs_lb;
    else 
        % Find intersects in z-axis
        z_delta_lb = z_quantize(pks_locs_lb) / v3(3);
        z_delta_ub = z_quantize(pks_locs_ub) / v3(3);

        x1 = 1; y1 = 1; z1 = -(p(1)*x1 + p(2)*y1 + p(4)) / p(3);
        z_lb = z1 - z_delta_lb;
        z_ub = z_delta_ub - z1;
        thickness = z_quantize(pks_locs_ub) - z_quantize(pks_locs_lb);
    end

    Coeff.z_lb(kd) = z_lb;
    Coeff.z_ub(kd) = z_ub;
    Coeff.thickness(kd) = thickness;
    Coeff.Unit{kd} = parameters_unit;
end

STD_POOLED = sqrt( sum((Coeff.N-1) .* Coeff.STD_REL.^2) / sum(Coeff.N-1) );
Coeff.STD_POOLED(:) = STD_POOLED;

Coeff_table = struct2table(Coeff);
if strcmp(parameters_unit,'pixel')
    writetable(Coeff_table, fullfile(output_directory,'Hyperplane_Coefficients.csv'));
else
    writetable(Coeff_table, fullfile(output_directory,'Hyperplane_Coefficients_PhysicalUnit.csv'));
end