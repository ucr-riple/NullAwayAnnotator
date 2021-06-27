package edu.ucr.cs.riple.diagnose.explorer;
import edu.ucr.cs.riple.injector.Fix;

public abstract class AbstractExplorer {
    final boolean isActive;

    public AbstractExplorer(ExplorerConfig config) {
        this.isActive = checkActivation(config);
    }

    protected abstract boolean checkActivation(ExplorerConfig config);

    public Fix[] run(ExplorerConfig config){
        if(!isActive){
            return null;
        }
        init(config);
        Context context = makeContext();
        context.addNeighbor(new com.uber.nullaway.autofix.fixer.Fix(), new com.uber.nullaway.autofix.fixer.Fix());
        context.getNode(new com.uber.nullaway.autofix.fixer.Fix());
        context.updateFixEffect(new com.uber.nullaway.autofix.fixer.Fix(), 0);
        context.updateStateWithFix(new com.uber.nullaway.autofix.fixer.Fix());
        return solveContext(context);
    }

    protected abstract void init(ExplorerConfig config);

    protected abstract Fix[] solveContext(Context c);

    protected abstract Context makeContext();
}
