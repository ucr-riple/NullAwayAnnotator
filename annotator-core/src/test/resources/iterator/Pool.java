package test;

import java.util.Stack;
import org.jspecify.nullness.NullUnmarked;

@NullUnmarked
public abstract class Pool<T> {
  /** The maximum number of objects that will be pooled. */
  public final int max;
  /** The highest number of free objects. Can be reset any time. */
  public int peak;

  private final Stack<T> freeObjects;

  public Pool(int initialCapacity, int max) {
    freeObjects = new Stack();
    this.max = max;
  }

  protected abstract T newObject();

  public T obtain() {
    return freeObjects.pop();
  }

  public void free(T object) {}

  public void fill(int size) {}

  protected void reset(T object) {}

  protected void discard(T object) {}

  public void freeAll(Stack<T> objects) {}

  public void clear() {}

  public int getFree() {
    return 0;
  }

  public static interface Poolable {
    public void reset();
  }
}
