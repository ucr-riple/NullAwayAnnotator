package test;
import javax.annotation.Nullable;
public class Bar {
    @Nullable public String foo;
    @Nullable public String foo2 = "";
    @Nullable public String getFoo() {
        return foo;
    }
}
