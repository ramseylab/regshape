%
% Copyright (C) 2004 by Institute for Systems Biology,
% Seattle, Washington, USA.  All rights reserved.
% 
% This source code is distributed under the GNU Lesser 
% General Public License, the text of which is available at:
%   http://www.gnu.org/copyleft/lesser.html
%

%----------------------------------------
% Module:    qnorm.m
%
% Author:    Daehee Hwang 
%            Institute for Systems Biology
%
% Function:  Quantile normalization of data.
% The output is written to a file "_norm.txt",
% where the prefix is the supplied filename
% (minus the ".txt" of the supplied filename).
% For example, if the supplied filename is
% "foo.txt", the output file is "foo_norm.txt".
%
% Arguments: filename - the name of the data
% file containing the observation values to 
% be normalized.  The data is in ASCII 
% tab-delimited format.  The first row 
% contains column header names.  The first
% column contains ORF names or gene names
%
%            ms - an optional parameter 
% containing the special string that indicates
% a missing value
%----------------------------------------

function qnorm(filename, ms);
%input argument
%filename & ms: a flag for missing values (e.g., -100)

%Import an ascii tab delimited data
%a.data: a number array & a.textdata: a text cell (array) with blanks for
%the cells where the numbers exist.
a=importdata(filename);

x=a.data;%x: a number array;

%with no missing value flag, assign an empty vector for ms.
if nargin<2
    ms=[];
end
%if there are negative values in the data, add a value to make all the
%values positive to take the log2.
if length(find(x<1))>0
    adv=abs(min(min(x)));
    x=x+adv+1;
    if ~isempty(ms)
        ms=ms+adv+1;
    end
end
x=log2(x);

%extract time point information from the first row (column headers).
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
else
    [jj,ii,cl]=unique(tab,'rows');
    uc=unique(cl);
    ut=[];
end

%depending on whether the data has missing values (i.e., ms is not an empty
%vector) and are time-course data, different normalizations are applied.
if length(ut)<2
    %uni-time point data normalization
    if isempty(ms)
        x=normz(x);
    else
        ms=log2(ms);
        if length(uc)>1
            indo=find(x==ms);
        end        
        ii=sum((x==ms),2);
        ind=find(ii~=size(x,2));
        x(ind,:)=normzmissing(x(ind,:),ms);
        if length(uc)>1
            x(indo)=ms;
            for i=1:length(uc)
                tn=find(cl==uc(i));
                xx=x(:,tn);
                ii=sum((xx==ms),2);
                ind=find(ii~=size(xx,2));
                x(ind,tn)=normzmissing(xx(ind,:),ms);
            end
        end
        x(indo)=NaN;
    end
else
    %normalization for the data with multiple time points.
    if isempty(ms)
        for i=1:length(ut)
            tn=find(t==ut(i));
            x(:,tn)=normz(x(:,tn));
        end
    else
        ms=log2(ms);
        indo=find(x==ms);
        x=normtc(x,t,ms);
        x(indo)=NaN;
    end
end

%combining texts with the numbers obtained from normalization tools. Also,
%rename the filename into filename_norm.txt
filerd(a.textdata,x,filename);
%writing a tab-delimited file for the data with texts and numbers. 
%filewt(y,filerename);

function x=normz(x)
%Quantile normalization method makes all the quantiles (distributions) of the microarrays
%(columns in the data) the same.
m=size(x,2);
ind=[];y=[];
for i=1:m
    [y(:,i),ind(:,i)]=sort(x(:,i));
end
ymean=mean(y,2);
for i=1:m
    x(ind(:,i),i)=ymean;
end

function x=normzmissing(x,ms)
%quantile normalization for the data with missing values comprise the
%following three steps:
%1. estimate the missing values as the medians of measured values
%2. apply quantile normalization method to the data with estimated missing
%values and measured data.
%3. repeat steps 1 and 2 until the estimated missing values do not vary.
[m,n]=size(x);
orix=x;
err=1.e6;olderr=1.e7;
iter=0;
while abs(err-olderr)/err>0.01 & err>0.1
    %iter=iter+1
    olderr=err;  
    index=(orix==ms);
    ii=find(index);    
    x(ii)=NaN*zeros(size(ii));
    medx=nanmedian(x')';
    x(ii)=zeros(size(ii));
    tn=medx*ones(1,n);
    x=x+tn.*index;
    oldx=x;    
    ind=[];y=[];
    for i=1:n
        [y(:,i),ind(:,i)]=sort(x(:,i));
    end
    ymean=mean(y,2);
    for i=1:n
        x(ind(:,i),i)=ymean;
    end
    err=sum(sum((oldx-x).^2));
    %[err abs(err-olderr)/err]
end

function x=normtc(x,t,ms)
% this function works similarly to the quantile normalization method
% for the data with missing values. But, apply the method for the data at
% each time point (see for loop within while loop). 
[m,n]=size(x);
ii=sum((x==ms),2);
ind=find(ii~=size(x,2));
indt0=find(t==min(t));
indt=find(t~=min(t));
x(ind,indt0)=subnorm(x(ind,indt0),t(indt0),ms);
x(ind,:)=subnorm(x(ind,[indt0 indt]),t,ms);

function x=subnorm(x,t,ms)
[m,n]=size(x);
orix=x;
ut=unique(t);
indd=[1:n];
err=1.e6;olderr=1.e7;
iter=0;

while abs(err-olderr)/err>0.01 & err>0.1
    iter=iter+1;
    medx=[];
    olderr=err;  
    for i=1:length(ut)
        jj=find(t==ut(i));
        xx=x(:,indd(jj));
        index=(orix(:,indd(jj))==ms);
        ii=find(index);    
        xx(ii)=NaN*zeros(size(ii));
        tg=nanmedian(xx')';
        if i==1
            tg(find(isnan(tg)))=ms;
        else
            tg(find(isnan(tg)))=medx(find(isnan(tg)),i-1);
        end
        medx(:,i)=tg;
        xx(ii)=zeros(size(ii));
        tn=tg*ones(1,length(jj));
        xx=xx+tn.*index;
        x(:,indd(jj))=xx;
    end    
    oldx=x;
    
    ind=[];y=[];
    for i=1:n
        [y(:,i),ind(:,i)]=sort(x(:,i));
    end
    ymean=mean(y,2);
    for i=1:n
        x(ind(:,i),i)=ymean;
    end
    err=sum(sum((oldx-x).^2));
    %[err abs(err-olderr)/err]
    if iter>50
        break;
    end
end

function filerd(textdata,x,filename)
tn=find(filename=='.');
if ~isempty(tn)
    filerename=[filename(1:tn-1) '_norm.txt'];
else
    filerename=[filename '_norm.txt'];
end
fid=fopen(filerename,'w');

tabc=char(textdata(2:end,1));
y=[' ' sprintf('\t')];
[n,m]=size(textdata);
for i=2:m
    if i<m
        y=[y deblank(char(textdata(1,i))) sprintf('\t')];
    else
        y=[y deblank(char(textdata(1,i)))];
    end
end
fprintf(fid,'%s\n',y);
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