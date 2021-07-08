package edu.ucr.cs.riple.autofixer.explorers;



public class MethodParamExplorer extends Explorer {

    @Override
    public void init() {

    }

    @Override
    public String typeSupport() {
        return "METHOD_PARAM";
    }

    @Override
    protected Context makeContext() {
        return  new Context();
    }
}

