package com.uber;

import javax.annotation.Nullable;

public class TargetMethodContextSelector implements ContextSelector {

    @Override
    public Context getCalleeTarget() {
        class MethodDispatchContext implements Context {

            @Override
            @Nullable
            public ContextItem get(ContextKey name) {
            }
        }
    }
}
