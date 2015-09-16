package isb.chem.scripting;

import java.util.*;

/**
 * Represents a single instruction, as a part of a {@link Script}, that
 * is intended to be executed within the {@link ScriptRuntime}.  
 * The function of this class is analogous to the &quot;Command&quot; 
 * pattern described in the <em>Design Patterns</em> book.
 * A {@link Script} is an ordered collection of
 * <code>Statement</code> objects.  A statement contains
 * a <code>Statement.Type</code>, a <code>String</code> 
 * statement &quot;name&quot; (a symbol that goes into
 * the global namespace of the {@link ScriptRuntime}),
 * and a collection of <code>Element</code> objects.  The 
 * <code>Element</code> objects give the details of the
 * command and its attributes.  
 *  
 * @see Script
 * @see ScriptRuntime
 *
 * @author Stephen Ramsey
 */
public class Statement implements Cloneable
{
    /*========================================*
     * constants
     *========================================*/

    /*========================================*
     * inner classes
     *========================================*/

    static class Category
    {
        private final String mName;
        private Category(String pName)
        {
            mName = pName;
        }
        public String toString()
        {
            return(mName);
        }
        public static final Category DEFINITION = new Category("definition");
        public static final Category PERFACTION = new Category("perfaction");
    }

    /**
     * Represents a statement type.
     */
    static class Type
    {
        private final String mName;
        private final Category mCategory;
        private static HashMap mTypes;

        static
        {
            mTypes = new HashMap();
        }

        private Type(String pName, Category pCategory)
        {
            mName = pName;
            mCategory = pCategory;
            mTypes.put(pName, this);
        }

        public String toString()
        {
            return(mName);
        }

        public static final Type SPECIES = new Type("species", Category.DEFINITION);
        public static final Type COMPARTMENT = new Type("compartment", Category.DEFINITION);
        public static final Type REACTION = new Type("reaction", Category.DEFINITION);
        public static final Type PARAMETER = new Type("parameter", Category.DEFINITION);
        public static final Type MODEL = new Type("model", Category.DEFINITION);
        public static final Type ADDREACTIONTOMODEL = new Type("addReactionToModel", Category.DEFINITION);
        public static final Type ADDPARAMETERTOMODEL = new Type("addParameterToModel", Category.DEFINITION);
        public static final Type SPECIESPOPULATIONS = new Type("speciesPopulations", Category.DEFINITION);
        public static final Type ADDTOSPECIESPOPULATIONS = new Type("addToSpeciesPopulations", Category.DEFINITION);
        public static final Type SIMULATE = new Type("simulate", Category.PERFACTION);
        public static final Type PRINTMODEL = new Type("printModel", Category.PERFACTION);
        public static final Type PRINTSPECIESPOPULATIONS = new Type("printSpeciesPopulations", Category.PERFACTION);
        public static final Type VALIDATEMODEL = new Type("validateModel", Category.PERFACTION);
        public static final Type EXPORTMODELINSTANCE = new Type("exportModelInstance", Category.PERFACTION);

        public static Type get(String pName)
        {
            return((Type) mTypes.get(pName));
        }

        public Category getCategory()
        {
            return(mCategory);
        }

        public static Iterator getIter()
        {
            return( mTypes.values().iterator() );
        }
    }

    /*========================================*
     * member data
     *========================================*/
    private final Type mType;
    private HashMap mElements;
    private String mName;

    /*========================================*
     * accessor/mutator methods
     *========================================*/
    Type getType()
    {
        return(mType);
    }

    String getName()
    {
        return(mName);
    }

    void setName(String pName)
    {
        mName = pName;
    }

    HashMap getElements()
    {
        return(mElements);
    }

    /*========================================*
     * initialization methods
     *========================================*/
    /*========================================*
     * constructors
     *========================================*/
    public Statement(Type pType)
    {
        mType = pType;
        mElements = new HashMap();
        mName = null;
    }

    /*========================================*
     * private methods
     *========================================*/

    /*========================================*
     * protected methods
     *========================================*/
    Iterator getElementsIter()
    {
        return(getElements().values().iterator());
    }


    Element getElement(Element.Type pElementType)
    {
        return((Element) getElements().get(pElementType));
    }

    void putElement(Element pElement)
    {
        getElements().put(pElement.getType(), pElement);
    }

    /*========================================*
     * public methods
     *========================================*/
    
    public Object clone()
    {
        Statement newStatement = new Statement(getType());
        newStatement.setName(getName());
        Iterator elementsIter = getElementsIter();
        while(elementsIter.hasNext())
        {
            Element element = (Element) elementsIter.next();
            newStatement.putElement((Element) element.clone());
        }
        return(newStatement);
    }

    public String toString()
    {
        StringBuffer str = new StringBuffer();
        str.append(getType().toString() + " \"" + mName + "\"");
        HashMap elements = getElements();
        Set elementsSet = elements.keySet();
        List elementsList = new LinkedList(elementsSet);
        Collections.sort(elementsList);
        Iterator elementIter = elementsList.iterator();
        if(elementIter.hasNext())
        {
            str.append(": ");
            while(elementIter.hasNext())
            {
                Element.Type elementType = (Element.Type) elementIter.next();
                Element element = (Element) elements.get(elementType);
                str.append(element.toString());
                if(elementIter.hasNext())
                {
                    str.append(", ");
                }
            }
        }
        str.append(";");
        return(str.toString());
    }
}
