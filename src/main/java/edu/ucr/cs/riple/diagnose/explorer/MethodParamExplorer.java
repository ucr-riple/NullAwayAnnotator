package edu.ucr.cs.riple.diagnose.explorer;

import edu.ucr.cs.riple.injector.Fix;

import java.util.ArrayList;
import java.util.List;

public class MethodParamExplorer extends AbstractExplorer{

    public MethodParamExplorer(ExplorerConfig config) {
        super(config);
    }

    @Override
    protected boolean checkActivation(ExplorerConfig config) {
        return config.METHOD_PARAM_TEST_ACTIVE;
    }

    @Override
    protected void init(ExplorerConfig config) {
        //        static class Node{
//            boolean applied;
//            final int id;
//            int referred;
//            int effect;
//
//            final com.uber.nullaway.autofix.fixer.Fix fix;
//            List<Context.Node> neighbors;

//            private Node(com.uber.nullaway.autofix.fixer.Fix fix, int id){
//                this.fix = fix;
//                this.id = id;
//            }

//            static Context.Node findOrCreate(com.uber.nullaway.autofix.fixer.Fix fix){
//                int hash = fix.hashCode();
//                List<Context.Node> candidates = nodes.get(hash);
//                if(candidates == null){
//                    Context.Node node = new Context.Node(fix, LAST_ID++);
//                    List<Context.Node> newList = new ArrayList<>();
//                    newList.add(node);
////                    nodes.put(hash, newList);
//                    return node;
//                }
//                for(Context.Node candidate: candidates){
//                    if(candidate.fix.equals(fix)){
//                        return candidate;
//                    }
//                }
//                Context.Node node = new Context.Node(fix, LAST_ID++);
//                candidates.add(node);
//                return node;
//            }
//
//            static Context.Node find(com.uber.nullaway.autofix.fixer.Fix fix){
//                List<Context.Node> candidates = nodes.get(fix.hashCode());
//                if(candidates != null) {
//                    for (Context.Node candidate : candidates) {
//                        if (candidate.fix.equals(fix)) {
//                            return candidate;
//                        }
//                    }
//                }
//                return null;
//            }
//        }
    }

    @Override
    protected Fix[] solveContext(Context c) {
//        static class Node{
//            boolean applied;
//            final int id;
//            int referred;
//            int effect;
//
//            final com.uber.nullaway.autofix.fixer.Fix fix;
//            List<Context.Node> neighbors;

//            private Node(com.uber.nullaway.autofix.fixer.Fix fix, int id){
//                this.fix = fix;
//                this.id = id;
//            }

//            static Context.Node findOrCreate(com.uber.nullaway.autofix.fixer.Fix fix){
//                int hash = fix.hashCode();
//                List<Context.Node> candidates = nodes.get(hash);
//                if(candidates == null){
//                    Context.Node node = new Context.Node(fix, LAST_ID++);
//                    List<Context.Node> newList = new ArrayList<>();
//                    newList.add(node);
////                    nodes.put(hash, newList);
//                    return node;
//                }
//                for(Context.Node candidate: candidates){
//                    if(candidate.fix.equals(fix)){
//                        return candidate;
//                    }
//                }
//                Context.Node node = new Context.Node(fix, LAST_ID++);
//                candidates.add(node);
//                return node;
//            }
//
//            static Context.Node find(com.uber.nullaway.autofix.fixer.Fix fix){
//                List<Context.Node> candidates = nodes.get(fix.hashCode());
//                if(candidates != null) {
//                    for (Context.Node candidate : candidates) {
//                        if (candidate.fix.equals(fix)) {
//                            return candidate;
//                        }
//                    }
//                }
//                return null;
//            }
//        }
        return new Fix[0];
    }

    @Override
    protected Context makeContext() {
        //        static class Node{
//            boolean applied;
//            final int id;
//            int referred;
//            int effect;
//
//            final com.uber.nullaway.autofix.fixer.Fix fix;
//            List<Context.Node> neighbors;

//            private Node(com.uber.nullaway.autofix.fixer.Fix fix, int id){
//                this.fix = fix;
//                this.id = id;
//            }

//            static Context.Node findOrCreate(com.uber.nullaway.autofix.fixer.Fix fix){
//                int hash = fix.hashCode();
//                List<Context.Node> candidates = nodes.get(hash);
//                if(candidates == null){
//                    Context.Node node = new Context.Node(fix, LAST_ID++);
//                    List<Context.Node> newList = new ArrayList<>();
//                    newList.add(node);
////                    nodes.put(hash, newList);
//                    return node;
//                }
//                for(Context.Node candidate: candidates){
//                    if(candidate.fix.equals(fix)){
//                        return candidate;
//                    }
//                }
//                Context.Node node = new Context.Node(fix, LAST_ID++);
//                candidates.add(node);
//                return node;
//            }
//
//            static Context.Node find(com.uber.nullaway.autofix.fixer.Fix fix){
//                List<Context.Node> candidates = nodes.get(fix.hashCode());
//                if(candidates != null) {
//                    for (Context.Node candidate : candidates) {
//                        if (candidate.fix.equals(fix)) {
//                            return candidate;
//                        }
//                    }
//                }
//                return null;
//            }
//        }
        return  new Context();
    }
}

