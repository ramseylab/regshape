% Copyright (C) 2004 by Institute for Systems Biology,
% Seattle, Washington, USA.  All rights reserved.
% 
% This source code is distributed under the GNU Lesser 
% General Public License, the text of which is available at:
%   http://www.gnu.org/copyleft/lesser.html
%

%----------------------------------------
% Module:    mainpointillist.m
%
% Author:    Daehee Hwang 
%            Institute for Systems Biology
%
% Function:  
%
%----------------------------------------

function [out,stat]=mainpointillist(filename,cutoff)

if nargin < 2
    cutoff=0.05;
end
a=importdata(filename);
x=a.data;%x: a number array;
tab=char(a.textdata(1,2:end)');
tabc=char(a.textdata(2:end,1));

n=size(x,1);
tp=floor(n*0.15);
[stat,out]=pointillist(x,cutoff);
R=out.R(:,2);
stat

G=out.G;

if n<11*length(G)
    rand('seed',0);
    iter=1
    err(iter)=1;
    C=setdiff([1:n]',G);
    terr=err(end)>0.01;
    [ii,jj]=sort(R);
    tmp=x(jj(end:-1:end-tp+1),:);
    while terr
        OR=R;
        iter=iter+1
        [stat,out]=pointillist(x,cutoff,tmp);  
        R=out.R(1:n,2);
        [ii,jj]=sort(R);
        tmp=x(jj(end:-1:end-tp+1),:);
        err(iter)=norm((OR-R)./R)^2
        stat  
        if iter==2
            err(iter-1)=2*err(iter);
        end
        terr=err(end)>0.01 & err(iter) <= err(iter-1);
        if ~terr
            out=oldout;stat=oldstat;
            break;
        end
        G=out.G(find(out.G<=n));
        C=setdiff([1:n]',G);
        out.G=G;out.C=C;
        oldout=out;oldstat=stat;
    end
end
save stat.txt stat -ascii -tabs;
y=[x R];
filerd(y,tab,tabc,filename);

function filerd(x,tab,tabc,filename)
tn=find(filename=='.');
if ~isempty(tn)
    filerename=[filename(1:tn-1) '_point.txt'];
else
    filerename=[filename '_point.txt'];
end
fid=fopen(filerename,'w');
%make the header row
y=[' ' sprintf('\t')];
n=size(tabc,1);m=size(x,2)-1;
for i=1:size(tab,1)
    y=[y deblank(tab(i,:)) sprintf('\t')];
end
y=[y 'joint_P'];
fprintf(fid,'%s\n',y);
%define the format for numeric data
fformat=[];
for j=1:size(x,2)
    if j<size(x,2)
        fformat=[fformat '%6.4f\t'];
    else
        fformat=[fformat '%6.4f\n'];
    end
end
for i=1:n-1
    tab=[deblank(tabc(i,:)) sprintf('\t')];
    fprintf(fid,'%s',tab); 
    fprintf(fid,fformat,x(i,:));
end
fclose(fid);    