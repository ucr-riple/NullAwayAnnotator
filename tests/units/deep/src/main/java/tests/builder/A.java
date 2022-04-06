package tests.builder;

public class A implements X {

  protected Y arg2 = null;

  protected Y arg3 = null;

  protected A(Builder builder) {
    this.arg2 = builder.arg2;
    this.arg3 = builder.arg3;
  }

  public static class Builder {

    protected Y arg2 = null;

    protected Y arg3 = null;

    public Builder setArg2(Y arg2) {
      if (this.arg2 != null) {
        this.arg2.release();
      }
      this.arg2 = arg2;
      return this;
    }

    public Builder setArg3(Y arg3) {
      if (this.arg3 != null) {
        this.arg3.release();
      }
      this.arg3 = arg3;
      return this;
    }
  }
}
