package edu.ucr.cs.riple.autofixer.metadata.index;

import com.google.common.base.Preconditions;
import edu.ucr.cs.riple.autofixer.nullaway.Writer;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class Bank<T extends Hashable> {

  private final Index<T> rootInClass;
  private final Index<T> rootInMethod;
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

  private Result<T> compareByList(List<T> previousItems, List<T> currentItems) {
    int effect = currentItems.size() - previousItems.size();
    previousItems.forEach(currentItems::remove);
    return new Result<>(effect, currentItems);
  }

  public Result<T> compareByClass(String className, boolean fresh) {
    saveState(fresh, false);
    return compareByList(rootInClass.getByClass(className), currentInClass.getByClass(className));
  }

  public Result<T> compareByMethod(String className, String methodName, boolean fresh) {
    saveState(false, fresh);
    return compareByList(
        rootInMethod.getByMethod(className, methodName),
        currentInMethod.getByMethod(className, methodName));
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
