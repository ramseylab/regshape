SOURCE_ROOT = src
INSTALL_DOCS = ${HOME}/public_html/javadoc

JAVAC_BIN = /tools/java/sdk/bin/javac
JAVAC_FLAGS = -source 1.4
JAVAC = ${JAVAC_BIN} ${JAVAC_FLAGS}

JAVADOC_BIN = /tools/java/sdk/bin/javadoc
JAVA_PACKAGES = isb.chem \
                isb.util \
                isb.chem.sbw \
                isb.chem.scripting \
	 	isb.chem.app

JAVADOC_FLAGS = -source 1.4 -overview src/overview.html -sourcepath ${SOURCE_ROOT} 
JAVADOC = ${JAVADOC_BIN} ${JAVADOC_FLAGS}

JAR_FILE = isb.jar

all: docs compile

install-web:
	${JAVADOC} -d ${INSTALL_DOCS} ${JAVA_PACKAGES}
	cp apps/Dizzy/docs/*.html ${HOME}/public_html/Dizzy
	cp docs/LICENSE.txt ${HOME}/public_html
	cp apps/Dizzy/docs/*.png ${HOME}/public_html/Dizzy

compile:
	${JAVAC} `find . -name \*.java`

clean:
	find ${SOURCE_ROOT} -name \*.class -exec rm {} \;
	rm -f ${JAR_FILE}
	rm -r -f build
	rm -r -f apps/Dizzy/build/projectlocales

jar: 
	rm -r -f build
	mkdir -p build
	cd ${SOURCE_ROOT} && tar cvf classes.tar `find . -name \*.class`
	tar xvf ${SOURCE_ROOT}/classes.tar -C build
	rm -f ${SOURCE_ROOT}/classes.tar
	jar cvf ${JAR_FILE} -C build isb

srctar:
	cd ${SOURCE_ROOT} && tar cvzf ../build/isb-src.tar.gz `find . -name \*.java`
