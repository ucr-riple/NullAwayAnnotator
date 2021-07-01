package edu.ucr.cs.riple.diagnose.metadata;

import com.uber.nullaway.autofix.Writer;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MethodInheritanceTree {

    HashMap<Long, MethodNode> nodes;
    HashMap<Integer, List<Long>> idHash;

    public MethodInheritanceTree(String filePath){
        try {
            nodes = new HashMap<>();
            idHash = new HashMap<>();
            fillNodes(filePath);
        }catch (IOException e){
            System.out.println("File at: " + filePath + " not found, skipping inheritance tree creation.");
        }
        catch (ParseException e){
            System.err.println("Error happened in Parsing");
            e.printStackTrace();
        }
    }

    private void fillNodes(String filePath) throws IOException, ParseException {
        BufferedReader reader;
        reader = new BufferedReader(new FileReader(filePath));
        String line = reader.readLine();
        if(line != null) line = reader.readLine();
        while (line != null) {
            String[] values = line.split("(\\$\\*\\$)");
            Long id = Long.parseLong(values[0]);
            MethodInfo info = new MethodInfo(id, values[1], values[2], values[5]);
            MethodNode node;
            if (nodes.containsKey(id)) {
                node = nodes.get(id);
            } else {
                node = new MethodNode();
                nodes.put(id, node);
            }
            Long parentId = Long.parseLong(values[3]);
            node.fillInformation(info, parentId);
            Integer hash = info.hashCode();
            if (idHash.containsKey(hash)) {
                idHash.get(hash).add(id);
            } else {
                List<Long> singleHash = Collections.singletonList(id);
                idHash.put(hash, singleHash);
            }
            if (parentId != -1) {
                MethodNode parent = nodes.get(parentId);
                if (parent == null) {
                    parent = new MethodNode();
                    nodes.put(parentId, parent);
                }
                parent.addChild(id);
            }
            line = reader.readLine();
        }
        reader.close();
    }

    private MethodNode findNode(String method, String clazz){
        MethodNode node = null;
        int hash = Objects.hash(method, clazz);
        List<Long> candidateIds = idHash.get(hash);
        if(candidateIds == null){
            return null;
        }
        for(Long c_id: candidateIds){
            MethodNode candidateNode = nodes.get(c_id);
            if(nodes.get(c_id).value.method.equals(method) && nodes.get(c_id).value.clazz.equals(clazz)){
                node = candidateNode;
                break;
            }
        }
        return node;
    }

    public List<MethodInfo> getSuperMethods(String method, String clazz){
        List<MethodInfo> ans = new ArrayList<>();
        MethodNode node = findNode(method, clazz);
        if(node == null) {
            return ans;
        }
        while (node != null){
            MethodNode parent = nodes.get(node.parent);
            if(parent != null){
                ans.add(parent.value);
            }
            node = parent;
        }
        return ans;
    }

    public List<MethodInfo> getSubMethods(String method, String clazz){
        List<MethodInfo> ans = new ArrayList<>();
        MethodNode node = findNode(method, clazz);
        if(node == null) {
            return ans;
        }
        if(node.children == null){
            return ans;
        }
        Set<Long> workList = new HashSet<>(node.children);
        while (!workList.isEmpty()){
            Set<Long> tmp = new HashSet<>();
            for(Long id: workList){
                MethodNode selected = nodes.get(id);
                if(!ans.contains(selected.value)){
                    ans.add(selected.value);
                    if(selected.children != null) {
                        tmp.addAll(selected.children);
                    }
                }
            }
            workList.clear();
            workList.addAll(tmp);
        }
        return ans;
    }
}

class MethodNode{
    List<Long> children;
    Long parent;
    MethodInfo value;

    void fillInformation(MethodInfo value, Long parent){
        this.value = value;
        this.parent = parent;
    }

    void addChild(Long id){
        if(children == null){
            children = new ArrayList<>();
        }
        children.add(id);
    }

    @Override
    public String toString() {
        return "MethodNode{" +
                "child=" + children +
                ", parent=" + parent +
                ", value=" + value +
                '}';
    }
}
