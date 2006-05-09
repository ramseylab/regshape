%-------------------------------------------------------------------------------
% Copyright (C) 2005 by Institute for Systems Biology,
% Seattle, Washington, USA.  All rights reserved.
% 
% This source code is distributed under the GNU Lesser 
% General Public License, the text of which is available at:
%   http://www.gnu.org/copyleft/lesser.html
%
%-------------------------------------------------------------------------------
% Module:    genwfunf.m
%
% Author:    Daehee Hwang 
%            Institute for Systems Biology
%
% Function:  select elements given a weight vector and alpha
%-------------------------------------------------------------------------------

function [f,g]=genwfunf(w,opt,d,e,param,out)
w=w/sum(w); % a given weight vector.
k=length(w); %number of data types.

meth=param.meth;
alpha=param.alpha;
H0y=param.H0y;%Random numbers from H0 (null hypothesis)

if meth<3
    %Meth=1: H0 based testing
    %estimate of averaged cdf function (from m replicates).
    [H0cdf,H0pdf,H0int]=cdfgen(w,H0y,opt);
    
    %estimate of a cutoff value with alpha =0.05.
    c=cutoffgen(H0cdf,H0int,alpha);
    dpc=intfun(d,opt,w,c);
    t1s=find(dpc>d(:,end));
    g.ts=t1s;
    
    if ~isempty(param.mvp)
        th=sum(d(t1s,:)==param.mvp,2);
        tnh=length(find(th>=1));
        if tnh>0
            f=-(length(t1s)-tnh);
        end
    else
        f=-length(t1s);
    end
    
    if out==1
        Ry=realcomp(w,d,opt);
        Sy=simcomp(w,H0y,opt);Sy=Sy(:,1);OSy=Sy;
        %scaling the combined p-values when Fisher's method was used.
        if opt==1
            Ry=log(Ry);
            Sy=log(Sy);
        end

        if opt==1
            phat=gamfit(exp(Sy));
            g.p=1-gamcdf(exp(Ry),phat(1),phat(2));
        else
            [mhat,shat]=normfit(Sy);
            g.p=1-normcdf(Ry,mhat,shat);
        end
    end
            
    if out==1 & (meth==2)
        %Meth=2: vary alpha to find an optimal alpha to separate H1 from H0.
        %combined p values of real data
        Ry=realcomp(w,d,opt);
        Sy=simcomp(w,H0y,opt);Sy=Sy(:,1);OSy=Sy;
        %scaling the combined p-values when Fisher's method was used.
        if opt==1
            Ry=log(Ry);
            Sy=log(Sy);
        end
        %The intervals common for 1-2) both sets of RNs from H0 and H1
        %, and 3) combined p values.
        
        Tint=[-Inf; unique([Sy;Ry]); Inf];
        %Computing cdfs for H0 and real data p values.
        tm=histc(Ry,Tint);
        Rcdf=cumsum(tm)/sum(tm);
        Rcdf=Rcdf(1:end-1);
        tm=histc(Sy,Tint);
        H0cdf=cumsum(tm)/sum(tm);
        H0cdf=H0cdf(1:end-1);
        Tint=Tint(1:end-1);
        if opt==1
            Tint(1)=Tint(2)/2;
        else
            Tint(1)=Tint(2)-0.1;
        end   
        %correct non-ideality in the Ry distribution.
        [Rcdf,cpi,cpalpha]=cdfcorc(Ry,Sy,Rcdf,H0cdf,Tint);
        %density ratio of observed (1-Rcdf) vs. expected (1-H0cdf)
        [uH0cdf,uH0cdfi]=unique(H0cdf);
        cumSden=[1-uH0cdf 1-Rcdf(uH0cdfi)];        
        %tn=max(find(cumSden(:,1)~=0 & cumSden(:,2)~=0));
        tn1=min(find(cumSden(:,2)>=max(0.0001,10/size(Ry,1))));
        tn2=max(find(cumSden(:,1)>=max(0.0001,10/size(H0y,1))));
        cumSden=cumSden(tn1:tn2,:);
        cumSden=[cumSden cumSden(:,2)./cumSden(:,1)];
        cumSden=cumSden(end:-1:1,:);
        %detect a density change        
        tm=cumSden(:,[1 3]);   
        tm=tm*inv(diag(max(tm)));
        tg=sum(tm.^2,2);
        [ii,jj]=min(tg);
        alpha2=cumSden(jj,1);
        if alpha2>cpalpha & ~isempty(cpalpha)
            alpha2=cpalpha;
        end
        tm=sort(OSy);
        c2=tm(round(length(tm)*(1-alpha2)));
        dpc=intfun(d,opt,w,c2);
        t2s=find(dpc>d(:,end));
        g.t2s=t2s;g.alpha2=alpha2;
        c=[c c2];
        if opt==1
            phat=gamfit(exp(Sy));
            g.p2=1-gamcdf(exp(Ry),phat(1),phat(2));
        else
            [mhat,shat]=normfit(Sy);
            g.p2=1-normcdf(Ry,mhat,shat);
        end
    end
elseif meth==3    
    %fitting the T pdf (combination of H0 and H1 pdfs) to the real data pdf
    [tmcdf,tmpdf,H0int]=cdfgen(w,H0y,opt);
    %Random numbers from H1 (alternative hypothesis)
    df=param.df;     
    %[tmcdf,tmpdf,H1int]=cdfgen(w,H1y,opt);
    %combined p values of real data
    Ry=realcomp(w,d,opt);
    Sy=simcomp(w,H0y,opt);Sy=Sy(:,1);OSy=Sy;   
    %scaling the combined p-values when Fisher's method was used.
    if opt==1
        Ry=log(Ry);
        Sy=log(Sy);
    end       
    %The intervals common for 1-2) both sets of RNs from H0 and H1
    %, and 3) combined p values.
    Tint=[-Inf; unique([Sy;Ry]); Inf]; 
    %Computing cdfs for mixture modeling
    tm=histc(Ry,Tint);
    Rcdf=cumsum(tm)/sum(tm);
    Rcdf=Rcdf(1:end-1);    
    tm=histc(Sy,Tint);
    H0cdf=cumsum(tm)/sum(tm);
    H0cdf=H0cdf(1:end-1);
    Tint=Tint(1:end-1);
    Tint(1)=Tint(2)-abs(diff(Tint(2:3)));
    %H1cdf modeling
    H1cdf=normcdf(Tint,e,df);%normal distribution
    %H1cdf=gamcdf(Tint,e,df);%gamma distribution
    %correct non-ideality in the Ry distribution.
    [Rcdf,cpi,cpalpha]=cdfcorc(Ry,Sy,Rcdf,H0cdf,Tint);
    H1frc=param.H1frc;
    %merging H0 and H1 pdfs using a H1 fraction.       
    Tcdf=(1-H1frc)*H0cdf+H1frc*H1cdf;    
    % objective function as difference btw. Rcdf and Tcdf.
    [iii,iij]=unique(Rcdf);
    tmdif=abs(Rcdf(iij)-Tcdf(iij));    
    f=sum(tmdif.^2);    
    [ii,jj]=max(H0cdf-H1cdf);
    g.alpha3=1-H0cdf(jj);
    
    %display intermediate fitting results for meth 2.
    if nargin>5 & (meth==3 & out==1)
        figure;
        [tmx,tmw]=lgwt(200,min(Tint)*0.95,max(Tint)*1.05);
        tmx=sort(tmx);
        [f,xi]=ksdensity(Sy,tmx);
        f1=zeros(size(tmx));
        f1=normpdf(tmx,e,df);
        [n,ni]=hist(Ry,50);
        bar(ni,n/max(n)*max((1-H1frc)*f+H1frc*f1),'g');hold on;        
        plot(tmx,(1-H1frc)*f,'g-',tmx,H1frc*f1,'m-',tmx,(1-H1frc)*f+H1frc*f1,'c-');
        pause(0.75)        
        
        [ii,jj]=max(H0cdf-H1cdf);
        g.alpha3=1-H0cdf(jj);
        tm=sort(OSy);
        c3=tm(round(length(tm)*(1-g.alpha3)));
        c=c3;
        if opt==1
            phat=gamfit(exp(Sy));
            g.p3=1-gamcdf(exp(Ry),phat(1),phat(2));
        else
            [mhat,shat]=normfit(Sy);
            g.p3=1-normcdf(Ry,mhat,shat);
        end
    elseif nargin>5 & (meth==3 & out==2)
        plot(Tint,Rcdf,'b.--',Tint,Tcdf,'r.-');
        pause(0.75);
    end
end

%vidualizing the outputs.
if nargin>5 & (meth~=3 & out==1)
    figure
    vidz(d,opt,w,c,param);
end

function vidz(d,opt,w,c,param);
meth=param.meth;
%pseudo P variable generation
if length(w)>2
    px=pseudornd(d,opt,w);
    pw=[1-w(end) w(end)];
else
    px=d(:,1);
    pw=w;
end
%generation of grids for decision boundaries
[xx,wx]=lgwt(100,0,1);
%plotting decision boundarys in 2d data space (the real p-value for 
%the last data type and the pseudo P values generated from the remaining 
%real P values (see pseudornd.m). 
plot(px,d(:,end),'g.'); hold on;
if meth==1
    ZC=intfun(xx,opt,pw,c);   
    dpc=intfun(d,opt,pw,c);   
    t1s=find(dpc>d(:,end));
    plot(px(t1s),d(t1s,end),'mo');
    plot(xx,ZC,'b-','linewidth',2.5);hold on      
elseif meth==2 | meth==3
    tab=['b--';'m--';'r--'];
    ZC=[];
    for i=1:length(c)
        ZC(:,i)=intfun(xx,opt,pw,c(i));
        plot(xx,ZC(:,i),tab(i,:),'linewidth',2.5);
    end
end
axis([0 1 0 1]);

function [y,cpi,cpalpha]=cdfcorc(x,c,xc,cc,Tint)
[px,pxq]=qdistpeak(x);
[pc,pcq]=qdistpeak(c);
y=xc;
[xii,xjj]=min(abs(xc-pxq));
[cii,cjj]=min(abs(cc-pcq));
n=length(xc);
if pc>=px
    y(1:cjj)=cc(1:cjj)*pxq/pcq;
    njj=n-cjj;
    intjj=round(linspace(xjj+1,n,njj));
    y(cjj+1:end)=xc(intjj);
end    
xc=supsmu(Tint,xc);
cc=supsmu(Tint,cc);
sdif=sign(xc-cc);
tm1=find(sdif==-1);tm1c=find(sdif==1 | sdif==0);
tg1=intersect(tm1,tm1c-1);
tm2=find(sdif==1);tm2c=find(sdif==-1 | sdif==0);
tg2=intersect(tm2,tm2c-1);
cp=[tg1;tg2];
tn=sort(cp);
if length(tn)>2
    cpi=tn(end-1);
    cpalpha=1-cc(cpi);
else
    cpi=[];
    cpalpha=1;
end
%y=supsmu(Tint,y);

function [px,pxq]=qdistpeak(x)
x(find(x==0))=min(x(find(x~=0)));
[n,m]=size(x);
int=[floor(max(30,0.01*n)):floor(0.01*n):floor(0.5*n)]';
maxp=zeros(length(int),1);
for j=1:length(int)
    [f,xi]=hist(x,int(j));
    [ii,jj]=max(f);
    maxp(j)=xi(jj);
end
px=median(maxp);
sx=sort(x);
[ii,jj]=min(abs(sx-px));
pxq=jj/n;

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

function g=pseudornd(p,opt,w)
[n,k]=size(p);
g=zeros(n,1);
if opt==1
    for i=1:k-1
        g=g+w(i)*log(p(:,i));
    end
    g=exp(g/(1-w(end)));
elseif opt==2
    for i=1:k-1
        g=g+w(i)*log(p(:,i)./(1-p(:,i)));
    end
    g=g/(1-w(end));
    g=1./(exp(-g)+1);
elseif opt==3    
    for i=1:k-1
        g=g-w(i)*norminv(p(:,i));
    end
    g=1-normcdf(g/(1-w(end)));
end

function g=intfun(p,opt,w,c)
k=length(w);
   
if opt==1
    g=-c/2;
    for i=1:k-1
        g=g-w(i)*log(p(:,i));
    end
    g=exp(g/w(end));
elseif opt==2
    const=-sqrt((15*k+12)/((5*k+2)*k*(pi^2)));
    g=-c/const;
    for i=1:k-1
        g=g+w(i)*log(p(:,i)./(1-p(:,i)));
    end
    g=1./(exp(g/w(end))+1);
elseif opt==3
    g=c*sqrt(k);
    for i=1:k-1
        g=g+w(i)*norminv(p(:,i));
    end
    g=1-normcdf(g/w(end));
end

function c=cutoffgen(cdf,int,alpha,beta)
if nargin<4
    %finding a cutoff for H0 pdf
    tn=round(0.001*length(int));
    cdf(tn+1:end-tn-1)=supsmu(int(tn+1:end-tn-1),cdf(tn+1:end-tn-1));    
    [mindev,mindevind]=min(abs(cdf-(1-alpha)));
    if cdf(mindevind)>=1-alpha
        mindevind=mindevind-1;
    end
    Ux=int(mindevind+1);Lx=int(mindevind);
    Uy=cdf(mindevind+1);Ly=cdf(mindevind);
    if abs(Uy-Ly)<1.e-4
        [mindev,mindevind]=min(abs(cdf-(1-alpha)));
        c=int(mindevind);
    else
        c=Lx+(Ux-Lx)/(Uy-Ly)*(1-alpha-Ly);
    end    
else
    %finding a cutoff for H1 pdf
    [mindev,mindevind]=min(abs(cdf-beta));
    if cdf(mindevind)>=beta
        mindevind=mindevind-1;
    end
    Ux=int(mindevind+1);Lx=int(mindevind);
    Uy=cdf(mindevind+1);Ly=cdf(mindevind);
    if abs(Uy-Ly)<1.e-4
        [mindev,mindevind]=min(abs(cdf-beta));
        c=int(mindevind);
    else
        c=Lx+(Ux-Lx)/(Uy-Ly)*(beta-Ly);
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

function [y,tx,int]=cdfgen(w,x,opt,int);
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
tx=zeros(n,m);
for i=1:m    
    for j=1:k
        tx(:,i)=tx(:,i)+const*w(j)*x(:,j,i);
    end
end

if nargin<4
    nn=max([3 size(tx,2) k-1]);
    nn=min(nn,size(tx,2));
    tg=tx(:,1:nn);
    int=[-Inf; sort(tg(:)); Inf];
    int(2)=min(tx(:));
    int(end-1)=max(tx(:));
end
%generation of histograms
nc=zeros(length(int),m);
tmz=nc;
for i=1:m
    tmz(:,i)=histc(tx(:,i),int);
    %nc(:,i)=cumsum(tmz(:,i))/sum(tmz(:,i));
end
%nc=nc(1:end-1,:);
int=int(1:end-1);
if opt==1
    int(1)=int(2)/2;
else
    int(1)=int(2)-0.1;
end
%estimate of cdf by averaging the m cdfs.
z=mean(tmz,2);
nc=cumsum(z)/sum(z);
z=z(1:end-1,:);
y=nc(1:end-1,:);

function [x,w]=lgwt(N,a,b)
N=N-1;
N1=N+1; N2=N+2;
xu=linspace(-1,1,N1)';
% Initial guess
y=cos((2*(0:N)'+1)*pi/(2*N+2))+(0.27/N1)*sin(pi*xu*N/N2);
% Legendre-Gauss Vandermonde Matrix
L=zeros(N1,N2);
% Derivative of LGVM
Lp=zeros(N1,N2);
% Compute the zeros of the N+1 Legendre Polynomial
% using the recursion relation and the Newton-Raphson method
y0=2;
% Iterate until new points are uniformly within epsilon of old points
while max(abs(y-y0))>eps   
    L(:,1)=1;
    Lp(:,1)=0;    
    L(:,2)=y;
    Lp(:,2)=1;    
    for k=2:N1
        L(:,k+1)=( (2*k-1)*y.*L(:,k)-(k-1)*L(:,k-1) )/k;
    end 
    Lp=(N2)*( L(:,N1)-y.*L(:,N2) )./(1-y.^2);
    y0=y;
    y=y0-L(:,N2)./Lp;    
end
% Linear map from[-1,1] to [a,b]
x=(a*(1-y)+b*(1+y))/2;      
% Compute the weights
w=(b-a)./((1-y.^2).*Lp.^2)*(N2/N1)^2;
    
