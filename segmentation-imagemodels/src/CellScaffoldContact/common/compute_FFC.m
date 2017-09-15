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


function [FFC_cell, FFC_scaffold] = compute_FFC(Z_cell, Z_scaffold, bg_img_cell, bg_img_scaffold)

[L,H,W] = size(Z_cell);

max_Z_cell = max(Z_cell(:));
max_Z_scaffold = max(Z_scaffold(:));

FFC_cell = zeros(L,H,W);
FFC_scaffold = zeros(L,H,W);
for kl = 1:L        
    FFC_cell(kl,:,:) = (squeeze(Z_cell(kl,:,:)) - bg_img_cell) ./ (max_Z_cell - bg_img_cell);
    FFC_scaffold(kl,:,:) = (squeeze(Z_scaffold(kl,:,:)) - bg_img_scaffold) ./ (max_Z_scaffold - bg_img_scaffold);
end

FFC_cell(FFC_cell<0) = 0;
FFC_scaffold(FFC_scaffold<0) = 0;

if max(FFC_cell(:)) > 1
    error('Flat-field correction for cell channel exceeds 1');
end
if max(FFC_scaffold(:)) > 1
    error('Flat-field correction for scaffold channel exceeds 1');
end