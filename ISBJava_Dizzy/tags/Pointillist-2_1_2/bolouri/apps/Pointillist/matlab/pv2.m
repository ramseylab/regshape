%-------------------------------------------------------------------------------
% Copyright (C) 2005 by Institute for Systems Biology,
% Seattle, Washington, USA.  All rights reserved.
% 
% This source code is distributed under the GNU Lesser 
% General Public License, the text of which is available at:
%   http://www.gnu.org/copyleft/lesser.html
%
%-------------------------------------------------------------------------------
% Module:    pv2.m
%
% Author:    Daehee Hwang 
%            Institute for Systems Biology
%
% Function:  a wrapper to call esa.m and genwfunf.m depending upon
% the four methods mentioned in the main text of the paper
%-------------------------------------------------------------------------------

function y=pv2(d,meth,opt,y);

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

m=5;% m replicates of Random numbers being generated by Monte Carlo using Rejection method.
rand('state',0);
H0y=mcmc(k,opt,m);
param.H0y=H0y;

if meth==1
    %meth 1: H0 testing only with alpha (significance level)=0.05 and
    %suggest alternative significance levels that can be used by detecting
    %the change in the sample distribution.
    tmw=1/k*ones(1,k-1); %initial weight [1/k,...1/k];
    %H0 testing only with alpha (significance level)=0.05;
    param.meth=1;%param.alpha=0.05;
    %find the optimal weight using Enhanced Simulated Annealing (ESA) for
    %alpha=0.05.
    param.alpha=0.05;
    param.mvp=[];    
    minth=zeros(1,k-1);maxth=ones(1,k-1);%min-max for decision variables.
    param
    [wopt,Elowst]=esa(tmw,minth,maxth,d,opt,param);%ESA optimization
    y.wopt1=[wopt 1-sum(wopt)];
    %detecting the change in the sample distribution.
    figure(1)
    S=rand('state');rand('state',0);
    [y.Nsel1,y.sel1]=genwfunf(y.wopt1,opt,d,[],param,1);
    rand('state',S);      
elseif meth==2
    %H0 testing only with the optimal alpha (significance level);
    param.meth=2;param.alpha=0.05;param.mvp=[];
    if nargin>3
        S=rand('state');rand('state',0);
        [h.Nsel,h.sel]=genwfunf(y.wopt1,opt,d,[],param,1);
        rand('state',S);
        alphanew=h.sel.alpha2;
        param.alpha=alphanew;
    end   
    tmw=1/k*ones(1,k-1); %initial weight [1/k,...1/k];    
    %find the optimal alpha using Enhanced Simulated Annealing (ESA)    
    minth=zeros(1,k-1);maxth=ones(1,k-1);%min-max for decision variables.
    err=1;
    iter=0;
    while err>5.e-2
        alphaold=param.alpha;
        iter=iter+1;
        [wopt,Elowst]=esa(tmw,minth,maxth,d,opt,param);%ESA optimization
        S=rand('state');rand('state',0);
        [h.Nsel,h.sel]=genwfunf([wopt 1-sum(wopt)],opt,d,[],param,1);
        rand('state',S);
        alphanew=h.sel.alpha2;
        err=abs(alphanew-alphaold)/alphanew;
        %param.alpha=(alphanew+alphaold)/2
        param.alpha=alphanew
        if iter>4
            disp('the data structure may be not ideal for this method');
            disp('if you are experiencing this even after normalizing p-values');
            disp('please try meth = 1 instead');
            return;
        end
    end
    %detecting the change in the sample distribution.
    figure(1)
    y.wopt2=[wopt 1-sum(wopt)];
    S=rand('state');rand('state',0);
    param.alpha=0.05;
    [y.Nsel2,y.sel2]=genwfunf(y.wopt2,opt,d,[],param,1);
    rand('state',S); 
    pause(1);    
elseif meth==3    
    %fit the theoretical pdf constructed with H0 and H1 pdf to the real
    %data by varing the non-centrality parameter.
    param.meth=3;%param.alpha=0.05;
    %the range of the non-centrality parameter for each integration method.
    %1. Fisher's chi2 test; 2. MG t-test; 3. Stouffer's Z test.
    param.ncrng=[0 5;0 5;0 5];
    if nargin>3
        param.w=y.wopt2;
        param.alpha=y.sel2.alpha2;
        %prior estimates for the numbers of selected elements and their ranges.
        pEnSel=[length(y.sel2.ts) length(y.sel2.t2s)]/n;
        %min-max for decision variables- weights and the non-centrality param.
        kn=length(param.w);
    else
        param2=param;
        param2.meth=2;param2.alpha=0.05;param2.mvp=[];        
        h=pv2(d,2,opt,param2);
        param.w=h.wopt2;
        param.alpha=h.sel2.alpha2;
        %prior estimates for the numbers of selected elements and their ranges.
        pEnSel=[length(h.sel2.ts) length(h.sel2.t2s)]/n;
        %min-max for decision variables- weights and the non-centrality param.
        kn=length(param.w);
    end

    minth=[param.ncrng(opt,1) 0 0.01];
    maxth=[param.ncrng(opt,2) 5 1];
    theta0=[mean([minth(1:end-1);maxth(1:end-1)]) min(pEnSel)]
    %get the optimal fitting parameters and the fraction of elements from H1 in the samples.    
    figure;
    err1=1;err2=1;
    while err1>5.e-2 & err2>5.e-2    
        wold=param.w;alphaold=param.alpha;
        [thetaopt,Elowst]=esa(theta0,minth,maxth,d,opt,param);
        y.e=thetaopt(1);
        y.df=thetaopt(2);
        y.H1frc=thetaopt(end);
        param.df=thetaopt(2);
        param.H1frc=thetaopt(end);  
        S=rand('state');rand('state',0);        
        [h.Nsel,h.sel]=genwfunf(y.wopt2,opt,d,thetaopt(1),param,1);
        rand('state',S);
        
        param2=param;
        param2=param;param2.mvp=[];
        param2.meth=1;param2.alpha=h.sel.alpha3;param2.H0y=H0y;
        S=rand('state');rand('state',0); 
        [y.Nsel3,y.sel3]=genwfunf(y.wopt2,opt,d,[],param2,1);
        rand('state',S);        
        break %normally it converges after the 1st iteration when the weight is close to optimal.       
        
        alphanew=h.sel.alpha3;
        err1=abs(param.alpha-alphanew)/alphanew;
        tmw=1/k*ones(1,k-1); 
        minthin=zeros(1,k-1);maxthin=ones(1,k-1);%min-max for decision variables.
        [wopt,Elowst]=esa(tmw,minthin,maxthin,d,opt,param2);%ESA optimization
        wnew=[wopt 1-sum(wopt)];        
        err2=norm(wold-wnew)/norm(wnew);
        param.w=wnew;
        param.alpha=alphanew;
        param.H0y=H0y;        
    end
    y.wopt3=param.w;      
end