package edu.ucr.cs.riple.autofixer.errors;

import edu.ucr.cs.riple.autofixer.nullaway.Writer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class Index{

    HashMap<Integer, List<Error>> errors;
    int total;

    public enum Type{
        BY_METHOD,
        BY_CLASS;
    }

    Type type;

    public Index(Type type) {
        this.type = type;
        errors = new HashMap<>();
        total = 0;
    }

    public void index(){
        try (BufferedReader br = new BufferedReader(new FileReader(Writer.ERROR))) {
            String line;
            String delimiter = Writer.getDelimiterRegex();
            while ((line = br.readLine()) != null) {
                String[] infos = line.split(delimiter);
                Error error = new Error(infos[0], infos[1], infos[2], infos[3]);
                total++;
                int hash;
                if(type.equals(Type.BY_CLASS)){
                    hash = Objects.hash(error.clazz);
                }else{
                    hash = Objects.hash(error.clazz, error.method);
                }
                if(errors.containsKey(hash)){
                    errors.get(hash).add(error);
                }else{
                    List<Error> newList = new ArrayList<>();
                    newList.add(error);
                    errors.put(hash, newList);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Error> getByHash(int hash){
        return errors.get(hash);
    }
}
