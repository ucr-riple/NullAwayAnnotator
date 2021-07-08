package edu.ucr.cs.riple.autofixer.metadata;


import java.util.ArrayList;
import java.util.List;

public class CallGraph extends AbstractRelation<CallGraphNode>{


    public CallGraph(String filePath) {
        super(filePath);
    }

    @Override
    protected CallGraphNode addNodeByLine(String[] values) {
        return new CallGraphNode(values[0], values[1], values[2]);
    }

    public String[] getUserClassesOfMethod(String method, String inClass){
        List<CallGraphNode> nodes = findAllNodes((candidate, values) -> candidate.calleeMethod.equals(values[0]) && candidate.calleeClass.equals(values[1]), method, inClass);
        if(nodes == null || nodes.size() == 0){
            return null;
        }
        List<String> ans = new ArrayList<>();
        for(CallGraphNode node: nodes){
            ans.add(node.callerClass);
        }
        return ans.toArray(new String[0]);
    }
}

