package com.uber;
public class TargetMethodContextSelector implements ContextSelector {
   @Override
   public Context getCalleeTarget() {
     class MethodDispatchContext implements Context {
        @Override
        public ContextItem get(ContextKey name) { }
     }
   }
}
