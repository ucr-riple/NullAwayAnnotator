package injector;

import javax.annotation.Nullable;

public class Main {

    enum Type {

        FOO;

        public Object get() {
            return null;
        }
    }

    static class Inner {

        public Object bar() {
            return null;
        }
    }

    public static boolean condition() {
        return false;
    }

    Run c = new Run() {

        Comp outerComp = new Comp() {

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
                return new Comp() {

                    @Override
                    @Nullable
                    public Object compare() {
                        return null;
                    }
                };
            }
        }
    };

    Run c1 = new Run() {

        @Override
        public Object exec(@Nullable Object o) {
            return null;
        }
    };

    public void foo() {
        class Helper {

            class InnerHelper {

                public Object foo() {
                    return null;
                }
            }

            Object bar(boolean b) {
                if (b) {
                    return null;
                } else {
                    return new Comp() {

                        @Override
                        public Object compare() {
                            return null;
                        }
                    };
                }
            }
        }
        executeRunner(new Run() {

            Object field;

            @Override
            public Object exec(@Nullable Object o) {
                return null;
            }
        });
    }

    public void foo1() {
        class OtherHelper {

            Object bar(boolean b) {
                return null;
            }
        }
    }

    public void foo2() {
        class Helper {

            Object bar(boolean b) {
                return null;
            }
        }
    }

    void executeRunner(Run r) {
        r.exec(null);
    }
}

class Outer {

    public Object foo() {
        return null;
    }
}
