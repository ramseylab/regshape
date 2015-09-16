package org.systemsbiology.chem;

/**
 * Represents possible units of a value in the
 * {@link SpeciesCompartmentValue} class.
 *
 * @author Stephen Ramsey
 */
public class SpeciesCompartmentValueUnits
{
    private final String mName;

    private SpeciesCompartmentValueUnits(String pName)
    {
        mName = pName;
    }

    public String toString()
    {
        return(mName);
    }

    public static final SpeciesCompartmentValueUnits MOLES_PER_LITER = new SpeciesCompartmentValueUnits("moles-per-liter");
    public static final SpeciesCompartmentValueUnits NUMBER_MOLECULES= new SpeciesCompartmentValueUnits("number-molecules");
}
