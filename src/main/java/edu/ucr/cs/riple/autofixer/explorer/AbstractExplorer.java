package edu.ucr.cs.riple.autofixer.explorer;
import edu.ucr.cs.riple.autofixer.nullaway.AutoFixConfig;
import edu.ucr.cs.riple.injector.Fix;

public abstract class AbstractExplorer {
    final boolean isActive;

    public AbstractExplorer(AutoFixConfig config) {
        this.isActive = checkActivation(config);
    }

    protected abstract boolean checkActivation(AutoFixConfig config);

    public Fix[] run(AutoFixConfig config){
        if(!isActive){
            return null;
        }
        init(config);
        Context context = makeContext();
        return solveContext(context);
    }

    protected abstract void init(AutoFixConfig config);

    protected abstract Fix[] solveContext(Context c);

    protected abstract Context makeContext();
}
