%-------------------------------------------------------------------------------
% Copyright (C) 2005 by Institute for Systems Biology,
% Seattle, Washington, USA.  All rights reserved.
% 
% This source code is distributed under the GNU Lesser 
% General Public License, the text of which is available at:
%   http://www.gnu.org/copyleft/lesser.html
%
%-------------------------------------------------------------------------------
% Module:    supsmu.m
%
% Author:    Daehee Hwang 
%            Institute for Systems Biology
%
% Function:  a non-parametric smoothing algorithm
%-------------------------------------------------------------------------------
function smo = supsmu(x,y)

% This supsmu (super-smoothing) estimates a non-parametric relationship
% between a predictor variable (x) and a response variable (y): refer to
% the paper regarding Super-Smoothing (J. Friedman,
% Department of Statistics, Stanford University).
%
% The input arguments that should be carefully determined depending on given data structure include:
% 1) a weight vector (w) which assign relative importance for all x values
% during smoothing. The default values are all ones, meaning that all x
% values are equally important.
% 2) a window size (span) for smoothing. If span is 0, the program assigns
% an optimal window size for the given data structure by selecting the best
% one among 0.05,0.2, and 0.5 (the upper limit in any smoothing).
% 3) another smoothing parameter (alpha) which can be used to control for
% the sensitivity of smoothing other than "span". This alpha should be
% between 0 and 10: but, use one of 2, 5, and 8. The default value is 2
% because we want to ensure a bit sensitivity while we loose the
% sensitivity by smoothing the data.

w=ones(size(x));
span=0.001;
alpha=0;

iper=1;
n=length(x);
spans=[0.05,0.2,0.5];
big=1.0e20;
sml=1.0e-7;
eps=1.0e-3;
sc=[];
if x(n)<=x(1)
    sy=sum(w.*y);
    sw=sum(w);
    if sw>0 
        a=sy/sw;
    end;
    smo=a;
else
    i=round(n/4);
    j=round(3*i);
    scale=x(j)-x(i);
    while scale<=0
        if j<n, j=j+1;end
        if i>1, i=i-1;end;
        scale=x(j)-x(i);
    end;
    vsmlsq=(eps*scale)^2;
    jper=iper;
    if iper==2 & (x(1)<0 | x(n)>1)
        jper=1;
    end;
    if jper<1 | jper>2
        jper=1;
    end;
    if span>0
        [smo,sc]=smooth(n,x,y,w,span,jper,vsmlsq);
    else
        for i=1:3
            [sc(:,2*i-1),sc(:,7)]=smooth(n,x,y,w,spans(i),jper,vsmlsq);
            [sc(:,2*i),h]=smooth(n,x,sc(:,7),w,spans(2),-jper,vsmlsq);
        end;
        for j=1:n
            resmin=big;
            for i=1:3
                if sc(j,2*i)<resmin
                    resmin=sc(j,2*i);
                    sc(j,7)=spans(i);
                end;
            end;
            if alpha>0 & alpha<=10 & resmin<sc(j,6) & resmin > 0
                sc(j,7)=sc(j,7)+(spans(3)-sc(j,7))*max(sml,resmin/sc(j,6))^(10.0-alpha);
            end;
        end;
        [sc(:,2),h]= smooth(n,x,sc(:,7),w,spans(2),-jper,vsmlsq);
        for j=1:n
            if (sc(j,2)<=spans(1)) 
                sc(j,2)=spans(1);end;
            if (sc(j,2)>=spans(3)) 
                sc(j,2)=spans(3);end;
            f=sc(j,2)-spans(2);
            if f<0
                f=-f/(spans(2)-spans(1));
                sc(j,4)=(1.0-f)*sc(j,3)+f*sc(j,1);
            else
                f=f/(spans(3)-spans(2));
                sc(j,4)=(1.0-f)*sc(j,3)+f*sc(j,5);
            end;
        end;
        [smo,h]=smooth(n,x,sc(:,4),w,spans(1),-jper,vsmlsq);
    end;
end;

%-------------------------------------------------------------------------
function [smo,acvr]= smooth(n,x,y,w,span,iper,vsmlsq)

smo=zeros(n,1);
acvr=smo;

xm=0;
ym=xm;
var=ym;
cvar=var;
fbw=cvar;
jper=abs(iper);
ibw=floor(0.5*span*n+0.5);
if ibw<2
    ibw=2;
end;
it=2*ibw+1;
for i=1:it
    j=i;
    if jper==2
        j=i-ibw-1;
    end;
    xti=x(j);
    if j<1
        j=n+j;
        xti=x(j)-1.0;
    else
        wt=w(j);
        fbo=fbw;
        fbw=fbw+wt;
        if (fbw>0) 
            xm=(fbo*xm+wt*xti)/fbw;
            ym=(fbo*ym+wt*y(j))/fbw;
        end;
        tmp=0;
        if fbo>0
            tmp=fbw*wt*(xti-xm)/fbo;
        end;
        var=var+tmp*(xti-xm);
        cvar=cvar+tmp*(y(j)-ym);
    end;
end;

for j=1:n
    out=j-ibw-1;
    in=j+ibw;
    if (jper==2)|(out>=1 & in<=n)
        if out<1
            out=n+out;
            xto=x(out)-1.0;
            xti=x(in);
        else
            if in>n
                in=in-n;
                xti=x(in)+1.0;
                xto=x(out);
            else
                xto=x(out);
                xti=x(in);
            end;
        end;
        wt=w(out);
        fbo=fbw;
        fbw=fbw-wt;
        tmp=0.0;
        if (fbw>0)
            tmp=fbo*wt*(xto-xm)/fbw;
        end;
        var=var-tmp*(xto-xm);
        cvar=cvar-tmp*(y(out)-ym);
        if (fbw>0) 
            xm=(fbo*xm-wt*xto)/fbw;
            ym=(fbo*ym-wt*y(out))/fbw;
        end;
        wt=w(in);
        fbo=fbw;
        fbw=fbw+wt;
        if (fbw>0) 
            xm=(fbo*xm+wt*xti)/fbw;
            ym=(fbo*ym+wt*y(in))/fbw;
        end;
        tmp=0;
        if (fbo>0) 
            tmp=fbw*wt*(xti-xm)/fbo;
        end;
        var=var+tmp*(xti-xm);
        cvar=cvar+tmp*(y(in)-ym);
    end;
    a=0;
    if var>vsmlsq
        a=cvar/var;
    end;
    smo(j)=a*(x(j)-xm)+ym;
    if iper>0
        h=0;
        if fbw>0 
            h=1.0/fbw;
        end;
        if var>vsmlsq
            h=h+(x(j)-xm)^2/var;
        end;
        a=1.0-w(j)*h;
        if a>0
            acvr(j)=abs(y(j)-smo(j))/a;
        else
            if j>1
                acvr(j)=acvr(j-1);
            end;
        end;
    end;
end;

j=1;
while j<=n
j0=j;
sy=smo(j)*w(j);
fbw=w(j);
    while j<n
        if x(j+1)<=x(j)
            j=j+1;
            sy=sy+w(j)*smo(j);
            fbw=fbw+w(j);
        else
            break;
        end
    end;    
    if j>j0
        a=0.0;
        if fbw>0 
            a=sy/fbw;
        end;
        for i=j0:j
            smo(i)=a;
        end
    end;
    j=j+1;
end;
