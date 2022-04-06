package tests.parampass;

public class Other {

  Object f;

  Object d;

  Object p;

  Other() {
    Main main = new Main();
    this.f = main.f;
    this.d = main.d;
    this.p = main.p;
  }

  public Object f_run() {
    f_param(f, d, p);
    return f;
  }

  public Object d_run() {
    d_param(f, d, p);
    return d;
  }

  public Object p_run() {
    d_param(f, d, p);
    return p;
  }

  public void f_param(Object f, Object f1, Object f2) {
    f_param1(f, f1, f2);
  }

  public void f_param1(Object f, Object f1, Object f2) {
    f_param2(f, f1, f2);
  }

  public void f_param2(Object f, Object f1, Object f2) {
    f_param3(f, f1, f2);
  }

  public void f_param3(Object f, Object f1, Object f2) {
    f_param4(f, f1, f2);
  }

  public void f_param4(Object f, Object f1, Object f2) {
    f_param5(f, f1, f2);
  }

  public void f_param5(Object f, Object f1, Object f2) {
    f_param6(f, f1, f2);
  }

  public void f_param6(Object f, Object f1, Object f2) {}

  public void d_param(Object f, Object f1, Object f2) {
    d_param1(f, f1, f2);
  }

  public void d_param1(Object f, Object f1, Object f2) {
    d_param2(f, f1, f2);
  }

  public void d_param2(Object f, Object f1, Object f2) {
    d_param3(f, f1, f2);
  }

  public void d_param3(Object f, Object f1, Object f2) {
    d_param4(f, f1, f2);
  }

  public void d_param4(Object f, Object f1, Object f2) {
    d_param5(f, f1, f2);
  }

  public void d_param5(Object f, Object f1, Object f2) {
    d_param6(f, f1, f2);
  }

  public void d_param6(Object f, Object f1, Object f2) {}
}
