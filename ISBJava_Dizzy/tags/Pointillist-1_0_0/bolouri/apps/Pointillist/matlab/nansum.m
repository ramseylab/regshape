%
% Copyright (C) 2004 by Institute for Systems Biology,
% Seattle, Washington, USA.  All rights reserved.
% 
% This source code is distributed under the GNU Lesser 
% General Public License, the text of which is available at:
%   http://www.gnu.org/copyleft/lesser.html
%

%----------------------------------------
% Module:    nansum.m
%
% Author:    Daehee Hwang 
%            Institute for Systems Biology
%
% Function:  Computes the sum of all columns
%            of a matrix, discarding NaN values.
%            If any column is *entirely* NaN
%            values, that column's sum is NaN.
%            The result is returned as a row 
%            vector.
%
% Arguments: x - a matrix
%----------------------------------------

function y = nansum(x)

% create an matrix of ones and zeroes, where
% ones correspond to elements of "x" that are NaNs
nans = isnan(x);

% create an array containing the linear indices
% of elements of "nans" that are nonzero; this
% means that "i" is the array of indices of NaN
% elements of "x"
i = find(nans);

% set all the "NaN" elements of "x" to zero
x(i) = zeros(size(i));

% sum each column in "x"
y = sum(x);

% fina any columns that are *all* NaNs
i = find(all(nans));

% those columns which are *all* NaNs, 
% are just assigned to NaN
y(i) = i + NaN;

