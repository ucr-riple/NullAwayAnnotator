package test.dep;
import test.Bar;
public class Dep {
    public Bar bar = new Bar();
    public void exec() {
        bar.foo2 = bar.getFoo();
    }
}
