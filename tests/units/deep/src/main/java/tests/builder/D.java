package tests.builder;

public class D<T> extends B<T> {

  private D(Builder<T> builder) {
    super(builder);
  }

  public static class Builder<T> extends B.Builder<T> {
    @Override
    public Builder<T> setArg2(Y arg2) {
      super.setArg2(arg2);
      return this;
    }

    @Override
    public Builder<T> setArg3(Y arg3) {
      super.setArg3(arg3);
      return this;
    }

    public D<T> build() {
      return new D<>(this);
    }
  }
}
