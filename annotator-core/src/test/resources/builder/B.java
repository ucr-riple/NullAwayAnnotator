package test;

import java.util.HashMap;
import java.util.Map;

public abstract class B<T> extends A implements Z {

  protected Map<String, String> headers;

  protected T body = null;

  protected B(Builder<T> builder) {
    super(builder);
    this.body = builder.body;
    this.headers = builder.headers;
  }

  public static class Builder<T> extends A.Builder {

    // These have setters too, also overriden by every subclass
    protected Map<String, String> headers = new HashMap<>();

    protected T body = null;

    @Override
    public Builder<T> setArg2(Y arg2) {
      if (arg2 != null && !this.headers.isEmpty()) {
        throw new IllegalStateException("Cannot set both `arg2` and `headers`.");
      }
      super.setArg2(arg2);
      return this;
    }

    @Override
    public Builder<T> setArg3(Y arg3) {
      if (arg3 != null && this.body != null) {
        throw new IllegalStateException("Cannot set both `arg3` and `body`.");
      }
      super.setArg3(arg3);
      return this;
    }

    public Builder<T> setHeaders(Map<String, String> headers) {
      this.headers = headers;
      return this;
    }

    public Builder<T> setBody(T body) {
      this.body = body;
      return this;
    }
  }
}
