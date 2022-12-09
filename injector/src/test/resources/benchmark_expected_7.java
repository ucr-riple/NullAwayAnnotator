package injector;

import javax.annotation.Nullable;

public class Main {

  enum Type { // injector.Main$Type
    FOO;

    public Object get() {
      return null;
    }
  }

  static class Inner { // injector.Main$Inner
    Object f0;

    public Object bar() {
      return null;
    }
  }

  public static boolean condition() {
    return false;
  }

  Run c = new Run() { // injector.Main$1
        Object f1;
        final Comp outerComp = new Comp() { // injector.Main$1$1
              Object f2;

              @Override
              public Object compare() {
                return null;
              }
            };

        @Override
        public Object exec(@Nullable Object o) {
          if (condition()) {
            return null;
          } else {
            return new Comp() { // injector.Main$1$2
              Object f3;

              public Object inner() {
                Comp c = new Comp() { // injector.Main$1$2$1
                      Object f4;

                      @Override
                      public Object compare() {
                        class Helper { // injector.Main$1$2$1$1Helper
                          @Nullable
                          Object f5;
                          final Comp c = new Comp() { // injector.Main$1$2$1$1Helper$1
                                Object f6;

                                @Override
                                public Object compare() {
                                  return null;
                                }
                              };
                        }
                        return null;
                      }

                      public void anotherHelper() {
                        class Helper { // injector.Main$1$2$1$2Helper
                          Object f7;
                          final Comp c = new Comp() { // injector.Main$1$2$1$2Helper$1
                                Object f8;

                                @Override
                                public Object compare() {
                                  return null;
                                }
                              };
                        }
                      }

                      class Helper { // injector.Main$1$2$1$Helper
                        Object f9;
                        final Comp c = new Comp() { // injector.Main$1$2$1$Helper$1
                              Object f10;

                              @Override
                              public Object compare() {
                                return null;
                              }
                            };
                      }
                    };
                class Helper { // injector.Main$1$2$1Helper
                  Object f11;
                  final Comp c = new Comp() { // injector.Main$1$2$1Helper$1
                        Object f12;

                        @Override
                        public Object compare() {
                          return null;
                        }
                      };
                }
                return null;
              }

              @Override
              public Object compare() {
                return null;
              }
            };
          }
        }
      };

  Run c1 = new Run() { // injector.Main$2
        Object f13;

        @Override
        public Object exec(@Nullable Object o) {
          return null;
        }
      };

  public void foo() {
    class Helper { // injector.Main$1Helper
      Object f14;

      class InnerHelper { // injector.Main$1Helper$InnerHelper
        Object f15;

        public Object foo() {
          return null;
        }
      }

      Object bar(boolean b) {
        if (b) {
          return null;
        } else {
          return new Comp() { // injector.Main$1Helper$1
            Object f16;

            @Override
            public Object compare() {
              return null;
            }
          };
        }
      }
    }

    executeRunner(
        new Run() { // injector.Main$3
          Object f17;

          @Override
          public Object exec(@Nullable Object o) {
            return null;
          }
        });
  }

  public void foo1() {
    class Helper { // injector.Main$2Helper
      Object f18;

      Object bar(boolean b) {
        return null;
      }
    }
  }

  public void foo2() {
    class Helper { // injector.Main$3Helper
      Object f19;

      Object bar(boolean b) {
        return null;
      }
    }
  }

  void executeRunner(Run r) {
    r.exec(null);
  }
}

class Outer { // injector.Outer
  public Object foo() {
    return null;
  }
}
