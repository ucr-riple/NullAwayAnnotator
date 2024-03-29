package edu.ucr.cs.riple.scanner.testdata;

public class SampleClassForTest {

  Object field;

  static class Inner {}

  public void foo() {
    class InnerMethod {
      Run r =
          new Run() {
            @Override
            public void exec() {}
          };
    }
  }

  public Object bar() {
    return new Object();
  }
}

interface Run {
  void exec();
}
