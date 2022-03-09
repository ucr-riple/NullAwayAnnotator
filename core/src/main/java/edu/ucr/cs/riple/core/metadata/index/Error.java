package edu.ucr.cs.riple.core.metadata.index;

// todo we have to store more information for dif to work in bank

public class Error extends Hashable {
  public final String messageType;
  public final String message;

  public Error(String[] infos) {
    this(infos[0], infos[1], infos[2], infos[3]);
  }

  public Error(String messageType, String message, String clazz, String method) {
    this.messageType = messageType;
    this.message = message;
    this.method = method;
    this.clazz = clazz;
  }

  @Override
  public String toString() {
    return "Error{"
        + "messageType='"
        + messageType
        + '\''
        + ", message='"
        + message
        + '\''
        + ", clazz='"
        + clazz
        + '\''
        + ", method='"
        + method
        + '\''
        + '}';
  }
}
