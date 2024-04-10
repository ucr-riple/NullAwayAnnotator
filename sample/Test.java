public class Test {

    @RUntainted List<Map<List<@RUntainted String>, @RUntainted String>> s;

    private @RUntainted String a;
    @RUntainted String b;

    List<@RUntainted String> m = new ArrayList<@RUntainted String>(), n = new ArrayList<@RUntainted String>();

    public void useI(I i, Map<? extends @RUntainted String, ? extends @RUntainted String> map){
        List<@RUntainted String> l = new ArrayList<@RUntainted String>();
    }

    public void foo(){
        useI(new I<@RUntainted String>(){
            public void bar(String s){
                System.out.println(s);
            }
        });
    }

    interface I<T> {
        void bar(T t);
    }

    class B implements I<@RUntainted String> {
        public void bar(@RUntainted List<Map<List<@RUntainted String>, @RUntainted String>> s){
            System.out.println(s);
        }
    }

    private java.lang.@RUntainted Number findNumberName() {
    }
}
