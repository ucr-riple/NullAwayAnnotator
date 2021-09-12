package edu.ucr.cs.riple.autofixer.metadata.index;

import com.google.common.base.Preconditions;
import edu.ucr.cs.riple.autofixer.nullaway.Writer;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class Bank<T extends Hashable> {

  public final Index<T> rootInClass;
  public final Index<T> rootInMethod;
  private Index<T> currentInMethod;
  private Index<T> currentInClass;
  private final Factory<T> factory;
  private final String path;

  public Bank(String path, Factory<T> factory) {
    this.factory = factory;
    this.path = path;
    rootInClass = new Index<>(path, Index.Type.BY_CLASS, factory);
    rootInMethod = new Index<>(path, Index.Type.BY_METHOD, factory);
    rootInMethod.index();
    rootInClass.index();
    Preconditions.checkArgument(rootInClass.total == rootInMethod.total);
  }

  public void saveState(boolean saveClass, boolean saveMethod) {
    if (saveClass) {
      currentInClass = new Index<>(this.path, Index.Type.BY_CLASS, factory);
      currentInClass.index();
    }
    if (saveMethod) {
      currentInMethod = new Index<>(this.path, Index.Type.BY_METHOD, factory);
      currentInMethod.index();
    }
  }

  public int compareByClassSize(String className, boolean fresh) {
    saveState(fresh, false);
    List<T> currentItems = currentInClass.getByClass(className);
    List<T> previousItems = rootInClass.getByClass(className);
    return currentItems.size() - previousItems.size();
  }

  public int compareByMethodSize(String className, String methodName, boolean fresh) {
    saveState(false, fresh);
    List<T> currentItems = currentInMethod.getByMethod(className, methodName);
    List<T> previousItems = rootInMethod.getByMethod(className, methodName);
    return currentItems.size() - previousItems.size();
  }

  public int compareBySize() {
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

  public List<T> compareByMethod(String className, String method, boolean fresh) {
    saveState(false, fresh);
    List<T> currentItems = currentInMethod.getByMethod(className, method);
    List<T> previousItems = rootInMethod.getByMethod(className, method);
    previousItems.forEach(currentItems::remove);
    return currentItems;
  }
}
