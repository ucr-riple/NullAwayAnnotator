package edu.ucr.cs.riple.autofixer.metadata;


import edu.ucr.cs.riple.autofixer.nullaway.Writer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public abstract class AbstractRelation<T> {
    HashMap<Integer, T> nodes;
    HashMap<Integer, List<Integer>> idHash;

    public AbstractRelation(String filePath){
        nodes = new HashMap<>();
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
            Integer hash = addNodeByLine(line.split(Writer.getDelimiterRegex()));
            if (idHash.containsKey(hash)) {
                idHash.get(hash).add(hash);
            } else {
                List<Integer> singleHash = Collections.singletonList(hash);
                idHash.put(hash, singleHash);
            }
            line = reader.readLine();
        }
        reader.close();
    }

    protected abstract Integer addNodeByLine(String[] values);
}
