%-------------------------------------------------------------------------------
% Copyright (C) 2005 by Institute for Systems Biology,
% Seattle, Washington, USA.  All rights reserved.
% 
% This source code is distributed under the GNU Lesser 
% General Public License, the text of which is available at:
%   http://www.gnu.org/copyleft/lesser.html
%
%-------------------------------------------------------------------------------
% Module:    nprampv2.m
%
% Author:    Daehee Hwang 
%            Institute for Systems Biology
%
% Function:  non-parametric method for integrating datasets
%-------------------------------------------------------------------------------
function [out,stat]=nprampv2(p,alpha,objc,opt)
%alpha: use the alpha obtained by running "method=2" or "method=3" in pv2.m
%objc: 1-alpha-beta; use 0.99-alpha or 0.95-alpha.
k=size(p,2);
H0y=mcmc(k,opt,1);
H0y=H0y(:,:,1);

G=[];
[n,m]=size(p);
%adjestment for too high and low p-values
for i=1:m
    if ~isempty(find(p(:,i)<1.e-323))
        p(find(p(:,i)<1.e-323),i)=1.e-323;
    end
    if ~isempty(find(p(:,i)>0.99999999999999994))
        p(find(p(:,i)>0.99999999999999994),i)=0.99999999999999994;
    end    
    G=unique([G;find(p(:,i)<alpha)]);
end

C=setdiff([1:n]',G);
%p-value computation for observations from H0.
if opt==1
    H0p=1-chi2cdf(H0y,2);
elseif opt==2
    H0p=1-tcdf(H0y,9);
elseif opt==3
    H0p=2*normcdf(-abs(H0y));
end
%objective function computation for initial guesses.
[threold,tkold]=cepup(p,G,H0p,ones(1,m)/m,0,objc,[0.01 0.01],opt);

iter=1;
TG=[];TGy=[];
tk=zeros(1,m+2);
while tk(end-1)<objc
    iter;
    %adaptive scheme for the rate of false positive removal.
    if iter < 2
        chngiter=[0.01 0.01];
    else
        chngiter=[fchng nGchng];
    end
    nGold=length(G); 
    %weight computation.
    w(iter,:)=weval(p(G,:),H0p,opt);
    %objective function computation.
    [thre(iter,:),tk,G,C,Gy,Cy,rGp,frc(iter),cr]=cepup(p,G,H0p,w(iter,:),iter,objc,chngiter,opt); 
    threnew=thre(iter,:);tknew=tk;
    fchng=abs(tknew(end-1)-tkold(end-1));
    threold=threnew;tkold=tknew;
    
    %saving intermediate sets of selected genes and identification of false
    %negative elements assuming the iteration stopped in the current
    %iteration.
    tmG=G;tmGy=Gy;tmC=C;tmCy=Cy;
    jj=find(tmCy>=min(rGp,cr));jj2=find(tmGy>=rGp);
    tmG=[tmG(jj2);tmC(jj)]';tmGy=[tmGy(jj2);tmCy(jj)]';
    [tmGy,ii]=msort(tmGy);tmG=tmG(ii);
    TG=subcat(TG,tmG);TGy=subcat(TGy,tmGy);
    G=G(find(Gy>rGp));
    nGnew=length(G);
    nGchng=abs(1-nGnew/nGold);
    Gy=Gy(find(Gy>rGp));
    th=length(find(Gy>rGp));
    [Gy,ii]=sort(Gy);G=G(ii);
    stat(iter,:)=[tk(1:end-1) min(rGp,cr) length(G) th length(tmG)];
    iter=iter+1;
end

titer=iter-1;
stat;
%saving pointillist results.
out.w=w(titer,:);out.G=TG(titer,find(TG(titer,:)~=0));
Ry=realcomp(out.w,p,opt);
Sy=simcomp(out.w,H0y,opt);Sy=Sy(:,1);
%scaling the combined p-values when Fisher's method was used.
if opt==1
    Ry=log(Ry);
    Sy=log(Sy);
end
phat=gamfit(exp(Sy));
out.p=1-gamcdf(exp(Ry),phat(1),phat(2));

function [thre,stat,G,C,Gy,Cy,rGp,frc,cr]=cepup(p,G,H0p,w,iter,objc,frciter,opt)
n=size(p,1);
C=setdiff([1:n]',G);
Gy=cpestim(p(G,:),w,opt);
Cy=cpestim(p(C,:),w,opt);
H0y=cpestim(H0p,w,opt);
%scaling the combined p-values when Fisher's method was used.
if opt==1
    Gy=log(Gy);Cy=log(Cy);H0y=log(H0y);
end
[Gy,ii]=msort(Gy);G=G(ii);
[Cy,ii]=msort(Cy);C=C(ii);
%computing non-overlapping area between potential H0 and H1.
int=[-Inf; unique([Cy;Gy;H0y]); Inf];
txx=histc(Gy,int);
txx=cumsum(txx)/sum(txx);
tyy=histc(H0y,int);
tyy=cumsum(tyy)/sum(tyy);
[stat4,cr]=max(tyy-txx);%stat4: objective function (non-overlapping area) and cr: critical value.
cr=int(cr);
frc=max(min((objc-stat4)/objc,0.01),0.001);
if objc-stat4>0.01
    rGp=Gy(round(length(G)*(1-frc)));
else
    rGp=Gy(end-1);
end
thre=[cr Gy(round(length(G)*(1-frc))) rGp];
stat=[iter w stat4 rGp];

function w = weval(g,c,opt)
[m,n]=size(g);
if opt==1
    x=-2*log(g);y=-2*log(c);
elseif opt==2
    x=-log(g./(1-g));y=-log(c./(1-c));
elseif opt==3
    x=-norminv(g);y=-norminv(c);
end    
rndcum=[];cums=[];
int=[-Inf; unique([x(:);y(:)]); Inf];
% computation of non-overlapping area between H1 and H0 using 
% observations of individual variables.
for i=1:n    
    tm=histc(y(:,i),int);
    tm=cumsum(tm)/sum(tm);
    rndcum(:,i)=tm(1:end-1);
    tm=histc(x(:,i),int);
    tm=cumsum(tm)/sum(tm);
    cums(:,i)=tm(1:end-1);
    ar(i)=max(rndcum(:,i)-cums(:,i));  
end
[kk,kki]=max(ar);
w=zeros(1,n);
%adjustment of non-overlapping area compared to the largest non-overlapping
%area.
for i=1:n
    w(i)=-max(cums(:,i)-cums(:,kki));
end
w=w+ar(kki);
w=w/sum(w);

function y=cpestim(x,w,opt)
[n,k]=size(x);
%multiplication const for Fisher's, MG, and Stouffer's methods.
if opt==1
    const=-2;
elseif opt==2
    const=-sqrt((15*k+12)/((5*k+2)*k*pi^2));
elseif opt==3
    const=1/sqrt(k);
end
%interval generation & combining k random numbers according to methods
%above.
y=zeros(n,1);
for i=1:k
    if opt==1
        y=y+const*w(i)*log(x(:,i));
    elseif opt==2
        y=y+const*w(i)*log(x(:,i)./(1-x(:,i)));
    elseif opt==3      
        y=y-const*w(i)*norminv(x(:,i));
    end
end

function y=realcomp(w,x,opt);
[n,k]=size(x);
%multiplication const for Fisher's, MG, and Stouffer's methods.
if opt==1
    const=-2;
elseif opt==2
    const=-sqrt((15*k+12)/((5*k+2)*k*pi^2));
elseif opt==3
    const=1/sqrt(k);
end
%interval generation & combining k random numbers according to methods
%above.
y=zeros(n,1);
for i=1:k
    if opt==1
        y=y+const*w(i)*log(x(:,i));
    elseif opt==2
        y=y+const*w(i)*log(x(:,i)./(1-x(:,i)));
    elseif opt==3      
        y=y-const*w(i)*norminv(x(:,i));
    end
end

function y=simcomp(w,x,opt);
[n,k,m]=size(x);
%multiplication const for Fisher's, MG, and Stouffer's methods.
if opt==1
    const=1;
elseif opt==2
    const=sqrt((15*k+12)/((5*k+2)*k*pi^2))/sqrt(27/(7*pi^2));
elseif opt==3
    const=sqrt(1/k);
end
%interval generation & combining k random numbers according to methods
%above.
y=zeros(n,m);
for i=1:m    
    for j=1:k
        y(:,i)=y(:,i)+const*w(j)*x(:,j,i);
    end
end

function y=subcat(y,x)
[n,m]=size(y);
k=length(x);
if m<k
    tn=zeros(n,k-m);
    y=[y tn];
elseif m>k
    tn=zeros(1,m-k);
    x=[x tn];
end;
y=[y;x];

function [x,ii]=msort(x)
[tm,ii]=sort(x);
ii=ii(end:-1:1);
x=x(ii);
