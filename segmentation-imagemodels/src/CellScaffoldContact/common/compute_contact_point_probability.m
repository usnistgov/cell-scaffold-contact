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


function P_contact = ComputeContactPointProbability(P_cell_singleCH, P_scaffold_singleCH, P_cell_bothCHs, P_scaffold_bothCHs, nNeighborhood, Method)

if strcmp(Method, 'Max')
    P_cell_max = dilate_3D(P_cell_singleCH, nNeighborhood);
    P_scaffold_max = dilate_3D(P_scaffold_singleCH, nNeighborhood);
    
    P_cell_contact = P_cell_singleCH .* P_scaffold_max;
    P_scaffold_contact = P_scaffold_singleCH .* P_cell_max;
    
    P_contact = P_cell_contact .* P_cell_bothCHs + P_scaffold_contact .* P_scaffold_bothCHs;
    
elseif strcmp(Method, 'Mean')
    P_cell_mean = convn(P_cell_singleCH, ones(nNeighborhood,nNeighborhood,nNeighborhood), 'same');
    P_scaffold_mean = convn(P_scaffold_singleCH, ones(nNeighborhood,nNeighborhood,nNeighborhood), 'same');
    
    P_cell_contact = P_cell_singleCH .* P_scaffold_mean;
    P_scaffold_contact = P_scaffold_singleCH .* P_cell_mean;
    
    P_contact = P_cell_contact .* P_cell_bothCHs + P_scaffold_contact .* P_scaffold_bothCHs;
    
elseif strcmp(Method, 'Mul')
    % Will be added
    error('Not yet implemented');
    
else
    error('Not determined probability computation method');
    
end