package edu.ucr.cs.riple.autofixer.errors;

import com.google.common.base.Preconditions;
import com.uber.nullaway.autofix.Writer;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class Bank {

  public final Index rootInClass;
  public final Index rootInMethod;
  private Index currentInMethod;
  private Index currentInClass;

  public Bank() {
    rootInClass = new Index(Index.Type.BY_CLASS);
    rootInMethod = new Index(Index.Type.BY_METHOD);
    rootInMethod.index();
    rootInClass.index();
    Preconditions.checkArgument(rootInClass.total == rootInMethod.total);
  }

  public void saveState(boolean saveClass, boolean saveMethod) {
    if (saveClass) {
      currentInClass = new Index(Index.Type.BY_CLASS);
      currentInClass.index();
    }
    if (saveMethod) {
      currentInMethod = new Index(Index.Type.BY_METHOD);
      currentInMethod.index();
    }
  }

  public int compareByClass(String className, boolean fresh) {
    if (fresh) {
      saveState(true, false);
    }
    List<Error> currentErrors = currentInClass.getByClass(className);
    List<Error> previousErrors = rootInClass.getByClass(className);
    return currentErrors.size() - previousErrors.size();
  }

  public int compareByMethod(String className, String methodName, boolean fresh) {
    if (fresh) {
      saveState(false, true);
    }
    List<Error> currentErrors = currentInMethod.getByMethod(className, methodName);
    List<Error> previousErrors = rootInMethod.getByMethod(className, methodName);
    return currentErrors.size() - previousErrors.size();
  }

  public int compare() {
    BufferedReader reader;
    int lines = 0;
    try {
      reader = new BufferedReader(new FileReader(Writer.ERROR));
      reader.readLine();
      while (reader.readLine() != null) lines++;
      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return lines - rootInClass.total;
  }
}
