%-------------------------------------------------------------------------------
% Copyright (C) 2005 by Institute for Systems Biology,
% Seattle, Washington, USA.  All rights reserved.
% 
% This source code is distributed under the GNU Lesser 
% General Public License, the text of which is available at:
%   http://www.gnu.org/copyleft/lesser.html
%
%-------------------------------------------------------------------------------
% Module:    basisexp.m
%
% Author:    Daehee Hwang 
%            Institute for Systems Biology
%
% Function:  select a dataset to be used for p-values normalization (pscalef.m)
%-------------------------------------------------------------------------------

function ind=basisexp(x,tp)

if tp=='P'
    x=padjust(x);
    x=-norminv(x);
end
[n,m]=size(x);
z=zeros(n,m);
d=zeros(m,5);

for i=1:m    
    tx=x(find(~isnan(x(:,i))),i);
    nn=length(tx);
    int=[floor(max(30,0.01*nn)):max(1,floor(0.01*nn)):floor(0.5*nn)]';
    maxp=zeros(length(int),1);
    th=length(unique(tx));
    if th>5
        for j=1:length(int)
            [f,xi]=hist(tx,int(j));
            [ii,jj]=max(f);
            maxp(j)=xi(jj);
        end
        d(i,1)=median(maxp);
        sx=sort(tx);
        [ii,jj]=min(abs(sx-d(i,1)));
        d(i,2)=jj/nn;
        if d(i,2)>0.25 & d(i,2)<0.75
            d(i,3)=prctile(tx,d(i,2)*100-25);
            d(i,4)=prctile(tx,d(i,2)*100+25) ;
        else
            if d(i,2)>0.75
                td=1-d(i,2);
            else
                td=d(i,2);
            end
            d(i,3)=prctile(tx,d(i,2)*100-td*100/2);
            d(i,4)=prctile(tx,d(i,2)*100+td*100/2) ;
        end
        d(i,5)=abs(d(i,3)-d(i,1))/abs(d(i,4)-d(i,1));
    else
        d(i,1:4)=NaN; d(i,5)=Inf;
    end    
end
[ii,ind]=min(abs(d(:,5)-1));

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