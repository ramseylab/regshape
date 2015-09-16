%
% Copyright (C) 2004 by Institute for Systems Biology,
% Seattle, Washington, USA.  All rights reserved.
% 
% This source code is distributed under the GNU Lesser 
% General Public License, the text of which is available at:
%   http://www.gnu.org/copyleft/lesser.html
%

%----------------------------------------
% Module:    nanmedian.m
%
% Author:    Daehee Hwang 
%            Institute for Systems Biology
%
% Function:  Returns a row vector whose elements
% are the medians of the corresponding column
% vector of the argument matrix "x".  If any
% column is comprised entirely of NaNs, the
% corresponding element of the return vector is
% set to NaN.  For a column vector that has
% one or more NaNs but is not comprised entirely
% of NaNs, the NaNs are set to zero in the 
% column vector, and then the median is computed.
%
% Argument:  x - a matrix for which the median
% of each column vector is to be computed
%----------------------------------------

function y = nanmedian(x)
[m,n] = size(x);
x = sort(x); 
nans = isnan(x);
i = find(nans);
x(i) = zeros(size(i));
if min(size(x))==1,
  n = length(x)-sum(nans);
  if n == 0
    y = NaN;
  else
    if rem(n,2)     
      y = x((n+1)/2);
    else            
      y = (x(n/2) + x(n/2+1))/2;
    end
  end
else
  n = size(x,1)-sum(nans);
  y = zeros(size(n));
  odd = find(rem(n,2)==1 & n>0);
  idx =(n(odd)+1)/2 + (odd-1)*m;
  y(odd) = x(idx);
  even = find(rem(n,2)==0 & n>0);
  idx1 = n(even)/2 + (even-1)*m;
  idx2 = n(even)/2+1 + (even-1)*m;
  y(even) = (x(idx1)+x(idx2))/2;
  i = find(n==0);
  y(i) = i + nan;
end
