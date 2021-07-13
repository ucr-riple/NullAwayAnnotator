package edu.ucr.cs.riple.autofixer.explorers;


import edu.ucr.cs.riple.autofixer.Diagnose;
import edu.ucr.cs.riple.autofixer.DiagnoseReport;
import edu.ucr.cs.riple.autofixer.errors.Bank;
import edu.ucr.cs.riple.autofixer.metadata.MethodInheritanceTree;
import edu.ucr.cs.riple.autofixer.metadata.MethodNode;
import edu.ucr.cs.riple.autofixer.nullaway.AutoFixConfig;
import edu.ucr.cs.riple.autofixer.nullaway.Writer;
import edu.ucr.cs.riple.injector.Fix;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class MethodParamExplorer extends Explorer {

    MethodInheritanceTree mit;

    public MethodParamExplorer(Diagnose diagnose, Bank bank) {
        super(diagnose, bank);
        mit = diagnose.methodInheritanceTree;
        makeAllNodes();
        measureNullSafetyAllMethods(diagnose, bank);
    }

    private void makeAllNodes() {
        try {
            try (BufferedReader br = new BufferedReader(new FileReader(Writer.SUGGEST_FIX))) {
                String line;
                String delimiter = Writer.getDelimiterRegex();
                while ((line = br.readLine()) != null) {
                    String[] infos = line.split(delimiter);
                    if(!infos[0].equals("METHOD_PARAM")){
                        continue;
                    }
                    String clazz = infos[2];
                    String method = infos[3];
                    int index = Integer.parseInt(infos[5]);
                    Node node = Node.findOrCreate(index, method, clazz);
                    node.referred++;
                }
            }
        }catch (IOException e){
            System.err.println("Exception happened in initializing MethodParamExplorer...");
        }
    }

    private void measureNullSafetyAllMethods(Diagnose diagnose, Bank bank) {
        int maxsize = diagnose.methodInheritanceTree.maxParamSize();
        for (int i = 0; i < maxsize; i++) {
            AutoFixConfig.AutoFixConfigWriter writer = new AutoFixConfig.AutoFixConfigWriter()
                    .setLogError(true, true)
                    .setSuggest(true)
                    .setMethodParamTest(true, i);
            diagnose.writeConfig(writer);
            diagnose.buildProject();
            bank.saveState(false, true);
            for(List<Node> list: Node.nodes.values()){
                for(Node node : list){
                    int localEffect = bank.compareByMethod(node.clazz, node.method, false);
                    node.effect = localEffect + calculateInheritanceViolationError(node, i);
                }
            }
        }
    }


    private int calculateInheritanceViolationError(Node node, int index) {
        int effect = 0;
        boolean[] thisMethodFlag = mit.findNode(node.method, node.clazz).annotFlags;
        for(MethodNode subMethod: mit.getSubMethods(node.method, node.clazz, false)) {
            if (!thisMethodFlag[index]) {
                if (!subMethod.annotFlags[index]) {
                    effect++;
                }
            }
        }
        List<MethodNode> superMethods = mit.getSuperMethods(node.method, node.clazz, false);
        if(superMethods.size() != 0){
            MethodNode superMethod = superMethods.get(0);
            if (!thisMethodFlag[index]) {
                if (superMethod.annotFlags[index]) {
                    effect--;
                }
            }
        }
        return effect;
    }

    @Override
    public DiagnoseReport effect(Fix fix) {
        Node node = Node.find(Integer.parseInt(fix.index), fix.method, fix.className);
        if(node != null){
            return new DiagnoseReport(fix, node.effect - node.referred);
        }
        return super.effect(fix);
    }

    @Override
    public boolean isApplicable(Fix fix) {
        return fix.location.equals("METHOD_PARAM");
    }
}

class Node{
    final int index;
    final String method;
    final String clazz;
    int referred;
    int effect;
    static HashMap<Integer, List<Node>> nodes = new HashMap<>();

    private Node(int index, String method, String clazz) {
        this.index = index;
        this.method = method;
        this.clazz = clazz;
    }

    public static Node findOrCreate(int index, String method, String clazz){
        int hash = Objects.hash(index, method, clazz);
        if(nodes.containsKey(hash)){
            for(Node candidate: nodes.get(hash)){
                if(candidate.method.equals(method) && candidate.clazz.equals(clazz) && candidate.index == index){
                    return candidate;
                }
            }
            Node newNode = new Node(index, method, clazz);
            nodes.get(hash).add(newNode);
            return newNode;
        }
        Node newNode = new Node(index, method, clazz);
        List<Node> newList = new ArrayList<>();
        newList.add(newNode);
        nodes.put(hash, newList);
        return newNode;
    }

    public static Node find(int index, String method, String clazz){
        int hash = Objects.hash(index, method, clazz);
        if(nodes.containsKey(hash)){
            for(Node candidate: nodes.get(hash)){
                if(candidate.method.equals(method) && candidate.clazz.equals(clazz) && candidate.index == index){
                    return candidate;
                }
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node)) return false;
        Node node = (Node) o;
        return index == node.index && method.equals(node.method) && clazz.equals(node.clazz);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, method, clazz);
    }
}

