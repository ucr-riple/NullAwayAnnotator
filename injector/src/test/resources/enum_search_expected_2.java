package injector;
import javax.annotation.Nullable;

public enum Main {

    EC1 {
        @Override
        public String bar() {
            return "EC1 IMPL";
        }
    },

    EC2 {
        @Nullable @Override
        public String bar() {
            return "EC2 IMPL";
        }
    };

    A a = new A() {
        @Override
        public String bar() {
            return null;
        }
    };

    public final void foo() {
        System.out.println("TEST");
    }

    public abstract String bar();
}

interface A {
    String bar();
}
