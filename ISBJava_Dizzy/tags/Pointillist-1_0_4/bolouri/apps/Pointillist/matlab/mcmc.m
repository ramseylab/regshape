%-------------------------------------------------------------------------------
% Copyright (C) 2005 by Institute for Systems Biology,
% Seattle, Washington, USA.  All rights reserved.
% 
% This source code is distributed under the GNU Lesser 
% General Public License, the text of which is available at:
%   http://www.gnu.org/copyleft/lesser.html
%
%-------------------------------------------------------------------------------
% Module:    mcmc.m
%
% Author:    Daehee Hwang 
%            Institute for Systems Biology
%
% Function:  Monte Carlo simulation to generate random numbers for
% each integration method (for example, Fisher's method)
%-------------------------------------------------------------------------------
function y=mcmc(k,opt,m)

n=[19997,19997,10007,5003];
%n=19997;

%number of random numbers being sampled.
if k>length(n)
    nk=n(end);
else
    nk=n(k);
end

%number of replicates of k RVs.
if nargin<4
    m=5;
end
y=zeros(nk,k,m);
%generating random numbers nxkxm
for i=1:m
    for j=1:k
        cn=0;
        tmy=[];
        %rejection method (Monte Carlo)
        while cn<nk
            r1=initrand(nk-cn,opt);
            r2=rand(nk-cn,1);
            x=compxr2(r1,r2,opt);%seleting only r2<p(x)/p(x)max
            tmy=[tmy;x];
            cn=length(tmy);
        end
        y(:,j,i)=tmy;
    end
end

function y=initrand(n,opt)
x=rand(n,1);
a=[0 -5 -10];%lower limit of each chi2pdf, tpdf, and normpdf.
b=[25 5 10];%upper limit of each chi2pdf, tpdf, and normpdf.
y=a(opt)+(b(opt)-a(opt))*x;%transformation of random numbers into the [a,b] range from [0,1].

function x=compxr2(x,y,opt)
pmax=[0.5 0.39894 0.38803];%the maximum value of each chi2pdf, tpdf, and normpdf.
%rejection method (sampling efficiency is defined by e=int(p(x) in [a,b])/pmax.
if opt==1
    ind=find(y<chi2pdf(x,2)/pmax(opt));
elseif opt==2
    ind=find(y<tpdf(x,9)/pmax(opt));
elseif opt==3
    ind=find(y<normpdf(x,0,1)/pmax(opt));
end
x=x(ind);


