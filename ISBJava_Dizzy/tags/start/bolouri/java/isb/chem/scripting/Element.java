package isb.chem.scripting;

import java.util.*;

/**
 * Represents an attribute of a <code>Statement</code>.  An element
 * is analogous to the Microsoft &quot;Variant&quot; data type.
 * Elements can have one of four possible &quot;data types&quot;,
 * specified by the <code>Element.DataType</code> enumeration:
 * <dl>
 * <dt><code>symbol</code></dt>
 * <dd>a symbol element is an element that specifies a symbolic
 * name, such as the name of a chemical species or a reaction name.</dd>
 * <dt><code>number</code></dt>
 * <dd>a number element is an element that specifies a numeric
 * value, such as a reaction parameter or a compartment volume.</dd>
 * <dt><code>data-list</code></dt>
 * <dd>a data-list element is an element that contains a list of
 * data, where the data can be symbols or number values.</dd>
 * <dd><code>modifier</code></dt>
 * <dd>a modifier element is an element that contains an 
 * enumeration data type, such as &quot;floating&quot; or
 * &quot;boundary&quot; (as apply to the <code>speciesType</code>
 * element).</dd>
 * </dl>
 * Elements are normally not manipulated directly, but are usually
 * processed by the {@link ScriptRuntime} class.
 * 
 * @see ScriptRuntime
 *
 * @author Stephen Ramsey
 */

class Element implements Cloneable
{
    /*========================================*
     * constants
     *========================================*/

    /*========================================*
     * inner classes
     *========================================*/

    static class DataType
    {
        private final String mName;
        private DataType(String pName)
        {
            mName = pName;
        }

        public String toString()
        {
            return(mName);
        }

        public static final DataType MODIFIER = new DataType("keyword");
        public static final DataType DOUBLE = new DataType("double");
        public static final DataType SYMBOL = new DataType("symbol");
        public static final DataType SYMBOLLIST = new DataType("symbollist");
        public static final DataType DATALIST = new DataType("datalist");
        public static final DataType INTEGER = new DataType("integer");
    }

    static class Type implements Comparable
    {
        private final String mName;
        private static HashMap mTypes;

        private Type(String pName)
        {
            mName = pName;
            mTypes.put(pName, this);
        }

        static
        {
            mTypes = new HashMap();
        }

        public String toString()
        {
            return(mName);
        }

        public static Iterator getIter()
        {
            return(mTypes.values().iterator());
        }

        public static Type get(String pName)
        {
            return((Type) mTypes.get(pName));
        }

        public int compareTo(Object pObject)
        {
            return(mName.compareTo(((Element.Type) pObject).mName));
        }

        public static final Type SPECIESTYPE = new Type("speciesType");
        public static final Type COMPARTMENT = new Type("compartment");
        public static final Type RATE = new Type("rate");
        public static final Type SPECIESMODE = new Type("speciesMode");
        public static final Type VOLUME = new Type("volume");
        public static final Type PRODUCTS = new Type("products");
        public static final Type REACTANTS = new Type("reactants");
        public static final Type PARAMETERS = new Type("parameters");
        public static final Type SUBMODELS = new Type("submodels");
        public static final Type VALUE = new Type("value");
        public static final Type REACTIONS = new Type("reactions");
        public static final Type SPECIESLIST = new Type("speciesList");
        public static final Type POPULATIONS = new Type("populations");
        public static final Type STARTTIME = new Type("startTime");
        public static final Type STOPTIME = new Type("stopTime");
        public static final Type SPECIESPOPULATIONS = new Type("speciesPopulations");
        public static final Type NUMTIMEPOINTS = new Type("numTimePoints");
        public static final Type VIEWSPECIES = new Type("viewSpecies");
        public static final Type OUTPUT = new Type("output");
        public static final Type SPECIES = new Type("species");
        public static final Type POPULATION = new Type("population");
        public static final Type START = new Type("start");
        public static final Type STOP = new Type("stop");
        public static final Type STEP = new Type("step");
        public static final Type REACTION = new Type("reaction");
        public static final Type DEBUG = new Type("debug");
        public static final Type ENSEMBLESIZE = new Type("ensembleSize");
        public static final Type PARAMETER = new Type("parameter");
        public static final Type OUTPUTFILE = new Type("outputFile");
        public static final Type STORENAME = new Type("storeName");
        public static final Type SIMULATOR = new Type("simulator");
        public static final Type EXPORTER = new Type("exporter");
    }

    static class ModifierCode
    {
        private final String mName;
        private static HashMap mCodes;

        static
        {
            mCodes = new HashMap();
        }

        private ModifierCode(String pName)
        {
            mName = pName;
            mCodes.put(pName, this);
        }

        public String toString()
        {
            return(mName);
        }

        public static ModifierCode get(String pName)
        {
            return((ModifierCode) mCodes.get(pName));
        }

        public static final ModifierCode BOUNDARY = new ModifierCode("boundary");
        public static final ModifierCode FLOATING = new ModifierCode("floating");
        public static final ModifierCode CONCENTRATION = new ModifierCode("concentration");
        public static final ModifierCode MOLECULES = new ModifierCode("molecules");
        public static final ModifierCode PRINT = new ModifierCode("print");
        public static final ModifierCode STORE = new ModifierCode("store");
    }

    /*========================================*
     * member data
     *========================================*/
    private final Type mType;
    private Double mNumberValue;
    private String mSymbolName;
    private ModifierCode mModifier;
    private List mDataList;  // list of strings

    /*========================================*
     * accessor/mutator methods
     *========================================*/
    Type getType()
    {
        return(mType);
    }

    String getSymbolName()
    {
        return(mSymbolName);
    }

    void setSymbolName(String pSymbolName)
    {
        mSymbolName = pSymbolName;
    }

    Double getNumberValue()
    {
        return(mNumberValue);
    }

    void setNumberValue(Double pNumberValue)
    {
        mNumberValue = pNumberValue;
    }

    ModifierCode getModifier()
    {
        return(mModifier);
    }

    void setModifier(ModifierCode pModifier)
    {
        mModifier = pModifier;
    }

    private void setDataList(List pDataList)
    {
        mDataList = pDataList;
    }

    private List getDataList()
    {
        return(mDataList);
    }

    /*========================================*
     * initialization methods
     *========================================*/
    /*========================================*
     * constructors
     *========================================*/
    public Element(Type pType)
    {
        mType = pType;
        setNumberValue(null);
        setSymbolName(null);
        setDataList(null);
        setModifier(null);
    }

    /*========================================*
     * private methods
     *========================================*/


    /*========================================*
     * protected methods
     *========================================*/
    void createDataList() throws IllegalStateException
    {
        if(hasDataList())
        {
            throw new IllegalStateException("element already has a data list");
        }
        setDataList(new LinkedList());
    }

    /*========================================*
     * public methods
     *========================================*/

    public boolean hasDataList()
    {
        return(null != getDataList());
    }

    public Object clone()
    {
        Element newElement = new Element(getType());
        newElement.setNumberValue(getNumberValue());
        newElement.setSymbolName(getSymbolName());
        newElement.setModifier(getModifier());
        if(hasDataList())
        {
            newElement.createDataList();
            Iterator dataIter = getDataListIter();
            List newElementDataList = newElement.getDataList();
            while(dataIter.hasNext())
            {
                Object datum = dataIter.next();
                newElementDataList.add(datum);
            }
        }
        return(newElement);
    }
    
    public void addDataToList(String pSymbolName) throws IllegalStateException
    {
        List dataList = getDataList();
        if(null == dataList)
        {
            throw new IllegalStateException("data list has not been created yet");
        }
        getDataList().add(pSymbolName);
    }

    public void addDataToList(Double pDataValue)
    {
        getDataList().add(pDataValue);
    }

    public ListIterator getDataListIter()
    {
        return(getDataList().listIterator());
    }

    public String toString()
    {
        StringBuffer retStr = new StringBuffer();
        retStr.append(getType().toString());

        if(null != getNumberValue())
        {
            retStr.append(" " + getNumberValue());
        }
        else if(null != getSymbolName())
        {
            retStr.append(" \"" + getSymbolName() + "\"");
        }
        else if(null != getModifier())
        {
            retStr.append(" " + getModifier());
        }
        else
        {
            List dataList = getDataList();
            if(null != dataList &&
               dataList.size() > 0)
            {
                retStr.append(" (");
                Iterator dataIter = dataList.iterator();
                while(dataIter.hasNext())
                {
                    Object datum = dataIter.next();
                    if(datum instanceof String)
                    {
                        retStr.append("\"" + ((String) datum).toString() + "\"");
                    }
                    else if(datum instanceof Double)
                    {
                        retStr.append(((Double) datum).toString());
                    }
                    else
                    {
                        assert false : "unknown data type found in element data list";
                    }
                    if(dataIter.hasNext())
                    {
                        retStr.append(" ");
                    }
                }
                retStr.append(")");
            }
            else
            {
                retStr.append(" null");
            }
        }

        return(retStr.toString());
    }



    public void validate(HashMap pAllowedDataTypes) throws IllegalStateException
    {
        Set allowedDataTypes = (Set) pAllowedDataTypes.get(mType);
        if(null == allowedDataTypes)
        {
            throw new IllegalStateException("element type " + mType + " has no associated collection of allowed element data types");
        }

        DataType elementDataType = null;

        if(null != mNumberValue)
        {
            if(null != mSymbolName ||
               null != mDataList ||
               null != mModifier)
            {
                throw new IllegalStateException("two or more element members are defined: " + toString());
            }
            Double numberValue = mNumberValue;
            if(((double) numberValue.longValue()) != numberValue.doubleValue())
            {
                elementDataType = DataType.DOUBLE;
            }
            else
            {
                elementDataType = DataType.INTEGER;
            }
        }
        else if(null != mSymbolName)
        {
            if(null != mNumberValue ||
               null != mDataList ||
               null != mModifier)
            {
                throw new IllegalStateException("two or more element members are defined");
            }
            elementDataType = DataType.SYMBOL;
        }
        else if(null != mDataList)
        {
            if(null != mNumberValue ||
               null != mSymbolName ||
               null != mModifier)
            {
                throw new IllegalStateException("two or more element members are defined");
            }
            Iterator dataIter = getDataList().iterator();
            elementDataType = DataType.SYMBOLLIST;
            while(dataIter.hasNext())
            {
                Object datum = dataIter.next();
                if(datum instanceof String)
                {
                    // do nothing
                }
                else if(datum instanceof Double)
                {
                    elementDataType = DataType.DATALIST;
                }
                else
                {
                    assert false : "unknown data type found in element data list";
                }
            }
        }
        else if(null != mModifier)
        {
            if(null != mNumberValue ||
               null != mSymbolName ||
               null != mDataList)
            {
                throw new IllegalStateException("two or more element members are defined");
            }
            elementDataType = DataType.MODIFIER;
        }
        else
        {
            throw new IllegalStateException("no element data type found");
        }
        if(! allowedDataTypes.contains(elementDataType))
        {
            throw new IllegalStateException("element data type " + elementDataType + " is not permitted for this element type: " + mType);
        }
    }
}
