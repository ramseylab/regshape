This directory is the main directory for software
developed by the Bolouri Group at the Institute for Systems Biology.

The following subdirectories can be found in this
directory:

  apps:   Contains all of the applications in this
          source tree.  Generally does not contain
          any Java source code, just application build
          and config files, and other resources.  

          For more information, read the "apps/README.txt" file.

  java:   The java source tree.  All Java code is
          contained in this tree.  

          For more information, read the "java/README.txt" file.

  docs:   Documentation that is not specific to a particular
          application, is placed in this directory.

          For more information, read the "docs/README.txt" file.

  config: Configuration data that is not specific to a particular
          application, is placed in this directory.

          For more information, read the "config/README.txt" file.

  classes: Contains compiled java classes (created by build system)

  build:  Temporary directory created by build system, to hold 
          things like built Jar files, Web content, etc.

The build system used underneath this directory tree is "Ant"
(http://ant.apache.org).  Invoking "Ant" is just like invoking
the "make" program familiar from Unix operating systems, as shown
here:

  ant <target>

where "<target>" is the name of the target to be invoked. 

NOTE: All Ant invocations should be made in the top-level directory
for this project; attempting to invoke Ant from a lower-level
subdirectory will likely result in an error and a failed build.

Supported targets are:

  clean:          remove (most) build files (java class files are
                  not removed, but remain under the "classes" directory)

  distclean:      remove all temporary build files, including the
                  java class files

  build:          compile all java classes and perform other "build"
                  functions, resulting in a JAR file being placed
                  in the "build" subdirectory, along with a source
                  tarball

  buildWeb:       generate HTML documentation in a build directory

  uploadWeb:      upload the entire web content tree to the web server


In order to use the "uploadWeb" target, you will need to have 
the "ncftp" program (http://www.ncftpd.com) installed on your 
computer.  The program must also be found within your PATH
environment variable, so that the "Ant" program can find it.



-Stephen Ramsey

