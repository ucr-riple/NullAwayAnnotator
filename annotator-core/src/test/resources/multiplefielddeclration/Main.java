package test;

public class Main {

  public Object f1, f2, f3, f4 = new Object();

  Object f5;

  public void init() {
    f1 = new Object();
    f2 = new Object();
  }

  public boolean usef2(Object p) {
    return p.toString().equals(f2.toString());
  }

  public void passf2() {
    usef2(f2);
  }

  public void useF3() {
    this.f3 = null;
  }

  public void derefF3() {
    int i0 = f3.hashCode();
  }

  public void derefF31() {
    int i0 = f3.hashCode();
  }

  public void derefF32() {
    int i0 = f3.hashCode();
  }

  public void derefF33() {
    int i0 = f3.hashCode();
  }

  public void derefF34() {
    int i0 = f3.hashCode();
  }

  public Object getf1() {
    return f1;
  }

  public Object getf2() {
    return f2;
  }

  public Object getf3() {
    return f3;
  }

  public Object getf4() {
    return f4;
  }

  public Object getf5() {
    return f5;
  }
}
