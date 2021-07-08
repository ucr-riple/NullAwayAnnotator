package edu.ucr.cs.riple.autofixer.errors;

import com.google.common.base.Preconditions;
import edu.ucr.cs.riple.autofixer.nullaway.Writer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Bank {

    private Index rootInClass;
    private Index rootInMethod;

    public void setup(){
        rootInClass = new Index(Index.Type.BY_CLASS);
        rootInMethod = new Index(Index.Type.BY_METHOD);
        rootInMethod.index();
        rootInClass.index();
        Preconditions.checkArgument(rootInClass.total == rootInMethod.total);
    }

    public int compareByClass(String className){
        int errors = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(Writer.ERROR))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] infos = line.split(Writer.getDelimiterRegex());
                if(infos[2].equals(className)){
                    errors++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Error> previousErrors = rootInClass.getByHash(Objects.hash(className));
        previousErrors = previousErrors.stream().filter(error -> error.clazz.equals(className)).collect(Collectors.toList());
        return errors - previousErrors.size();
    }

    public int compareByMethod(String className, String methodName) {
        int errors = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(Writer.ERROR))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] infos = line.split(Writer.getDelimiterRegex());
                if (infos[2].equals(className) && infos[3].equals(methodName)) {
                    errors++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Error> previousErrors = rootInMethod.getByHash(Objects.hash(className, methodName));
        previousErrors = previousErrors.stream().filter(error -> error.clazz.equals(className) && error.method.equals(methodName)).collect(Collectors.toList());
        return errors - previousErrors.size();
    }

    public int compare(){
        BufferedReader reader;
        int lines = 0;
        try {
            reader = new BufferedReader(new FileReader(Writer.ERROR));
            while (reader.readLine() != null) lines++;
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines - rootInClass.total;
    }
}
