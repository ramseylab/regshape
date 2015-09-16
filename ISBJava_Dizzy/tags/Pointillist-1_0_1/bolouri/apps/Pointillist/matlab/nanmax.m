%
% Copyright (C) 2004 by Institute for Systems Biology,
% Seattle, Washington, USA.  All rights reserved.
% 
% This source code is distributed under the GNU Lesser 
% General Public License, the text of which is available at:
%   http://www.gnu.org/copyleft/lesser.html
%

%----------------------------------------
% Module:    nanmax.m
%
% Author:    Daehee Hwang 
%            Institute for Systems Biology
%
% Function:  
%
%----------------------------------------

function [m,ndx] = nanmax(a,b)
if nargin < 1, 
   error('Requires at least one input.'); 
end
if nargin==1,
  if isempty(a), m =[]; i = []; return, end
  d = find(isnan(a));
  if isempty(d), 
     [m,ndx] = max(a);
  else
     if min(size(a))==1,
       la = length(a);
       a(d) = []; 
        [m,ndx] = max(a);
        if nargout>1, 
           pos = 1:la; pos(d) = [];
           ndx = pos(ndx);
        end
    else
        e = any(isnan(a));
        m = zeros(1,size(a,2)); ndx = m;
        [m(~e),ndx(~e)] = max(a(:,~e)); 
        e = find(e);
        for i=1:length(e), 
           d = isnan(a(:,e(i)));
           aa = a(:,e(i)); aa(d) = [];
           if isempty(aa),
             m(e(i)) = NaN; ndx(e(i)) = 1;
           else
              [m(e(i)),ndx(e(i))] = max(aa);
              if nargout>1,
                 pos = 1:size(a,1); pos(d) = [];
                 ndx(e(i)) = pos(ndx(e(i)));
              end
           end
        end
     end
  end
elseif nargin==2,
  if any(size(a)~=size(b)), error('The inputs must be the same size.'); end
  if nargout>1, error('Too many output arguments.'); end
  if isempty(a), m =[]; i = []; return, end
  d = find(isnan(a));
  a(d) = b(d);
  d = find(isnan(b));
  b(d) = a(d);
  m = max(a,b);
else
  error('Not enough input arguments.');
end  
