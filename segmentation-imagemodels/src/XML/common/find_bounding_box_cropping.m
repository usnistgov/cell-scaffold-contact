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


function boxCellScaffold = find_bounding_box_cropping(boxCell, Z_scaffold, xyMargin, smooth_filter)

boxCellScaffold = [];

[L,H,W] = size(Z_scaffold);

% Size of the tight bounding box
X = boxCell.x2 - boxCell.x1+1;
Y = boxCell.y2 - boxCell.y1+1;
Z = boxCell.z2 - boxCell.z1+1;

% Center in x-y plane
cx = (boxCell.x1+boxCell.x2)/2;
cy = (boxCell.y1+boxCell.y2)/2;

% Extended bounds for x and y axes
boxCellScaffold = struct;
boxCellScaffold.x1 = max(round(cx - ((1+xyMargin)*X)/2), 1);
boxCellScaffold.x2 = min(round(cx + ((1+xyMargin)*X)/2), W);
boxCellScaffold.y1 = max(round(cy - ((1+xyMargin)*Y)/2), 1);
boxCellScaffold.y2 = min(round(cy + ((1+xyMargin)*Y)/2), H);

% Scaffold z-profile
Z_scaffold_cropped = Z_scaffold(:,boxCell.y1:boxCell.y2,boxCell.x1:boxCell.x2);

% Find peaks from smoothed z-profile
prof_z = max(max(Z_scaffold_cropped, [], 3), [], 2);
prof_z_smooth = conv(prof_z, smooth_filter, 'same');
[pks, pks_locs] = findpeaks(prof_z_smooth);

% Find peaks from smoothed second derivative of z-profile
prof_z_secdev = del2(prof_z_smooth);
prof_z_secdev_smooth = conv(prof_z_secdev, smooth_filter, 'same');
[pks_secdev, pks_locs_secdev] = findpeaks(prof_z_secdev_smooth);

% Scaffold location in z-axis
[pks_max, ind] = max(pks);
pks_locs_max = pks_locs(ind);

% Top scaffold layer in z-axis
ind = find(pks_locs_secdev < pks_locs_max);
if isempty(ind)
    pks_locs_lb = 1;
else
    [pks_max_lb, max_ind] = max(pks_secdev(ind));
    pks_locs_lb = pks_locs_secdev(ind(max_ind));
end

% Bottom scaffold layer in z-axis
ind = find(pks_locs_secdev > pks_locs_max);
if isempty(ind)
    pks_locs_ub = L;
else
    [pks_max_lb, max_ind] = max(pks_secdev(ind));
    pks_locs_ub = pks_locs_secdev(ind(max_ind));
end

boxCellScaffold.z1 = min(boxCell.z1, pks_locs_lb);
boxCellScaffold.z2 = max(boxCell.z2, pks_locs_ub);
        
%--- Uncomment below if you want figures ---%
% if bShow
%     Xn = boxCellScaffold.x2 - boxCellScaffold.x1+1;
%     Yn = boxCellScaffold.y2 - boxCellScaffold.y1+1;
%     Zn = boxCellScaffold.z2 - boxCellScaffold.z1+1;
% 
%     fig = figure(1);
%     fig_sz = get(fig, 'DefaultFigurePosition');
%     fig.Position = [fig_sz(1) fig_sz(2) fig_sz(3)*1.5 fig_sz(4)];
%     clf(fig);
%     subplot('Position', [0.025 0.12 0.25 0.8]);
%     I = squeeze(Z_scaffold_cropped(:,round(Yn/2),:));
%     I = imresize(I, [round(Xn*0.8/0.35) Xn]);
%     imshow(I, []);
%     axis image
%     title('Vertical Slice');
% 
%     subplot('Position', [0.375 0.12 0.25 0.8]);
%     plot([1:L], prof_z, 'b:', 'DisplayName', 'Z profile');
%     hold on
%     plot([1:L], prof_z_smooth, 'b-', 'linewidth', 2, 'DisplayName', 'Smoothed');
%     legend('show', 'Location', 'northeast');
%     plot(pks_locs_max, prof_z_smooth(pks_locs_max), 'rx');
%     plot(pks_locs_lb, prof_z_smooth(pks_locs_lb), 'ro', 'markerfacecolor', 'r');
%     plot(pks_locs_ub, prof_z_smooth(pks_locs_ub), 'ro', 'markerfacecolor', 'r');
%     hold off
%     xlim([1 L]);
%     view([90 90]);
%     title('Z Profile');
% 
%     subplot('Position', [0.725 0.12 0.25 0.8]);
%     plot([1:L], prof_z_secdev, ':', 'color', [0 0.5 0], 'DisplayName', 'Second derivative');
%     hold on
%     plot([1:L], prof_z_secdev_smooth, '-', 'color', [0 0.5 0], 'linewidth', 2, 'DisplayName', 'Smoothed');
%     legend('show', 'Location', 'northeast');
%     plot(pks_locs_secdev, prof_z_secdev_smooth(pks_locs_secdev), 'ro');
%     plot(pks_locs_lb, prof_z_secdev_smooth(pks_locs_lb), 'ro', 'markerfacecolor', 'r');
%     plot(pks_locs_ub, prof_z_secdev_smooth(pks_locs_ub), 'ro', 'markerfacecolor', 'r');
%     hold off
%     xlim([1 L]);
%     view([90 90]);
%     title('Second Derivative');
% 
%     pause
% end
%------------------------------------------------------------------------%
 