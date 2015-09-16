%-------------------------------------------------------------------------------
% Copyright (C) 2005 by Institute for Systems Biology,
% Seattle, Washington, USA.  All rights reserved.
% 
% This source code is distributed under the GNU Lesser 
% General Public License, the text of which is available at:
%   http://www.gnu.org/copyleft/lesser.html
%
%-------------------------------------------------------------------------------
% Module:    pscalef.m
%
% Author:    Daehee Hwang 
%            Institute for Systems Biology
%
% Function:  nomalization of p-values
%-------------------------------------------------------------------------------

function [y,z]=pscalef(x,ind,tp,tail,pbsubs)

if tp=='P'
    x=padjust(x);
    x=-norminv(x);
    tail=1;
end

[n,m]=size(x);
x=qnorms(x,ind);
z=autos(x);
if tail==1
    y=1-normcdf(z);
elseif tail==-1
    y=normcdf(z);
elseif tail==2
    y=2*normcdf(-abs(z));
else
    error('check your tail');
end    

if nargin>4
    y(find(isnan(y)))=psubs;
end

function y=qnorms(x,ind)
[n,m]=size(x);
tn=find(isnan(x(:,ind)));
if length(tn)==0
    ymean=sort(x(:,ind));
else
    ymean=sort(x(find(~isnan(x(:,ind))),ind));
end
rankb=[1:length(ymean)]'/length(ymean);
[n,m]=size(x);
y=NaN*ones(size(x));
for i=1:m
    i
    txind=find(~isnan(x(:,i)));
    tm=x(txind,i);
    [tg,tgi]=sort(tm);
    tgi=txind(tgi);
    rank=[1:length(tg)]'/length(tg);
    for j=1:length(rank)
        [tn,tni]=min(abs(rank(j)-rankb));
        y(tgi(j),i)=ymean(tni);
    end
end

function y=autos(x)
[m,n]=size(x);
y=NaN*zeros(size(x));
for i=1:n
    txind=find(~isnan(x(:,i)));
    tx=x(txind,i);
    mx=median(tx);
    %stdx=mean(abs([prctile(tx,75)-prctile(tx,50) prctile(tx,50)-prctile(tx,25)]))/norminv(0.75);
    stdx=std(tx);
    y(txind,i)=(tx-mx)/stdx;
end

function d=padjust(d)
[n,k]=size(d);
for i=1:k
    tn=find(d(:,i)==0);
    if ~isempty(tn)
        d(find(d(:,i)==0),i)=linspace(1.e-10,1.e-323,length(tn))';
    end
    if ~isempty(find(d(:,i)<1.e-323))
        d(find(d(:,i)<1.e-323),i)=1.e-323;
    end
    if ~isempty(find(d(:,i)>0.99999999999999994))
        d(find(d(:,i)>0.99999999999999994),i)=0.99999999999999994;
    end
end
