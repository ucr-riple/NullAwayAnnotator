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
    private Index currentInMethod;
    private Index currentInClass;

    public void setup(){
        rootInClass = new Index(Index.Type.BY_CLASS);
        rootInMethod = new Index(Index.Type.BY_METHOD);
        rootInMethod.index();
        rootInClass.index();
        Preconditions.checkArgument(rootInClass.total == rootInMethod.total);
    }

    public void saveState(boolean saveClass, boolean saveMethod){
        if(saveClass){
            currentInClass = new Index(Index.Type.BY_CLASS);
        }
        if(saveMethod){
            currentInMethod = new Index(Index.Type.BY_METHOD);
        }
    }

    public int compareByClass(String className, boolean fresh){
        if(fresh){
            saveState(true, false);
        }
        List<Error> currentErrors = currentInClass.getByHash(Objects.hash(className));
        currentErrors = currentErrors.stream().filter(error -> error.clazz.equals(className)).collect(Collectors.toList());
        List<Error> previousErrors = rootInClass.getByHash(Objects.hash(className));
        previousErrors = previousErrors.stream().filter(error -> error.clazz.equals(className)).collect(Collectors.toList());
        return currentErrors.size() - previousErrors.size();
    }

    public int compareByMethod(String className, String methodName, boolean fresh) {
        if(fresh){
            saveState(false, true);
        }
        List<Error> currentErrors = currentInMethod.getByHash(Objects.hash(className, methodName));
        currentErrors = currentErrors.stream().filter(error -> error.clazz.equals(className) && error.method.equals(methodName)).collect(Collectors.toList());
        List<Error> previousErrors = rootInMethod.getByHash(Objects.hash(className, methodName));
        previousErrors = previousErrors.stream().filter(error -> error.clazz.equals(className) && error.method.equals(methodName)).collect(Collectors.toList());
        return currentErrors.size() - previousErrors.size();
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
