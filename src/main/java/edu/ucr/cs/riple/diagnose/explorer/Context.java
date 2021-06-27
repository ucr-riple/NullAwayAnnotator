package edu.ucr.cs.riple.diagnose.explorer;

import com.uber.nullaway.autofix.fixer.Fix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@SuppressWarnings("ALL")

public class Context {

    static final Map<Integer, List<Node>> nodes = new HashMap<>();
    private static int LAST_ID = 0;

    static class Node{
        boolean applied;
        final int id;
        int referred;
        int effect;

        final Fix fix;
        List<Node> neighbors;

        private Node(Fix fix, int id){
            this.fix = fix;
            this.id = id;
        }

        static Node findOrCreate(Fix fix){
            int hash = fix.hashCode();
            List<Node> candidates = nodes.get(hash);
            if(candidates == null){
                Node node = new Node(fix, LAST_ID++);
                List<Node> newList = new ArrayList<>();
                newList.add(node);
                nodes.put(hash, newList);
                return node;
            }
            for(Node candidate: candidates){
                if(candidate.fix.equals(fix)){
                    return candidate;
                }
            }
            Node node = new Node(fix, LAST_ID++);
            candidates.add(node);
            return node;
        }

        static Node find(Fix fix){
            List<Node> candidates = nodes.get(fix.hashCode());
            if(candidates != null) {
                for (Node candidate : candidates) {
                    if (candidate.fix.equals(fix)) {
                        return candidate;
                    }
                }
            }
            return null;
        }
    }

    public Node getNode(Fix fix){
        return Node.find(fix);
    }

    public void updateStateWithFix(Fix fix){
        Node node = Node.findOrCreate(fix);
        node.referred++;
    }

    public void updateFixEffect(Fix fix, int effect){
        Node node = Node.findOrCreate(fix);
        node.effect = effect;
    }

    public void addNeighbor(Fix cause, Fix fix){
        Node base = Node.findOrCreate(cause);
        Node neighbor = Node.findOrCreate(fix);
        base.neighbors.add(neighbor);
    }
}
