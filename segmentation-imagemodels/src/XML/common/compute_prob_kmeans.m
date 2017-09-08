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


function [P_cell, P_scaffold] = compute_prob_kmeans(Z_cell, Z_scaffold, nCluster)

[L,H,W] = size(Z_cell);

% Kmeans clustering
X = [Z_cell(:)/max(Z_cell(:)) Z_scaffold(:)/max(Z_scaffold(:))]; % Feature vector
[Label, Ctr, sumd, D] = kmeans(X, nCluster); % Cell, Scaffold, Background

% Label determination
Label = reshape(Label, [L,H,W]);

Ctr = [[1:nCluster]' Ctr];

[~, IND_cell] = max(Ctr(:,2));
Centroid_cell = Ctr(IND_cell,2:3);

Ctr(IND_cell,:) = [];
[~, ind] = max(Ctr(:,3));
Centroid_scaffold = Ctr(ind,2:3);
IND_scaffold = Ctr(ind,1);

% Label_cell = (Label==IND_cell);
% Label_scaffold = (Label==IND_scaffold);

% Probabilities
D_sum = sum(D,2);

D_cell = reshape(D(:,IND_cell), [L,H,W]);
D_scaffold = reshape(D(:,IND_scaffold), [L,H,W]);
D_sum = reshape(D_sum, [L,H,W]);                

D_cell = D_cell ./ D_sum;
D_scaffold = D_scaffold ./ D_sum;
D_bg = 1-(D_cell+D_scaffold);

RD_cell = 1/(D_cell+eps);
RD_scaffold = 1/(D_scaffold+eps);
RD_bg = 1/(D_bg+eps);

RD_sum = RD_cell + RD_scaffold + RD_bg;

P_cell = RD_cell ./ RD_sum;
P_scaffold = RD_scaffold ./ RD_sum;
