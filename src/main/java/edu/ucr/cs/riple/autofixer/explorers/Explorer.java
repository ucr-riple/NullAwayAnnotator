package edu.ucr.cs.riple.autofixer.explorers;

public abstract class Explorer {

    public abstract String typeSupport();

    public abstract void init();

    protected abstract Context makeContext();
}
