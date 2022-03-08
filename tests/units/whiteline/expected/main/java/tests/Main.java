package tests;

public class Main {

  Object field2 = new Object();

  public Object returnNullable() {
    return null;
  }

  public void useNullable1(Object p) {
    Integer i = p.hashCode();
  }

  public void useNullable2(Object p) {
    Integer i = p.hashCode();
  }

  public void useNullable3(Object p) {

    Integer i = p.hashCode();

    Integer b = p.hashCode();
  }

  public void run() {
    useNullable1(returnNullable());
    useNullable2(returnNullable());
    useNullable3(returnNullable());
  }
}
