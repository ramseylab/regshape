# This file is part of the regshape software package.  regshape is free
# software: you can redistribute it and/or modify it under the terms of the GNU
# General Public License as published by the Free Software Foundation, version
# 2 (and incorporated into this package distribution in the file LICENSE).
#
# This program is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
# details.
#
# You should have received a copy of the GNU General Public License along with
# this program; if not, write to the Free Software Foundation, Inc., 51
# Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Copyright Jichen Yang and Stephen Ramsey, Oregon State University
# 2014.12.16

#' get DNA shape parameter values for a provided DNA sequence string
#' get DNA parameters: MGW, Prot, Roll, and Helt, the length of sequence should >=6
#' @param seq.string A DNA sequence, or a set of DNA sequences
#' @return the parameter values for MGW, Prot, Roll, and Helt, for each hexamer subsequence
#' of seq.string, all concatenated together as one vector (MGW parameter values first, then
#' Propeller twist parameter values, then roll values, and then helix twist parameter values)
#' @examples
#' getDNAShapeParamValues("ACGTAGATAG")
#' @export
getDNAShapeParamValues<-function(seq.string){
  l<-nchar(seq.string)
  MGW<-c();Prot<-c();
  Roll<-c();Helt<-c();
  if(l<5) return(NA)
  for(i in 1:(l-5+1)){
    temp<-substr(seq.string,start=i,stop=i+4)
    MGW<-c(MGW,FivemerTable[temp,]$MGW)
    Prot<-c(Prot, FivemerTable[temp,]$Prot)
    Roll<-c(Roll, FivemerTable[temp,]$Roll.1, FivemerTable[temp,]$Roll.2)
    Helt<-c(Helt, FivemerTable[temp,]$Helt.1, FivemerTable[temp,]$Helt.2)
  }
  Roll2<-Roll[1];Helt2<-Helt[1];
  for(i in 1:(length(Roll)-1)){
    if(i%%2==0){
      Roll2<-c(Roll2,mean(c(Roll[i],Roll[i+1])))
      Helt2<-c(Helt2,mean(c(Helt[i],Helt[i+1])))
    }
  }
  Roll2<-c(Roll2,Roll[length(Roll)])
  Helt2<-c(Helt2,Helt[length(Helt)])
  return(c(Helt2,MGW,Roll2,Prot))
}

getFeatures<-function(vector){
  minValue<-min(vector)
  maxValue<-max(vector)
  lengthVector<-length(vector)
  ind<-which(vector==minValue)[1]
  if(ind==1){
    while(ind<lengthVector){
      if(vector[ind+1]>vector[ind]) {ind<-ind+1}
      else{break}
    }
    peakWidth<-(ind-1)
    height<-vector[ind]-vector[1]
    }else if(ind==lengthVector){
      while(ind>1){
        if(vector[ind-1]>vector[ind]) {ind<-ind-1}
        else{break}
      }
      peakWidth<-(lengthVector-ind)
      height<-vector[ind]-vector[lengthVector]
      }else{
        left<-ind;right<-ind
        while(right<lengthVector){
          if(vector[right+1]>vector[right]) {right<-right+1}
          else{break}
        }
        while(left>1){
          if(vector[left-1]>vector[left]) {left<-left-1}
          else{break}
        }
        peakWidth<-(right-left)
        height<-mean(c(vector[right]-minValue,vector[left]-minValue))
      }
  aveValue<-mean(vector,na.rm=T)
  return<-c(lengthVector, peakWidth, height, aveValue, minValue, maxValue)
}


#' get DNA shape score for a set of sequences
#'
#' @param x A vector of character strings, each of length L characters, composed of sequences of
#' "A", "C", "G", or "T" characters
#' @return a vector of voting fraction scores in the range [0-1], for the DNA shape-based regulatory element classifier
#' @examples
#' getShapeScores(c("ACGTACGT","ACGTACTG","ACTTACGT","GCGTGACT"))
#' @export
#' @import randomForest
getShapeScores<-function(x){
  d<-data.frame(x)
  DNApars<-t(apply(d,1,getDNAShapeParamValues))
  l<-nchar(x[1])
  Helt<-DNApars[,1:(l-3),drop=FALSE]
  MGW<-DNApars[,(ncol(Helt)+1):(ncol(Helt)+(l-4)),drop=FALSE]
  Roll<-DNApars[,(ncol(Helt)+ncol(MGW)+1):(ncol(Helt)+ncol(MGW)+(l-3)),drop=FALSE]
  Prot<-DNApars[,(ncol(Helt)+ncol(MGW)+ncol(Roll)+1):(ncol(Helt)+ncol(MGW)+ncol(Roll)+(l-4)),drop=FALSE]
  HelTfeature<-t(apply(Helt,1,getFeatures))
  MGWfeature<-t(apply(MGW,1,getFeatures))
  ProTfeature<-t(apply(Roll,1,getFeatures))
  Rollfeature<-t(apply(Prot,1,getFeatures))
  featureTable<-cbind(HelTfeature,MGWfeature,ProTfeature,Rollfeature)
  head(featureTable)
  featureTable<-data.frame(featureTable)
  featureTable<-featureTable[,c(-1,-7,-13,-19)]
  DNAscore<-predict(RFmodel, newdata = featureTable, type = "prob")[,1]
  DNAscore<-as.vector(DNAscore)
  return(DNAscore)
}

#' get DNA shape score for a set of sequences
#'
#' @param seq.chars A character string composed of "A", "C", "G", or "T" characters
#' @param wlen A positive integer indicating the window size (in bp) for computing the DNA shape
#' based voting fraction score for the regulatory element classifier
#' @return a vector of voting fraction scores in the range [0-1], for the DNA shape-based regulatory element classifier,
#' at each position of the sliding window (length of the vector is nchar(seq.chars) - wlen + 1).
#' @examples
#' getShapeScoresSlidingWindow("ACGATATGAACTAGACTAGTAGAGTAGAGC", 8)
#' @export
getShapeScoresSlidingWindow <- function(seq.chars, wlen) {
    slen <- nchar(seq.chars)
    1:(slen - wlen + 1)
    seq.charvec <- strsplit(seq.chars, "")[[1]]
    window.seq <- apply(matrix(seq.charvec[t(sapply(1:(slen-wlen+1),
                                                    function(pos) { 0:(wlen-1) + pos }))], ncol=wlen),
                        1, function(crow) {paste(crow, collapse="")})
    getShapeScores(window.seq)
}

generatePred<-function(x){
	temp<-x%/%20 +1
	RFmodel<-load(paste("RFmodel",temp,".RData",sep=""))
	return(RFmodel)
}

# this is auto-run when the package loads (see zzz.R)
Initialize<-function(){
  print("setting random number seed")
  set.seed(20140930)
}

