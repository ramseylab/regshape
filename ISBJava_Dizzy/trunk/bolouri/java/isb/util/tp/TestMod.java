package isb.util.tp;

class TestMod
{
    public static final void main(String []pArgs)
    {
        String argStr = pArgs[0];
        double arg = Double.parseDouble(argStr);
        double val = arg % 2.5;
        System.out.println("val: " + val);
    }
}
