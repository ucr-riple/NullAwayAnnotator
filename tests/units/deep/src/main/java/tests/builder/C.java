package tests.builder;

public final class C extends A {

  protected C(A.Builder builder) {
    super(builder);
  }

  public static class Builder extends A.Builder {

    @Override
    public Builder setArg2(Y arg2) {
      super.setArg2(arg2);
      return this;
    }

    @Override
    public Builder setArg3(Y arg3) {
      super.setArg3(arg3);
      return this;
    }
  }
}
