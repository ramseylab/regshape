%
% Copyright (C) 2004 by Institute for Systems Biology,
% Seattle, Washington, USA.  All rights reserved.
% 
% This source code is distributed under the GNU Lesser 
% General Public License, the text of which is available at:
%   http://www.gnu.org/copyleft/lesser.html
%

%--------------------------------------------------------------------------------
% Module:    nanmean.m
%
% Author:    Daehee Hwang 
%            Institute for Systems Biology
%
% Function: for a matrix "x", returns a row vector "y".  Each element
%            of "y" is the mean of the corresponding column of "x".
%            If the corresponding column of "x" contains any NaNs,
%            they are ignored in computing the mean.  If a
%            corresponding column of "x" is all NaNs, the resulting
%            element of "y" will be NaN.
%            
% Arguments: x - a matrix for which the mean
%            of each column will be computed
%--------------------------------------------------------------------------------

function y = nanmean(x)
if isempty(x) % Check for empty input.
    y = NaN;
    return
end
% Replace NaNs with zeros.
nans = isnan(x);
i = find(nans);
x(i) = zeros(size(i));
% count terms in sum over first non-singleton dimension
dim = find(size(x)>1);
if isempty(dim)
   dim = 1;
else
   dim = dim(1);
end
count = sum(~nans,dim);
% Protect against a column of all NaNs
i = find(count==0);
count(i) = 1;
y = sum(x,dim)./count;
y(i) = NaN;
