%
% Copyright (C) 2004 by Institute for Systems Biology,
% Seattle, Washington, USA.  All rights reserved.
% 
% This source code is distributed under the GNU Lesser 
% General Public License, the text of which is available at:
%   http://www.gnu.org/copyleft/lesser.html
%

%----------------------------------------
% Module:    nanstd.m
%
% Author:    Daehee Hwang 
%            Institute for Systems Biology
%
% Function:  For a matrix "x", returns a row
% vector "y".  Each element of "y" is the standard
% deviation of the corresponding column of "x". If
% the corresponding column of "x" contains any NaNs,
% they ar eignored in computing the standard deviation.
% However, if a column of "x" is alll NaNs, the
% resulting element of "y" will be set to NaN.
%
% Arguments:  x - a matrix for which the
% standard deviation is to be computed for 
% each column.
%----------------------------------------

function y = nanstd(x)
nans = isnan(x);
i = find(nans);
avg = nanmean(x);
if min(size(x))==1,
   count = length(x)-sum(nans);
   x = x - avg;
else
   count = size(x,1)-sum(nans);
   x = x - avg(ones(size(x,1),1),:);
end
x(i) = zeros(size(i));
i = find(count==0);
count(i) = ones(size(i));
y = sqrt(sum(x.*x)./max(count-1,1));
y(i) = i + NaN;
