%
% Copyright (C) 2004 by Institute for Systems Biology,
% Seattle, Washington, USA.  All rights reserved.
% 
% This source code is distributed under the GNU Lesser 
% General Public License, the text of which is available at:
%   http://www.gnu.org/copyleft/lesser.html
%

%----------------------------------------
% Module:    pointillist.m
%
% Author:    Daehee Hwang 
%            Institute for Systems Biology
%
% Function:  Infer the set of affected network elements,
%            from a collection of observations for different
%            evidence types.
%
%----------------------------------------

%
% notes:
% (1) Throughout, I use the term "gene" to refer to 
%     network elements, which may include genes, proteins,
%     and metabolites.
%

function [stat,out]=pointillist(x,cutoff,tmp)
[m,n]=size(x);

[m,n]=size(x);

% This is the "greedy" step where we
% take all genes whose significances (for at least one
% evidence type) exceeds the cutoff
G=Gselect(x,cutoff);

y=x;
if nargin>3
    while size(y,1)<11*length(G)
        tx=[];
        for j=1:n
            tx(:,j)=shuffle(tmp(:,j));
        end
        y=[y; tx];
    end
end
x=y;
P=y;

iter=0;
TG=[];
tstat=0;
while tstat<0.99
% an iteration of the pointillist algorithm starts here
    iter;

% calculate the evidence-type-specific weights and bias factors,
% for the set of affected genes "G"
    [w(iter+1,:),b(iter+1,:)]=weval(P(G,:),P);


    [stat(iter+1,:),G,C,Gy,Cy,rGp,eGp]=cepup(P,G,w(iter+1,:),b(iter+1,:));
    R=[[G;C] [Gy;Cy]];
    R=sortrows(R,1);
    G=[G(find(Gy<rGp)); C(find(Cy<=eGp))];
    Gy=[Gy(find(Gy<rGp)); Cy(find(Cy<=eGp))];
    [Gy,ii]=sort(Gy);G=G(ii);
    stat    
    tstat=stat(iter+1,end-1);

% increment the iteration counter
    iter=iter+1;
end
tn=max(R(G,2));
out.w=w(end,:);out.b=b(end,:);out.G=G;out.R=R;out.cutoff=tn;out.P=P;


%
%
%
function [stat,G,C,Gy,Cy,rGp,eGp]=cepup(x,G,w,b,oldw,oldb)

% "n" is the number of genes
n=size(x,1);

% "C" is the set of putatively unaffected genes
C=setdiff([1:n]',G);

% obtain the conditional probability for putatively affected genes
Gy=cpestim(x(G,:),w,b,nanmean(x));

% obtain the conditinoal probability for putatively unaffected genes
Cy=cpestim(x(C,:),w,b,nanmean(x));
[Gy,ii]=sort(Gy);G=G(ii);
[Cy,ii]=sort(Cy);C=C(ii);

% define the foliation of significance values for
% calculating the distribution of significances
xo=[0:0.001:1];

% calculate the cumulative distribution of
% significances for estimated conditional probabilities
% for affected genes
txx=[0;cumfrc(Gy,xo)];
txx=cumsum(txx)/sum(txx);

% calculate the cumulative distribution of
% significances for estimated conditional probabilities
% for unaffected genes
tyy=[0;cumfrc(Cy,xo)];
tyy=cumsum(tyy)/sum(tyy);

% obtain the values ("stat4") and the indices ("cr") of
% maximum values of the vector "txx - tyy"
[stat4,cr]=max(txx-tyy);

cr=xo(cr);
rGp=max(0.1,Gy(round(length(G)*(1-0.05))));
eGp=min(0.05,Cy(round(length(G)*0.05)));   
thre=[cr Gy(round(length(G)*(1-0.05))) Cy(round(length(G)*0.05)) rGp eGp];
stat=[length(G) w stat4 rGp];

%========================================
function pvalue=kernp(ratio,x,dest)
n=length(ratio);
pvalue=ones(n,1);
for i=1:n
    if isnan(ratio(i))==0
        [tmi,tmj]=min(abs(x-ratio(i)));
        if length(x(1:tmj))==1
            sig=0;
        else
            sig=trapz(x(1:tmj),dest(1:tmj));
        end
        pvalue(i)=1-sig;
    end
end;

%========================================
% weval:
%
% Calculates the weights for the
% different types of evidences, and
% the corresponding bias factors.
%
% w - a vector of the weights of different
%         evidence types
% b - a vector of the biases of different
%         evidence types
%
% x - I think this is an array of significance
%     values, where the first dimension (rows)
%     is the set of genes, and the second dimension
%     (columns) is the set of evidence types.
%
% rndp - I think this is also an array of significance
%     values, but for a different set of genes?
function [w,b]=weval(x,rndp)

% obtain the number "n" of different evidence types
[m,n]=size(x);
tp=ones(m,1);

% create vector of all numbers between 0 and 1, inclusive,
% with a spacing of 0.001 (this vector is length 1001).
xo=[0:0.001:1];

% for each evidence type "i"
for i=1:n

    % for a given evidence type "i", find the number of genes 
    % whose significance for this evidence type lies within
    % each bin in the partition of unity "xo"
    tk=[0;cumfrc(x(:,i),xo)];

    % normalize the vector "tk" and compute the cumulative
    % sum, to obtain a cumulative distribution function
    cums(:,i)=cumsum(tk)/sum(tk);

    % do the same thing, for the set of significances "rndp"
    tk=[0;cumfrc(rndp(:,i),xo)];
    rndcum(:,i)=cumsum(tk)/sum(tk);

    % for each type of evidence, compute the
    % weight factor w(i) as the maximum of the
    % difference of the two distribution functions computed above
    w(i)=max(cums(:,i)-rndcum(:,i));
end

% the bias factor is just one minus the weight factor,
% for each type of evidence (this is a vector operation)
b=1-w;

% normalize the weights by dividing each
% weight by the sum of all weigts
w=w/sum(w);

% normalize the biases by dividing each
% bias by the sum of all biases
b=b/sum(b);

% ------ commented out, not sure what this code does ------
%figure;
%bar([0;cumfrc(log(x(:,1)),xo)],'g');hold on;
%bar([0;cumfrc(log(rndp(:,1)),xo)],'m');
%figure;
%xo=[0 mean([xo(1:end-1);xo(2:end)])];
%plot(xo,cums(:,1:5));hold on;
%plot(xo,rndcum(:,1:5),'--');
% ------ commented out, not sure what this code does ------

%========================================
% cpestim:
%
% (estimate the conditional probabilities)
%
% for each significance value "s" in "x", compute
% b + s*w, where "w" is the weight, and "b" is the bias.
% Divide by "b + m*w", where "m" is the mean significane
% value for the particular evidence type
%
% returns the joint conditional probability over all
% evidences, for each gene
%
function y=cpestim(x,w,b,m)
n=size(x,2);
for i=1:n
    x(find(isnan(x(:,i))==1),i)=1;
end
for i=1:n
     x(:,i)=(b(i)+x(:,i)*w(i))/(b(i)+m(i)*w(i));
end
y=prod(x,2);

%========================================
% cumfrc:
%
% Given an array of values "z" and a vector
% "xo" that is like a measure, returns an array "no"
% of the same length as "xo"; at index "i" in this return
% array, the value is the number of elements of "z" that
% lie between "xo(i)" and "xo(i+1)".
function no=cumfrc(z,xo)
no=zeros(length(xo)-1,1);
for i=1:length(xo)-1
    if i~=length(xo)-1
        tn=find(z>=xo(i) & z<xo(i+1));
    else
        tn=find(z>=xo(i) & z<=xo(i+1));
    end
    if ~isempty(tn)
        no(i)=length(tn);
    end
end;

%========================================
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

%========================================
% Gselect:
%
% Given a significance cutoff and a set
% of significance data "x", return the set
% of genes for which the significance for
% at least one evidence type, is less than
% the cutoff value
function G=Gselect(x,cutoff);
G=[];
for i=1:size(x,2)
    G=[G;find(x(:,i)<cutoff)];
end
G=unique(G);
