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

IMPORTANT:  Using the build system requires special software
to be installed on your computer.

Supported targets are:

  clean:          remove (most) build files (java class files are
                  not removed, but remain under the "classes" directory)

  distclean:      remove all temporary build files, including the
                  java class files

  build:          compile all java classes and perform other "build"
                  functions, resulting in a JAR file being placed
                  in the "build" subdirectory, along with a source
                  tarball

  buildWeb:       Build all web content, including HTML documentation,
                  PDF manuals, and InstallAnywhere web installers,
                  into the "build/web" subdirectory.
                  (NOTE:  this requires quite a bit of software to
                  be carefully installed on the build machine; make sure
                  you read the WebPagesManagementManual.html document,
                  which is contained in the "docs/private" subdirectory.)

  buildHTML:      Build all HTMl web content, including Javadoc API
                  documentation, into the "build/web" subdirectory
                  PDF documentation and InstallAnywhere installers are
                  not generated.  You must have first run the "build"
                  task.

  buildPDF:       Build the PDF documentation (you must have first
                  run the "buildHTML" task), into "build/web" 
                  subdirectory.  (NOTE: This requires special software 
                  to be installed on your computer; please read the
                  WebPagesManagementManual.html document,
                  which is contained in the "docs/private" subdirectory.)

  buildInstallers:  Build the InstallAnywhere installers and store
                    them under the "build/web" subdirectory.  This
                    requires that the InstallAnywhere program be
                    installed on your computer.

  uploadWeb:      upload the entire web content tree to the web server


In order to use the "uploadWeb" target, you will need to have 
a version of "Ant" installed that understands the "<ftp>" target.
This means that your version of "Ant" should have been built
with the external "NetComponents.jar" library in the CLASSPATH
(see the WebPagesManagementManual.html document for more info).

-Stephen Ramsey

