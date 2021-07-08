package edu.ucr.cs.riple.autofixer.metadata;


import edu.ucr.cs.riple.autofixer.nullaway.Writer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public abstract class AbstractRelation<T> {
    HashMap<Integer, List<T>> idHash;

    public AbstractRelation(String filePath){
        idHash = new HashMap<>();
        try {
            fillNodes(filePath);
        }catch (IOException e){
            System.out.println("Not found: " + filePath);
        }
    }
    protected void fillNodes(String filePath) throws IOException{
        BufferedReader reader;
        reader = new BufferedReader(new FileReader(filePath));
        String line = reader.readLine();
        if(line != null) line = reader.readLine();
        while (line != null) {
            T node = addNodeByLine(line.split(Writer.getDelimiterRegex()));
            Integer hash = node.hashCode();
            if (idHash.containsKey(hash)) {
                idHash.get(hash).add(node);
            } else {
                List<T> singleHash = Collections.singletonList(node);
                idHash.put(hash, singleHash);
            }
            line = reader.readLine();
        }
        reader.close();
    }

    protected abstract T addNodeByLine(String[] values);

    interface Comparator<T>{
        boolean equals(T candidate, String... arguments);
    }

    protected T findNode(Comparator<T> c, String... arguments){
        T node = null;
        int hash = Arrays.hashCode(arguments);
        List<T> candidateIds = idHash.get(hash);
        if(candidateIds == null){
            return null;
        }
        for(T candidate: candidateIds){
            if(c.equals(candidate, arguments)){
                node = candidate;
                break;
            }
        }
        return node;
    }

    protected List<T> findAllNodes(Comparator<T> c, String... arguments){
        int hash = Arrays.hashCode(arguments);
        List<T> candidateIds = idHash.get(hash);
        if(candidateIds == null){
            return null;
        }
        List<T> nodes = new ArrayList<>();
        for(T candidate: candidateIds){
            if(c.equals(candidate, arguments)){
                nodes.add(candidate);
            }
        }
        return nodes;
    }
}
