package edu.ucr.cs.riple.autofixer.metadata;

import java.util.Objects;


public class CallGraph extends AbstractRelation<CallGraphNode>{

    public CallGraph(String filePath) {
        super(filePath);
    }

    @Override
    protected Integer addNodeByLine(String[] values) {
        CallGraphNode node = new CallGraphNode(values[0], values[1], values[2]);
        return node.hashCode();
    }
}

class CallGraphNode {
    public final String callerMethod;
    public final String callerClass;
    public final String calleeClass;

    public CallGraphNode(String callerMethod, String callerClass, String calleeClass) {
        this.callerMethod = callerMethod;
        this.callerClass = callerClass;
        this.calleeClass = calleeClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CallGraphNode)) return false;
        CallGraphNode that = (CallGraphNode) o;
        return Objects.equals(callerMethod, that.callerMethod) && Objects.equals(callerClass, that.callerClass) && Objects.equals(calleeClass, that.calleeClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(callerMethod, callerClass, calleeClass);
    }

    @Override
    public String toString() {
        return "CallGraphInfo{" +
                ", method='" + callerMethod + '\'' +
                ", clazz='" + callerClass + '\'' +
                ", callee='" + calleeClass + '\'' +
                '}';
    }
}
