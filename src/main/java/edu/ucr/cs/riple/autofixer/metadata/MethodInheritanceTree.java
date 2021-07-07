package edu.ucr.cs.riple.autofixer.metadata;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MethodInheritanceTree extends AbstractRelation<MethodNode>{


    public MethodInheritanceTree(String filePath) {
        super(filePath);
    }

    @Override
    protected Integer addNodeByLine(String[] values) {
        Integer id = Integer.parseInt(values[0]);
        MethodInfo info = new MethodInfo(id, values[1], values[2], values[5]);
        MethodNode node;
        if (nodes.containsKey(id)) {
            node = nodes.get(id);
        } else {
            node = new MethodNode();
            nodes.put(id, node);
        }
        Integer parentId = Integer.parseInt(values[3]);
        node.fillInformation(info, parentId);
        if (parentId != -1) {
            MethodNode parent = nodes.get(parentId);
            if (parent == null) {
                parent = new MethodNode();
                nodes.put(parentId, parent);
            }
            parent.addChild(id);
        }
        return info.hashCode();
    }


    private MethodNode findNode(String method, String clazz){
        MethodNode node = null;
        int hash = Objects.hash(method, clazz);
        List<Integer> candidateIds = idHash.get(hash);
        if(candidateIds == null){
            return null;
        }
        for(Integer c_id: candidateIds){
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
        Set<Integer> workList = new HashSet<>(node.children);
        while (!workList.isEmpty()){
            Set<Integer> tmp = new HashSet<>();
            for(Integer id: workList){
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
    List<Integer> children;
    Integer parent;
    MethodInfo value;

    void fillInformation(MethodInfo value, Integer parent){
        this.value = value;
        this.parent = parent;
    }

    void addChild(Integer id){
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
