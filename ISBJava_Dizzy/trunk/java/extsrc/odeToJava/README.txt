NOTE:  This is a modified version of the odeToJava software package.
It was modified by Stephen Ramsey on 2002/04/06.  For more information,
see the comments in the ErkTriple.java and ODERecorder.java files.

-----------------------------------------------------------------------

The odeToJava software package: a package of Ordinary Differential
Equation solvers written in Java that can be used to solve
initial-value problems for stiff and non-stiff ordinary differential
equations of the form:

	y'(t) = f(t,y(t)), y(t_0) = y_0, t_0 <= t <= t_f.

The package contains some standard explicit Runge-Kutta methods (for
non-stiff problems), but the user can specify an arbitrary explicit
Runge-Kutta method.  Linearly implicit implicit-explicit (IMEX)
Runge-Kutta schemes are used for stiff problems (see Ascher, Ruuth,
and Spiteri, 1997). In the case of explicit methods, error control can
be done with an embedded Runge-Kutta method when one is supplied.  In
the case of IMEX methods, or when an embedded scheme is not supplied
for an explicit method, then standard step-doubling (see Bulirsch and
Stoer, 1993) can be used.

Authors: Murray Patterson and Raymond J. Spiteri

Date: November 11, 2002 (alpha.2 version).

Copyright 2002 Murray Patterson and Raymond J. Spiteri: This entire
package was created by Murray Patterson and Raymond J. Spiteri and is
therefore the Copyright of Murray Patterson and Raymond J. Spiteri.

The authors wish to acknowledge Ehab Eshtaya for his efforts in
testing and benchmarking the software, and Jesse Rusak for his good
ideas and help with the code.  Both of these people were essential for
the quality and speed of production of this package.

Credit goes out to the creators of the Jama package used by several
routines as well as the creators of the GraphPaperLayout class used in
the plotting tool.  These were packages and pieces of code that were
found online and were very helpful in creating and completing this
package.

The authors wish to acknowledge the writers of the University of Bari
test set (formerly known as the CWI test set) and DEtest test set
(also known as the NSDTST and STDTST test sets of Enright &
Pryce). These two test sets were very useful in benchmarking many of
the solvers in this package.  Both test sets are mentioned a number of
times in the documentation of this package.  A link to the *.pdf file
of the University of Bari testset, release 2.1, can be found at:
http://hilbert.dm.uniba.it/~testset/

The authors also wish to acknowledge the support received from the
Natural Sciences and Engineering Research Council (NSERC) of Canada.

**********************************

Here is a basic description of how to use this package for solving
ODEs.  We assume that you are somewhat familiar with numerical solvers
for ODEs, and also that you know a little about Java and Java packages
as well as some command-line programming. 


1) Preliminaries:

In acquiring the main folder/directory of the package and all files
relevant to it, the contents should all be inside a folder called
ODE_solvers.  In fact, this readme file is inside the ODE_solvers
folder.  This ODE_solvers folder should consist of the following
folders: jar_files, plotting_files, source_code, templates, testers,
and of course this file, readme.txt.  Once you have made sure
everything is there, we can go on with further instructions.  Also,
the Jama package is used by the IMEX routines, but is not included
with this package.  If you wish to use the IMEX routines, you must
download Jama-1.0.1.jar, which can be found at:

http://math.nist.gov/javanumerics/jama/

This file must then be placed in the jar_files directory (alongside
odeToJava.jar).  Everything is set up to work with this file, so once
Jama-1.0.1.jar is placed in the jar_files directory, the IMEX routines
will work fine.


2) Initial setup:

Windows users:

When initially setting things up in Windows, you can put the folder
wherever you wish, as long as the batch files in the templates and
testers directories reflect the placing of the jar_files directory.
This will already be set up if the ODE_solvers directory and all
directories and files below it are kept together, so you shouldn't
have to modify anything for it to work on your computer.  You also
need jdk1.3 or higher to run these solvers (as this is what was used
in developing this package). You should also have it so that *.java
files can be run from any directory (i.e., in using a *.java file
called Test.java, you should be able to compile with:

	javac Test.java

and run with: 

	java Test  

instead of i.e., C:\Java\jdk1.3\lib\javac Test.java, etc.).  This can
be easily set up by defining some paths and classpaths in
autoexec.bat.  Or if you do not want to edit autoexec.bat, you can
edit the batch files (found in each directory of the tester directory)
to allow the batch file to find the Java compiler.  Most programming
environments set up the Java compiler so that it can run from any
directory anyway.  The testers directory will be the one you will use
for testing the routines out for yourself.

Linux users:

When initially setting things up in Linux, you can also put the folder
wherever you wish, as long as the makefiles in the testers directory
reflect the placing of the jar_files directory.  This will already be
set up if the ODE_solvers directory and all directories and files
below it are kept together, so you shouldn't have to modify anything
for it to work on your computer.  You should have jdk1.3 or higher to
run these, and should modify the classpaths wherever they need
modification so that *.java files can be run from any directory (like
with the Windows instructions) as well.  If you have an older version
of jdk on your machine or had an old version on your machine that
jdk1.3 did not completely mask, you can check if the path is set to
the new version or not by typing:

	java -version

if the version less than jdk1.3, you may have to set the path to
jdk1.3 by typing:

	export PATH=/usr/java/jdk1.3/bin:$PATH

then you can type:

	java -version

again to see if it changed or not (if not, you will have to install a
newer version of jdk on your machine).  So for example, we installed a
new version of jdk on one of our machines to run this problem so we
typed:

	export PATH=/usr/java/jdk1.3.1_01/bin:$PATH

as jdk1.3.1_01 was the specific version of jdk1.3 that we had.  Then
we typed:

	java -version

and it responded with the correct jdk1.3.1_01 version.  The testers
folder could also be used for Unix users (with a possibility of small
changes needed; this folder happened to be developed and tested in a
Linux environment).


3) Directory Structure of all the files of and for this package:

jar_files

This directory contains the file odeToJava.jar (and should contain
Jama-1.0.1.jar if you wish to use the IMEX routines).  odeToJava.jar
is a *.jar file that we have created that contains all of the files
that are in the odeToJava package.  Both of these *.jar files are all
that you need to run code to solve ODEs.

plotting_files

This directory contains a number of *.txt files.  These files are
essentially data files that contain solutions to pre-solved ODEs that
can be used with the plotter.

source_code

This directory contains the source code for the odeToJava package.
Feel free to take a look at the source code for the solvers and
modules that go with it as you become more familiar with using this
package.  Note that this folder contains the source code that was used
to generate the odeToJava.jar file that is in the jar_files directory.

templates

This directory contains directories that are much the same as those in
the testers directory.  Each directory contains 2 batch files and a
Makefile as each directory of the testers directory does.  Each
directory of the templates directory also contains 2 *.java files, a
template ODE function file and a template driver file (set up to solve
the template function).  The idea here is that the you can easily edit
the 2 *.java files to solve whatever problem you wish.  The templates
directory also contains a master makefile that calls the compile and
clean tags (separately) of all the makefiles in the directories below
it.  It is recommended that you look at the testers directory to see
how the functions and drivers are implemented before you start making
your own.

testers

There are several directories in the testers directory.  Each of these
folders contains an example problem.  To test each of the solvers on
these problems, there are one or more *.java files that solve the
problem using a given solver, a batch file that compiles and runs
these *.java files for Windows users, a batch file that cleans up all
class and text files for Windows users, and a Makefile that compiles
and runs this *.java file for Linux users.  If you are a Windows user
who followed the instructions in Section 2 of this readme, all you
have to do is double click on the batch file, and run the test (the
*.java file that is in the directory alongside the batch file is run).
In running this batch file, class files and text files will be created
(text files being solutions for the plotter).  Then to clean up all
class and text files, all you have to do is double click on Clean.bat.
As for Linux users, the Makefiles will also create class and text
files as the batch file would in Windows.  To compile and run with the
Makefile, simply type:

	make run

If you only wish to compile, type:

	make compile

You can also clean up all class and text files by typing:

	make clean

We recommend looking at ErkTripleTest.java in the erkTripleTest
directory as a good place to start learning about the testers for this
package.  This testers folder also contains a master makefile that
calls the compile and clean tags (separately) of all the makefiles in
the directories below it.  The idea here is that the user can compile
all of the programs in every folder in the testers directory by
typing:

	make compile

and clean all the folders in the testers directory by typing:

	make clean

This does the same thing that the master makefile in the templates
directory does.  After trying all the testers, you should have a good
feel for what the odeToJava package has to offer, and you can start
writing your own routines.


4) The Walkthrough:

Probably the most important step in understanding the usage of the
odeToJava package is to read the documentation in the file called
ErkTripleTest.java.  This file can be found in testers/erkTripleTest.
You can open this *.java file in any editor (text editor or program
editor that supports *.java files).  After reading through
ErkTripleTest.java you will realize what the other 2 *.java files are
for, and you should read those as well.  These three *.java files of
heavily commented code should give you a good understanding of how to
import the Java package components, write ODE functions using
pre-defined ones included in this Java package, use the modules of
this package, call the routines that solve ODEs, and plot the
solutions to these ODEs.  Looking at the code and documentation of
ErkTripleTest.java is a virtual necessity in knowing how to use this
package.


5) Conclusions:

This should cover the essential descriptions of all of the files and
ways around this package.  We hope it is adequate for you to learn how
to use this package.  We also hope you find this package useful in
solving ODEs in Java, as we believe it is the first publicly available
Java package for the numerical solutions of ODEs.  If you have any
questions or comments about this package, please e-mail Murray
Patterson (039320p@acadiau.ca) or Raymond J. Spiteri
(spiteri@cs.dal.ca).  This is still very much a work in
progress. However, we feel that the package in its current state
should be useful to people who want to solve ODEs numerically in
Java. We are planning to extend the options and the class of problems
handled by the package in the near future, but until then . . . enjoy!
