package edu.ucr.cs.riple.autofixer.explorer;

import edu.ucr.cs.riple.autofixer.nullaway.FixDisplay;

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

        final FixDisplay fix;
        List<Node> neighbors;

        private Node(FixDisplay fix, int id){
            this.fix = fix;
            this.id = id;
        }

        static Node findOrCreate(FixDisplay fix){
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

        static Node find(FixDisplay fix){
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

    public Node getNode(FixDisplay fix){
        return Node.find(fix);
    }

    public void updateStateWithFix(FixDisplay fix){
        Node node = Node.findOrCreate(fix);
        node.referred++;
    }

    public void updateFixEffect(FixDisplay fix, int effect){
        Node node = Node.findOrCreate(fix);
        node.effect = effect;
    }

    public void addNeighbor(FixDisplay cause, FixDisplay fix){
        Node base = Node.findOrCreate(cause);
        Node neighbor = Node.findOrCreate(fix);
        base.neighbors.add(neighbor);
    }
}
