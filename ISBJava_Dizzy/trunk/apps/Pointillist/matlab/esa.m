%-------------------------------------------------------------------------------
% Copyright (C) 2005 by Institute for Systems Biology,
% Seattle, Washington, USA.  All rights reserved.
% 
% This source code is distributed under the GNU Lesser 
% General Public License, the text of which is available at:
%   http://www.gnu.org/copyleft/lesser.html
%
%-------------------------------------------------------------------------------
% Module:    esa.m
%
% Author:    Daehee Hwang 
%            Institute for Systems Biology
%
% Function:  enhanced simulated annealing optimizer
%-------------------------------------------------------------------------------

function [thetaopt,Elowst]=esa(theta,minth,maxth,d,opt,param)

p=length(theta);
randn('state',0) %initialize RN generators.
rand('state',0)
stpini=(maxth-minth)*0.25;
meth=param.meth;
H0y=param.H0y;

%objective function computation for initial guesses for each method.
if meth<3
    S=rand('state');rand('state',0);
    E_old=genwfunf([theta 1-sum(theta)],opt,d,[],param,0)    
    rand('state',S);
elseif meth==3    
    w=param.w;
    kn=length(w);
    param.H1frc=theta(end);
    param.df=theta(2);
    S=rand('state');rand('state',0);
    E_old=genwfunf(w,opt,d,theta(1),param)
    rand('state',S);
end

%determination of an initial termperature (deactivated for the speed)
%nuphill=0;
%dgyini=0;
%while nuphill<50
%    tmx=pertb(theta,minth,maxth,stpini,p,meth);
%    if meth<3
%       E_tm=genwfunf([tmx 1-sum(tmx)],opt,d,[],param);
%    else
%       param.H1frc=tmx(2);
%       E_tm=genwfunf([tmx 1-sum(tmx)],opt,d,tmx(1),param);
%    end
%    if E_tm>E_old
%        nuphill=nuphill+1
%        dgyini=dgyini+E_tm-E_old
%    end
%end
%T=-(dgyini/nuphill)/log(0.5)		%initial temperature

if meth<3
    T=50;
else
    T=150;
end

%initialization of parameters
nfobj=1;
thetaopt=theta;
Elowst=E_old;
stpmst=stpini;
cri=[0 0 0 0];
% four termination conditions;
iter=0;
Tnmvdst=[];
while ~cri	
    iter=iter+1
    mvokst=0; %the number of accepted moves;
    nmvust=0; %accepted uphill moves;
    nmvst=0; %attempted moves- mtotst;
    avgyst=0; % sum of successive FOBJ values;
    sdgyup=0; % sum of accepted uphill FOBJ variations;
    nmvdst=0; %the number of accepted downhill moves;
    numsel=zeros(1,p);
    while mvokst<12*p & nmvst <100*p
        nmvst=nmvst+1;
        if meth<3
            stmx=2;
            while stmx>=1
                tmx=pertb(theta,minth,maxth,stpmst,p,meth);
                stmx=sum(tmx);
            end
        else
            tmx=pertb(theta,minth,maxth,stpmst,p,meth);
        end        
        %displaying random sampling.
        %plot(tmx(1),tmx(end),'g.');hold on;
        %axis([0 1 0 1]);
        %pause(0.75);
        if meth<3
            S=rand('state');rand('state',0);
            E_new=genwfunf([tmx 1-sum(tmx)],opt,d,[],param,0);
            rand('state',S);
        elseif meth==3     
            w=param.w;
            kn=length(w);
            param.H1frc=tmx(end);
            param.df=tmx(2);
            S=rand('state');rand('state',0);
            E_new=genwfunf(w,opt,d,tmx(1),param);
            rand('state',S);         
        end
        nfobj=nfobj+1;
        avgyst=avgyst+E_new;
        %Metropolis algorithm
        if E_new < E_old
            theta=tmx;
            E_old=E_new;
            Elowst=E_new
            thetaopt=theta
            mvokst=mvokst+1;
            nmvdst=nmvdst+1;
            %displaying intermediate fitting restults in methods 3 and 4.
            if meth==3
                w=param.w;
                kn=length(w);
                param.H1frc=tmx(end);
                param.df=tmx(2);
                S=rand('state');rand('state',0);
                E_new=genwfunf(w,opt,d,tmx(1),param,2);
                rand('state',S);
            end
        else
            prob=exp(-(E_new-E_old)/T); %criterion includes tau for noisy measurements
            test=rand;
            if test < prob
                theta=tmx;
                nmvust=nmvust+1;
                sdgyup=sdgyup+E_new-E_old;
                mvokst=mvokst+1;
            end
        end
    end
    %temp adjustment depending on the success rate.
    Tnmvst(iter)=nmvdst;
    avgyst=avgyst/nmvst;
    rftmp=max(min(Elowst/avgyst,0.9),0.1);    
    T=rftmp*T
    Tnmvst
    rok=mvokst/nmvst;
    %step size(~cooling rate) adjustment based on the success rate.
    if rok>0.2
        stpmst=stpmst*2;
    elseif rok<0.05
        stpmst=stpmst*0.5;
    end
    tm=find(stpmst>0.5*(maxth-minth));
    stpmst(tm)=0.5*(maxth(tm)-minth(tm));
    %checking four stopping criteria
    %if iter>4 & sum(Tnmvst(iter-3:iter))==0
    if iter>2 & sum(Tnmvst(iter-1:iter))==0
        cri(1)=1;
    end
    if T<0.001
        cri(2)=1;
    end
    cri3rd=find(stpmst<1.e-6*stpini+1.e-8);
    if ~isempty(cri3rd)
        cri(3)=1;
    end
    if nfobj>=5000*p
        cri(4)=1;
    end    
    if meth==3
        if Elowst<0.2
            cri(5)=1;
        end
    end    
    cri
end

function tmx=pertb(theta,minth,maxth,stpmst,p,meth)
%sampling within: for weights [minth=0,maxth=1] while for noncentrality
%parameters, [minth=0,maxth=30];
perturb=sign(randn(1,p)).*rand(1,p).*stpmst;
tm=find(theta+perturb<minth | theta+perturb>maxth);
perturb(tm)=-perturb(tm);%adjustment if random weights outside of the range
tmx=theta+perturb;