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


function Z_mat = convert_fits2mat(Z_fits, fits_data_type, mat_data_type)

if strcmp(fits_data_type,'int16') && strcmp(mat_data_type,'uint16')
    Z_mat = double(Z_fits) + 2^15;
    Z_mat = uint16(Z_mat);
    Z_mat = flip(Z_mat, 1);
    Z_mat = permute(Z_mat, [3 1 2]);
    
elseif strcmp(fits_data_type,'double') && strcmp(mat_data_type,'double')
    Z_mat = flip(Z_fits, 1);
    Z_mat = permute(Z_mat, [3 1 2]);
    
elseif strcmp(fits_data_type,'int16') && strcmp(mat_data_type,'probability')
    Z_mat = double(Z_fits) / (2^15-1);
    Z_mat = flip(Z_mat, 1);
    Z_mat = permute(Z_mat, [3 1 2]);
    
elseif strcmp(fits_data_type,'uint8') && strcmp(mat_data_type,'probability')
    Z_mat = double(Z_fits) / (2^8-1);
    Z_mat = flip(Z_mat, 1);
    Z_mat = permute(Z_mat, [3 1 2]);
    
elseif strcmp(fits_data_type,'uint8') && strcmp(mat_data_type,'binary')
    Z_mat = (Z_fits > 0);
    Z_mat = flip(Z_mat, 1);
    Z_mat = permute(Z_mat, [3 1 2]);
   
end
