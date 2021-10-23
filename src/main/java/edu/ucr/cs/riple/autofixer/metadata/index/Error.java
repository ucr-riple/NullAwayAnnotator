package edu.ucr.cs.riple.autofixer.metadata.index;

// todo we have to store more information for dif to work in bank

public class Error extends Hashable {
  public final String messageType;
  public final String message;
  public final boolean covered;

  public Error(String[] infos) {
    this(infos[0], infos[1], infos[2], infos[3], Boolean.parseBoolean(infos[4]));
  }

  public Error(String messageType, String message, String clazz, String method, boolean covered) {
    this.messageType = messageType;
    this.message = message;
    this.method = method;
    this.clazz = clazz;
    this.covered = covered;
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
