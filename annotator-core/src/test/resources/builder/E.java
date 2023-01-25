package test;

public class E<T> extends B<T> {

  private E(Builder<T> builder) {
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

    public E<T> build() {
      return new E<>(this);
    }
  }
}
