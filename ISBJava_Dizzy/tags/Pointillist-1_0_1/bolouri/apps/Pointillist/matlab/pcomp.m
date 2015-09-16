% Copyright (C) 2004 by Institute for Systems Biology,
% Seattle, Washington, USA.  All rights reserved.
% 
% This source code is distributed under the GNU Lesser 
% General Public License, the text of which is available at:
%   http://www.gnu.org/copyleft/lesser.html
%

%----------------------------------------
% Module:    pcomp.m
%
% Author:    Daehee Hwang 
%            Institute for Systems Biology
%
% Function:  From observed values from an
%            experiment, compute significances
%            (p-values).
%
%----------------------------------------

function pcomp(filename,dt);
%dt: data type- 0: static; 1: dynamic(timecourse); 2: deletion data.
a=importdata(filename);
x=a.data;%x: a number array;

%extract time point or sample class information from the first row (column headers).
tab=[];
[n,m]=size(a.textdata);
tab=char(a.textdata(1,2:end)');
tn= find(tab(:,1)=='t');
if ~isempty(tn)
    t=[];
    for i=1:size(tab,1)
        tnl=find(tab(i,:)=='t')+1;
        t(i)=str2num(tab(i,tnl));
    end    
    ut=unique(t);
    taby=[];
else
    [conds,ii,cl]=unique(tab,'rows');
    uc=[];taby=[];
    for i=1:length(cl)
        if ~ismember(cl(i),uc)
            uc=[uc cl(i)];
            taby=strvcat(taby,deblank(tab(i,:)));
        end
    end
    cl2=zeros(size(cl));
    for i=1:length(uc)
        cl2(find(cl==uc(i)))=i;        
    end
    cl=cl2;
    ut=[];
end

%Apply a different test depending on the data type
if dt==0
    test=scp(x,cl,conds);%statistical tests for static data
elseif dt==1
    test=tcp(x,t);%cummulative logratio or t test
elseif dt==2
    test=kcp(x,cl);%statistical tests for deletion data
end
tabc=char(a.textdata(2:end,1));%gene names
%write the output file.
filerd(tabc,test,filename,dt,ut,taby);

%statistical tests for static data: logratio test, ttest, Wilcoxon ranksum
%test.
%-------------------------------------------------------------------------
function test=scp(x,cl,conds);
[lz,lp,f]=logratio(x,cl);%logratio test
[tz,tp]=gttest(x,cl,0);%ttest
[wz,wp]=wcrs(x,cl);%Wilcoxon ranksum test
n=length(unique(cl))-1;
test=[];
for i=1:n
    test=[test lz(:,i) lp(:,i) tz(:,i) tp(:,i) wz(:,i) wp(:,i) f(:,i) (lp(:,i).*tp(:,i).*wp(:,i)).^(1/3)];    
end

function [z,p,f]=logratio(x,cl);
n=size(x,1);
uc=unique(cl);
z=NaN*zeros(n,length(uc)-1);f=z;
for j=2:length(uc)
    xx=x(:,find(cl==uc(j)));
    yy=x(:,find(cl==uc(1)));
    ind=find(sum(~isnan(xx),2)>1 & sum(~isnan(yy),2)>1);
    z(ind,j-1)=nanmedian(xx(ind,:)')'-nanmedian(yy(ind,:)')';
    f(ind,j-1)=nanmean(xx(ind,:)')'-nanmean(yy(ind,:)')';
    f(ind,j-1)=sign(f(ind,j-1)).*2.^(abs(f(ind,j-1)));
end
p=kernp(z);

function [z,p]=gttest(x,cl,ls)
n=size(x,1);
uc=unique(cl);
z=NaN*zeros(n,length(uc)-1);
for i=2:length(uc)
    xx=x(:,find(cl==uc(i)));
    yy=x(:,find(cl==uc(1))); 
    ex1=nanstd([xx yy]')';
    ex=find(ex1<1.e-2);
    index=setdiff([1:n]',ex);
    for j=1:length(index);
        tx=xx(index(j),:);
        tx=tx(find(~isnan(tx)));
        ty=yy(index(j),:);    
        ty=ty(find(~isnan(ty)));     
        nx = length(tx); ny = length(ty);
        if nx>1 & ny>1
            difference = mean(tx)-mean(ty);
            dfx = nx-1; dfy = ny-1;  
            s2x = var(tx);
            s2y = var(ty);
            if s2x~=0 & s2y~=0
                if s2x==0
                    s2x=s2y;
                elseif s2y==0
                    s2y=s2x;
                end
                s2xbar = s2x ./ nx;
                s2ybar = s2y ./ ny;               
                se = sqrt(s2xbar + s2ybar);
                z(index(j),i-1) = difference ./ se;
            end
        end
    end
end
if ls==0
    p=kernp(z);
else
    p=[];
end

function [z,p] = wcrs(x,cl)
n=size(x,1);
uc=unique(cl);
z=NaN*zeros(n,length(uc)-1);
for j=2:length(uc)
    xx=x(:,find(cl==uc(j)));
    yy=x(:,find(cl==uc(1)));    
    for i=1:n  
        tx=xx(i,:);ty=yy(i,:);
        tx=tx(find(~isnan(tx)));
        ty=ty(find(~isnan(ty)));           
        if length(tx)>=length(ty)
            txa=ty;tya=tx;
            ns=length(ty);nl=length(tx);
            tx=txa;ty=tya;
        else            
            ns=length(tx);nl=length(ty);
        end
        if ns>1 
            [ranks,tieadj] = tiedrank([tx ty]);
            xrank = ranks(1:ns);
            w = sum(xrank);
            wmean = ns*(ns + nl + 1)/2;
            tiescor = 2 * tieadj / ((ns+nl) * (ns+nl-1));
            wvar  = ns*nl*((ns + nl + 1) - tiescor)/12;
            wc = w - wmean;
            if wvar~=0
                z(i,j-1) = (wc - 0.5 * sign(wc))/sqrt(wvar);
            end
        end
    end
end
p=kernp(z);

function [r,tieadj] = tiedrank(x)
[sx, rowidx] = sort(x);
ranks = 1:length(x);
% Adjust for ties
tieloc = find(~diff(sx));
tieadj = 0;
while (length(tieloc) > 0)
   tiestart = tieloc(1);
   ntied = 1 + sum(sx(tiestart) == sx(tiestart+1:end));
   tieadj = tieadj + ntied*(ntied-1)*(ntied+1)/2;
   ranks(tiestart:tiestart+ntied-1) = tiestart + (ntied-1)/2;
   tieloc(1:ntied-1) = [];
end
r(rowidx) = ranks;
%-----------------------------------------------------------------------

%cummulative logratio or t test
%----------------------------------------------------------------------
function test=tcp(x,t)
ut=unique(t);
n=size(x,1);m=length(ut);
T=zeros(n,m);M=T;
for i=2:m
    tn=find(t==ut(i));th=find(t==ut(1));
    ind=find(sum(~isnan(x(:,tn)),2)>1 & sum(~isnan(x(:,th)),2)>1);
    T(ind,i)=nanmedian(x(ind,tn)')'-nanmedian(x(ind,find(t==ut(1)))')';
    M(ind,i)=gttest(x(ind,[tn th]),[2*ones(length(tn),1); ones(length(th),1)],1);
end
ST=nansum(T')';SM=nansum(M')';
Tp=kernp([T(:,2:end) ST]);Tp=[1-prod(1-Tp(:,1:end-1),2).^(1/(size(Tp,2)-1)) Tp(:,end)];
Mp=kernp([M(:,2:end) SM]);Mp=[1-prod(1-Mp(:,1:end-1),2).^(1/(size(Mp,2)-1)) Mp(:,end)];
test=[T ST Tp M SM Mp];
%-----------------------------------------------------------------------

%test for deletion data
%-----------------------------------------------------------------------
function test=kcp(x,cl)
uc=unique(cl);
n=size(x,1);m=length(uc);
T=zeros(n,m-1);M=T;
for i=2:m
    tn=find(t==ut(i));th=find(t==ut(1));
    ind=find(sum(~isnan(x(:,tn)),2)>1 & sum(~isnan(x(:,th)),2)>1);
    T(ind,i-1)=nanmedian(x(ind,tn)')'-nanmedian(x(ind,find(t==ut(1)))')';
    M(ind,i-1)=gttest(x(ind,[tn th]),[2*ones(length(tn),1); ones(length(th),1)],1);
end
Tp=kernp(T);MT=minfinder(Tp,T);Tp2=kernp(MT);
Mp=kernp(M);MM=minfinder(Mp,M);Mp2=kernp(MM);
test=[T Tp2 M Mp2];

function y=minfinder(x,z)
n=size(x,1);
[ii,jj]=min(x,[],2);
y=zeros(n,1);
for i=1:n
    y(i)=z(i,jj(i));
end
%--------------------------------------------------------------------------

%Compute p values for each metric used for the data
function pvalue=kernp(z)
[n,m]=size(z);
pvalue=ones(size(z));
for j=1:m
    if max(abs(z(:,j)))>100
        tz=z(:,j);
        tz(find(tz<-1))=-log2(abs(tz(find(tz<-1))));
        tz(find(tz>1))=log2(abs(tz(find(tz>1))));
    else
        tz=z(:,j);
    end
    tm=tz(find(~isnan(tz) & tz~=0));
    tp=linspace(min(tm)*0.95,max(tm)*1.05,1000);
    [dest,xint]=ksdensity(tm,tp);
    dest=dest/trapz(xint,dest);
    for i=1:n
        if ~isnan(tz(i)) & tz(i)~=0
            [tmi,tmj]=min(abs(xint-tz(i)));
            if length(xint(1:tmj))==1
                sig=0;
            else
                sig=trapz(xint(1:tmj),dest(1:tmj));
            end
            pvalue(i,j)=abs(2*min(sig,1-sig));
        end
    end
    pvalue(find(pvalue(:,j)==0),j)=min(pvalue(find(pvalue(:,j)~=0),j))/2;
end

%write test outputs to a file
function filerd(tabc,x,filename,dt,ut,taby)
tn=find(filename=='.');
if ~isempty(tn)
    filerename=[filename(1:tn-1) '_test.txt'];
else
    filerename=[filename '_test.txt'];
end
fid=fopen(filerename,'w');
%make the header row
y=[' ' sprintf('\t')];
n=size(tabc,1);m=size(x,2)-1;
if dt==0        
    y=[y 'Log2ratio' sprintf('\t') 'Log2ratio_P' sprintf('\t') 'Ttest' sprintf('\t') 'Ttest_P' sprintf('\t')...
            'Ranksum' sprintf('\t') 'Ranksum_P' sprintf('\t') 'Fold Change' sprintf('\t') 'Averaged_P'];
elseif dt==1
    mm=length(ut);
    th1=['Log2ratio_Cummulative_Sum' sprintf('\t') 'Log2ratio_Cummulative Sum_P' sprintf('\t') 'Log2ratio_Cummulative Sum_P2' sprintf('\t')];
    th2=['T_Cummulative_Sum' sprintf('\t') 'T_Cummulative Sum_P' sprintf('\t') 'T_Cummulative Sum_P2' ];
    th=[];
    for i=1:mm
        th=[th sprintf('t%g\t',ut(i))];
    end
    y=[y th th1 th th2];
elseif dt==2
    mm=(size(x,2)-2)/2;
    th=[];
    for i=1:mm
        th=[th deblank(taby(i,:)) sprintf('\t')];
    end
    y=[y th 'Max_Log2ratio' sprintf('\t') 'Max_Log2ratio_P' sprintf('\t') th 'Max_T' sprintf('\t') 'Max_T_P'];
end
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
