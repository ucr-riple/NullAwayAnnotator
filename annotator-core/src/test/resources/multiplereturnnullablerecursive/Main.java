package test;

public class Main {

  Object field = new Object();

  boolean b1, b2, b3, b4, b5, b6;

  public Object makeNullable() {
    if (b1) {
      this.field = null;
    }
    if (b2) {
      this.field = null;
    }
    if (b3) {
      this.field = null;
    }
    if (b4) {
      this.field = null;
    }
    if (b5) {
      this.field = null;
    }
    if (b6) {
      this.field = null;
    }
    return new Object();
  }

  public Object returnNullable() {
    if (b1) {
      return null;
    }
    if (b2) {
      return null;
    }
    if (b3) {
      return null;
    }
    if (b4) {
      return null;
    }
    if (b5) {
      return null;
    }
    if (b6) {
      return null;
    }
    return new Object();
  }

  public Object returnNullableRecursive() {
    if (b1) {
      return null;
    }
    if (b2) {
      return null;
    }
    if (b3) {
      return null;
    }
    if (b4) {
      return null;
    }
    if (b5) {
      return null;
    }
    if (b6) {
      return null;
    }
    Object ans = this.returnNullable();
    return ans != null ? ans : new Object();
  }
}
