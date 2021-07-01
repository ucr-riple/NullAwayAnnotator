package edu.ucr.cs.riple.diagnose.explorer;

import com.uber.nullaway.autofix.AutoFixConfig;
import edu.ucr.cs.riple.injector.Fix;


public class MethodParamExplorer extends AbstractExplorer{

    public MethodParamExplorer(AutoFixConfig config) {
        super(config);
    }

    @Override
    protected boolean checkActivation(AutoFixConfig config) {
        return config.PARAM_TEST_ENABLED;
    }

    @Override
    protected void init(AutoFixConfig config) {
    }

    @Override
    protected Fix[] solveContext(Context c) {
        return new Fix[0];
    }

    @Override
    protected Context makeContext() {
        return  new Context();
    }
}

