function [fout,xout,u]=ksdensity(y,varargin)
if (numel(y) > length(y)), error('X must be a vector'); end
y = y(:);
y(isnan(y)) = [];
n = length(y);
ymin = min(y);
ymax = max(y);
xispecified = false;
if ~isempty(varargin)
   if ~ischar(varargin{1})
      xi = varargin{1};
      varargin(1) = [];
      xispecified = true;
   end
end
okargs = {'width' 'npoints' 'kernel' 'support'    'weights' 'censoring'};
defaults = {[]    []        'normal' 'unbounded'  1/n       false(n,1)};
[emsg,u,m,kernel,support,weight,cens] = statgetargs(okargs, defaults, varargin{:});
error(emsg);

if isnumeric(support)
   if numel(support)~=2
      error('Value of ''support'' parameter must have two elements.');
   end
   if support(1)>=ymin || support(2)<=ymax
      error('Data values must be between lower and upper ''support'' values.');
   end
elseif ischar(support) && length(support)>0
   okvals = {'unbounded' 'positive'};
   rownum = strmatch(support,okvals);
   if isempty(rownum)
      error('Invalid value of ''support'' parameter.')
   end
   support = okvals{rownum};
   if isequal(support,'positive') && ymin<=0
      error('Cannot set support to ''positive'' with non-positive data.')
   end
else
   error('Invalid value of ''support'' parameter.')
end
if numel(weight)==1
   weight = repmat(weight,1,n);
elseif numel(weight)~=n || numel(weight)>length(weight)
   error('Value of ''weight'' must be a vector of the same length as X.');
else
   weight = weight(:)';
end
weight = weight / sum(weight);
if ~all(ismember(cens(:),0:1)) || numel(cens)~=n || numel(cens)>length(cens)
   error('Value of ''censoring'' must be a logical vector of the same length as X');
end
if isequal(support,'unbounded')
   ty = y;
   L = -Inf;
   U = Inf;
elseif isequal(support,'positive')
   ty = log(y);
   L = 0;
   U = Inf;
else
   L = support(1);
   U = support(2);
   ty = log(y-L) - log(U-y);    % same as log((y-L)./(U-y))
end
iscensored = any(cens);
if iscensored
   [F,XF] = ecdf(ty, 'censoring',cens, 'frequency',weight);
   weight = diff(F(:)');
   ty = XF(2:end);
   n = length(ty);
   N = sum(~cens);
   issubdist = (F(end)<1);  % sub-distribution, integrates to less than 1
   ymax = max(y(~cens));
else
   N = n;
   issubdist = false;
end
if (isempty(u)),
   if ~iscensored
      % Get a robust estimate of sigma
      med = median(ty);
      sig = median(abs(ty-med)) / 0.6745;
   else
      % Estimate sigma using quantiles from the empirical cdf
      Xquant = interp1(F,XF,[.25 .5 .75]);
      if ~any(isnan(Xquant))
         % Use interquartile range to estimate sigma
         sig = (Xquant(3) - Xquant(1)) / (2*0.6745);
      elseif ~isnan(Xquant(2))
         % Use lower half only, if upper half is not available
         sig = (Xquant(2) - Xquant(1)) / 0.6745;
      else
         % Can't easily estimate sigma, just get some indication of spread
         sig = ty(end) - ty(1);
      end
   end
   if sig<=0, sig = max(ty)-min(ty); end
   if sig>0
      % Default window parameter is optimal for normal distribution
      u = sig * (4/(3*N))^(1/5);
   else
      u = 1;
   end
end

% Get XI values at which to evaluate the density
if ~xispecified
   % Compute untransformed values of lower and upper evaluation points
   ximin = min(ty) - 3*u;
   if issubdist
      ximax = max(ty);
   else
      ximax = max(ty) + 3*u;
   end
   
   if isequal(support,'positive')
      ximin = exp(ximin);
      ximax = exp(ximax);
   elseif ~isequal(support,'unbounded')
      ximin = (U*exp(ximin)+L) / (exp(ximin)+1);
      ximax = (U*exp(ximax)+L) / (exp(ximax)+1);
   end

   if isempty(m)
      m=100;
   end

   xi = linspace(ximin, ximax, m);

elseif (numel(xi) > length(xi))
   error('XI must be a vector');
end

% Compute transformed values of evaluation points that are in bounds
xisize = size(xi);
fout = zeros(xisize);
xout = xi;
xi = xi(:);
if isequal(support,'unbounded')
   inbounds = true(size(xi));
   txi = xi;
   foldpoint = ymax;
elseif isequal(support,'positive')
   inbounds = (xi>0);
   xi = xi(inbounds);
   txi = log(xi);
   foldpoint = log(ymax);
else
   inbounds = (xi>L) & (xi<U);
   xi = xi(inbounds);
   txi = log(xi-L) - log(U-xi);
   foldpoint = log(ymax-L) - log(U-ymax);
end
m = length(txi);


% If the density is censored at the end, add new points so that
% we can fold them back across the censoring point as a crude
% adjustment for bias
if issubdist
   needfold = (txi >= foldpoint - 3*u);
   nkeep = length(txi);
   nfold = sum(needfold);
   txifold = (2*foldpoint) - txi(needfold);
   txi(end+1:end+nfold) = txifold;
   m = length(txi);
else
   nkeep = length(txi);
   nfold = 0;
end

% Kernel can the name of a function local to here, or an external function
okkernels = {'normal' 'epanechinikov' 'box' 'triangle'};
if isempty(kernel)
   kernel = okkernels{1};
elseif ~(isa(kernel,'function_handle') || isa(kernel,'inline'))
   if ~ischar(kernel)
      error('Smoothing kernel must be a function.');
   end
   knum = strmatch(lower(kernel), okkernels);
   if (length(knum) == 1)
      kernel = okkernels{knum};
   end
end

% Now compute density estimate at selected points
blocksize = 1e6;
if n*m<=blocksize
   % Compute kernel density estimate in one operation
   z = (repmat(txi',n,1)-repmat(ty,1,m))/u;
   f = weight * feval(kernel, z);
else
   % For large vectors, process blocks of elements as a group
   M = max(1,ceil(blocksize/n));
   mrem = rem(m,M);
   if mrem==0, mrem = min(M,m); end
   txi = txi';
   
   f = zeros(1,m);
   ii = 1:mrem;
   z = (repmat(txi(ii),n,1)-repmat(ty,1,mrem))/u;
   f(ii) = weight * feval(kernel, z);
   for j=mrem+1:M:m
      ii = j:j+M-1;
      z = (repmat(txi(ii),n,1)-repmat(ty,1,M))/u;
      f(ii) = weight * feval(kernel, z);
   end
end

% If we added extra points for folding, fold them now
if nfold>0
   % Fold back over the censoring point to give a crisp upper limit
   ffold = f(nkeep+1:end);
   f = f(1:nkeep);
   f(needfold) = f(needfold) + ffold;
   txi = txi(1:nkeep);
   f(txi>foldpoint) = 0;
   
   % Include a vertical line at the end
   if ~xispecified
      xi(end+1) = xi(end);
      f(end+1) = 0;
      inbounds(end+1) = true;
   end
end


% Apply reverse transformation and create return value of proper size
f = f(:) ./ u;
if isequal(support,'positive')
   f = f ./ xi;
elseif isnumeric(support)
   f = f * (U-L) ./ ((xi-L) .* (U-xi));
end
fout(inbounds) = f;
xout(inbounds) = xi;


% -----------------------------
% The following are functions that define smoothing kernels k(z).
% Each function takes a single input Z and returns the value of
% the smoothing kernel.  These sample kernels are designed to
% produce outputs that are somewhat comparable (differences due
% to shape rather than scale), so they are all probability
% density functions with unit variance.
%
% The density estimate has the form
%    f(x;k,h) = mean over i=1:n of k((x-y(i))/h) / h

function f = normal(z)
%NORMAL Normal density kernel.
%f = normpdf(z);
f = exp(-0.5 * z .^2) ./ sqrt(2*pi);

function f = epanechinikov(z)
%EPANECHINIKOV Epanechinikov's asymptotically optimal kernel.
a = sqrt(5);
z = max(-a, min(z,a));
f = .75 * (1 - .2*z.^2) / a;

function f = box(z)
%BOX    Box-shaped kernel
a = sqrt(3);
f = (abs(z)<=a) ./ (2 * a);

function f = triangle(z)
%TRIANGLE Triangular kernel.
a = sqrt(6);
z = abs(z);
f = (z<=a) .* (1 - z/a) / a; 