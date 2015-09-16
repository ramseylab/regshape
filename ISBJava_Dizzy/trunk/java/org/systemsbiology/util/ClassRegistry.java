package org.systemsbiology.util;
/*
 * Copyright (C) 2003 by Institute for Systems Biology,
 * Seattle, Washington, USA.  All rights reserved.
 * 
 * This source code is distributed under the GNU Lesser 
 * General Public License, the text of which is available at:
 *   http://www.gnu.org/copyleft/lesser.html
 */

import java.util.*;
import java.util.jar.*;
import java.util.zip.*;
import java.io.*;
import java.net.*;
import java.lang.reflect.*;

/**
 * Implements a class registry for a given interface (that must itself
 * extend the {@link IAliasableClass} marker interface).  This registry
 * is capable of searching the entire java CLASSPATH for all classes
 * that implement the specified interface.  A hash of instances of 
 * the given interface is also stored, so that an instance of any class
 * implementing the interface, can be retrieved by referring to the 
 * &quot;class alias&quot;.
 *
 * @author Stephen Ramsey
 */
public class ClassRegistry
{
    /*========================================*
     * constants
     *========================================*/
    private static final String FIELD_NAME_CLASS_ALIAS = "CLASS_ALIAS";
    private static final String MANIFEST_DIR_NAME = "META-INF";
    private static HashSet sAliasableClasses;
    private static final String PACKAGE_ROOT = "org.systemsbiology";
    
    static
    {
        sAliasableClasses = null;
    }
    
    /*========================================*
     * member data
     *========================================*/
    private Class mInterface;
    private HashMap mRegistry;
    private HashMap mInstances;

    /*========================================*
     * accessor/mutator methods
     *========================================*/
    private void setInstances(HashMap pInstances)
    {
        mInstances = pInstances;
    }

    private HashMap getInstances()
    {
        return(mInstances);
    }

    private void setInterface(Class pInterface)
    {
        mInterface = pInterface;
    }

    private Class getInterface()
    {
        return(mInterface);
    }

    private void setRegistry(HashMap pRegistry)
    {
        mRegistry = pRegistry;
    }

    private HashMap getRegistry()
    {
        return(mRegistry);
    }

    /*========================================*
     * initialization methods
     *========================================*/
    /*========================================*
     * constructors
     *========================================*/
    /**
     * Create a ClassRegistry instance.  The
     * <code>pInterface</code> argument must specify
     * an interface that extends the <code>IAliasableClass</code>
     * interface.  Lets assume the interface is called
     * &quot;<code>IFoo</code>&quot;.  The class registry
     * instance (when you call <code>buildRegistry</code>)
     * will build a list of all objects in the classpath
     * that implement the <code>IFoo</code> interface, and
     * that contain the <code>CLASS_ALIAS</code> public static
     * String field.  
     */
    public ClassRegistry(Class pInterface) throws IllegalArgumentException
    {
        checkInterface(pInterface);
        setInterface(pInterface);
        setRegistry(new HashMap());
        setInstances(new HashMap());
    }

    /*========================================*
     * private methods
     *========================================*/

    private void registerClassIfImplementingInterface(String pClassName,
                                                      Class pInterface,
                                                      HashMap pRegistry) throws IOException, IllegalArgumentException
    {
        Class theClass = null;
        try
        {
            theClass = getClass().getClassLoader().loadClass(pClassName);
        }
        catch(ClassNotFoundException e)
        {
            System.err.println("class file is not a valid class: " + pClassName);
            return;                
        }
        catch(NoClassDefFoundError e)
        {
            // apparently this error message happens some times, not clear why; just ignore any
            // class that has no class definition found, but log an error message
            System.err.println("class definition not found: " + pClassName);
            return;                
        }

        if(theClass.isInterface())
        {
            // interfaces are to be skipped, because they cannot implement interfaces
            return;
        }

        if(pInterface.isAssignableFrom(theClass))
        {
            String className = theClass.getName();

            Field aliasField = null;
            try
            {
                aliasField = theClass.getDeclaredField(FIELD_NAME_CLASS_ALIAS);
            }
            catch(NoSuchFieldException e)
            {
                System.err.println(FIELD_NAME_CLASS_ALIAS + " field does not exist in class: " + className);
                return;
            }
                            
            String classAlias = null;

            try
            {
                classAlias = (String) aliasField.get(null);
            }
            catch(IllegalAccessException e)
            {
                System.err.println(FIELD_NAME_CLASS_ALIAS + " field is not public in class: " + className);
                return;
            }
                            
            String foundClassName = (String) pRegistry.get(classAlias);
            if(null != foundClassName && ! foundClassName.equals(className))
            {
                System.err.println("two classes are found to have the same class alias \"" + classAlias + "\"; first class is: \"" + foundClassName + "\"; second class is \"" + className + "\"; ignoring second class");
            }
            pRegistry.put(classAlias, className);
        }
    }

    private boolean classImplementsInterface(String pClassName,
                                             Class pInterface)
    {
        boolean retVal = false;
        
        Class theClass = null;
        try
        {
            theClass = getClass().getClassLoader().loadClass(pClassName);
            if(! theClass.isInterface() &&
                 pInterface.isAssignableFrom(theClass))
            {
                retVal = true;
            }
        }
        catch(Throwable e)
        {
            System.err.println("warning:  there is a problem with class file \"" + pClassName + "\"");
        }
      
        return retVal;
    }
    
    private void searchForClassesImplementingInterface(String pPackageName,
                                                       HashSet pPackagesAlreadySearched,
                                                       Class pInterface,
                                                       HashSet pClassesImplementingInterface) throws IOException, IllegalArgumentException
    {
        String resourceName = pPackageName.replace('.', '/');
        if(! resourceName.startsWith("/"))
        {
            resourceName = "/" + resourceName;
        }
        if(resourceName.endsWith("/"))
        {
            resourceName = resourceName.substring(0, resourceName.length() - 1);
        }
        assert (!resourceName.endsWith("/")) : "resource name ended with slash: " + resourceName;

        URL url = ClassRegistry.class.getResource(resourceName);
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        if(null == url)
        {
            url = pInterface.getResource(resourceName);
            if(null == url)
            {
                url = systemClassLoader.getResource(resourceName);
            }
        }
        if(null != url)
        {
            File directory = new File(url.getFile());
            String directoryName = directory.getAbsolutePath();
            if(directory.exists())
            {
                String []files = directory.list();
                int numFiles = files.length;
                for(int fileCtr = 0; fileCtr < numFiles; ++fileCtr)
                {
                    String fileName = files[fileCtr];
                    if(fileName.equals("CVS"))
                    {
                        continue;
                    }

                    String fullFileName = directoryName + "/" + fileName;
                    File subFile = new File(fullFileName);
                    if(subFile.isDirectory())
                    {
                        // this entry is a package
                        String subPackageResourceName = resourceName + "/" + fileName;
                        String subPackageName = (subPackageResourceName.substring(1, subPackageResourceName.length())).replace('/', '.');
                        
                        if(pPackagesAlreadySearched.contains(subPackageName))
                        {
                            continue;
                        }
                        pPackagesAlreadySearched.add(subPackageName);

                        searchForClassesImplementingInterface(subPackageName,
                                                              pPackagesAlreadySearched,
                                                              pInterface,
                                                              pClassesImplementingInterface);
                    }


                    if(fileName.endsWith(".class"))
                    {
                        // it is a class file; remove the ".class" extension to get the class name
                        String packageName = null;
                        if(resourceName.startsWith("/"))
                        {
                            packageName = resourceName.substring(1,resourceName.length());
                        }
                        else
                        {
                            packageName = resourceName;
                        }
                        packageName = packageName.replace('/', '.');
                        String className = packageName + "." + fileName.substring(0, fileName.length() - 6);
                        if(! pClassesImplementingInterface.contains(className) &&
                           classImplementsInterface(className, pInterface))
                        {
                            pClassesImplementingInterface.add(className);
                        }
                    }
                }
            }
            else
            {
                // must be a jar file
		URLConnection uconn = url.openConnection();
		if(uconn instanceof JarURLConnection)
		{
		    JarURLConnection conn = (JarURLConnection)uconn;
		    String starts = conn.getEntryName();
		    JarFile jfile = conn.getJarFile();
		    Enumeration e = jfile.entries();
		    while (e.hasMoreElements()) 
		    {
			ZipEntry entry = (ZipEntry)e.nextElement();
			String entryName = entry.getName();
			if(entryName.endsWith("/"))
			{
			    // this entry is a directory
			    if(entryName.equals(MANIFEST_DIR_NAME + "/"))
			    {
				continue;
			    }

			    // this entry is a package
			    String subPackageResourceName = entryName;
			    String subPackageName = entryName.replace('/', '.');
			    if(pPackagesAlreadySearched.contains(subPackageName))
			    {
				continue;
			    }
			    pPackagesAlreadySearched.add(subPackageName);

			    searchForClassesImplementingInterface(subPackageName,
								  pPackagesAlreadySearched,
								  pInterface,
								  pClassesImplementingInterface);
			}
			if (entryName.startsWith(starts)
			    &&(entryName.lastIndexOf('/')<=starts.length())
			    &&entryName.endsWith(".class")) 
			{
			    String classname = entryName.substring(0,entryName.length()-6);
			    if (classname.startsWith("/"))
				classname = classname.substring(1);
			    classname = classname.replace('/','.');
			    if(! pClassesImplementingInterface.contains(classname) &&
			       classImplementsInterface(classname, pInterface))
			    {
				pClassesImplementingInterface.add(classname);
			    }
			}
		    }
		}

            }
        }
    }

    private void searchForClassesImplementingInterface(HashSet pPackagesAlreadySearched, 
                                                       Class pInterface, 
                                                       HashSet pClassesImplementingInterface) throws IOException
    {
        // get list of all packages known to the JRE
        Package []packages = Package.getPackages();
        int numPackages = packages.length;
        for(int packageCtr = 0; packageCtr < numPackages; ++packageCtr)
        {
            Package thePackage = packages[packageCtr];
            String packageName = thePackage.getName();
            searchForClassesImplementingInterface(packageName, 
                                                  pPackagesAlreadySearched, 
                                                  pInterface, 
                                                  pClassesImplementingInterface);
        }

    }

    private void checkInterface(Class pInterface) throws IllegalArgumentException
    {
        if(! pInterface.isInterface())
        {
            throw new IllegalArgumentException("class argument is not an interface: " + pInterface.getName());
        }

  
    }

    /*========================================*
     * protected methods
     *========================================*/

    /*========================================*
     * public methods
     *========================================*/

    /**
     * Searches the classpath for all classes implementing the interface
     * that was specified in the constructor.  This method will take a while
     * to complete, because it is searching the entire classpath.
     */
    public void buildRegistry() throws ClassNotFoundException, IOException, IllegalArgumentException
    {
        if(null == sAliasableClasses)
        {
            sAliasableClasses = new HashSet();
            Class aliasableClassesInterfaceClass = IAliasableClass.class;
            HashSet packagesAlreadySearched = new HashSet();
            searchForClassesImplementingInterface(packagesAlreadySearched, 
                                                  aliasableClassesInterfaceClass,
                                                  sAliasableClasses);
            searchForClassesImplementingInterface(PACKAGE_ROOT,
                                                  packagesAlreadySearched,
                                                  aliasableClassesInterfaceClass,
                                                  sAliasableClasses);
        }

        Iterator classNamesIter = sAliasableClasses.iterator();
        HashMap registry = getRegistry();
        Class targetInterface = getInterface();
        while(classNamesIter.hasNext())
        {
            String className = (String) classNamesIter.next();
            registerClassIfImplementingInterface(className,
                                                 targetInterface,
                                                 registry);
        }
    }

    /**
     * Returns the Class object associated with the specified 
     * class alias (a string identifier that uniquely identifies
     * a particular implementation of the interface that was passed
     * to the constructor for this ClassRegistry object).  If no
     * class is found corresponding to the specified alias, an 
     * exception is thrown.
     *
     * @param pClassAlias the alias of the class that is to be returned
     *
     * @return the Class object associated with the specified 
     * class alias
     */
    public Class getClass(String pClassAlias) throws DataNotFoundException
    {
        HashMap registry = getRegistry();
        String className = (String) registry.get(pClassAlias);
        if(null == className)
        {
            throw new DataNotFoundException("unable to locate class for alias: " + pClassAlias);
        }
        Class retClass = null;
        try
        {
            retClass = Class.forName(className);
        }
        catch(ClassNotFoundException e)
        {
            assert false: new String("class not found for classname: " + className);
        }
        return(retClass);
    }

    /**
     * Returns an instance of the class corresponding to the
     * specified class alias <code>pClassAlias</code>.  This
     * object will be an instance of a class that implements the
     * interface that was passed to the <code>ClassRegistry</code>
     * constructor.  If no such instance exists, but the class
     * corresponding to the alias is known, an instance will be
     * created and the reference will be stored and returned.
     *
     * @param pClassAlias the alias of the class that is to be returned
     *
     * @return an instance of the class corresponding to the specified
     * class alias <code>pClassAlias</code>
     */
    public Object getInstance(String pClassAlias) throws DataNotFoundException
    {
        HashMap registry = getRegistry();
        String buildClassName = (String) registry.get(pClassAlias);

        Class interfaceClass = getInterface();

        if(null == buildClassName)
        {
            throw new DataNotFoundException("unable to find class implementing interface \"" + interfaceClass.getName() + "\" with alias \"" + pClassAlias + "\"");
        }

        Class buildClass = null;
        try
        {
            buildClass = Class.forName(buildClassName);
        }
        catch(ClassNotFoundException e)
        {
            throw new DataNotFoundException("unable to find class " + buildClassName, e);
        }
        assert (interfaceClass.isAssignableFrom(buildClass)) : new String("error in class registry; interface class " + interfaceClass.getName() + " is not assignable from build class: " + buildClassName);

        HashMap instances = getInstances();
        Object instance = instances.get(buildClassName);
        if(null == instance)
        {
            try
            {
                instance = buildClass.newInstance();
            }
            catch(Exception e)
            {
                throw new DataNotFoundException("unable to instantiate class " + buildClassName, e);
            }
            instances.put(buildClassName, instance);
        }

        return(instance);
    }
    
    /**
     * Print out a summary of the contents of the 
     * class registry, to the specified PrintStream
     * <code>pString</code>.
     */
    public void printRegistry(PrintStream pStream)
    {
        HashMap registry = getRegistry();
        Set aliases = registry.keySet();
        Iterator aliasesIter = aliases.iterator();
        while(aliasesIter.hasNext())
        {
            String alias = (String) aliasesIter.next();
            String className = (String) registry.get(alias);
            pStream.println(alias + " -> " + className);
        }
    }

    /**
     * Return a Set containing copies of all of the
     * aliases (as strings) for objects implementing
     * the interface that was passed to the ClassRegistry
     * constructor.
     *
     * @return a Set containing copies of all of the
     * aliases (as strings) for objects implementing
     * the interface that was passed to the ClassRegistry
     * constructor.
     */
    public Set getRegistryAliasesCopy()
    {
        HashSet newRegistryAliases = new HashSet();
        Set registryAliases = getRegistry().keySet();
        Iterator aliasesIter = registryAliases.iterator();
        while(aliasesIter.hasNext())
        {
            String alias = (String) aliasesIter.next();
            newRegistryAliases.add(alias);
        }
        return(newRegistryAliases);
    }

    public void clearInstances()
    {
        HashMap instances = getInstances();
        instances.clear();
    }

    /**
     * Test method for this class
     */
    public static void main(String []pArgs)
    {
        try
        {
            ClassRegistry classRegistry = new ClassRegistry(Class.forName(pArgs[0]));
            classRegistry.buildRegistry();
            classRegistry.printRegistry(System.out);
        }
        catch(Exception e)
        {
            e.printStackTrace(System.err);
        }
    }
}
