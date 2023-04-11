package edu.ucr.cs.riple.core.checkers.nullaway;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import java.util.Objects;

public class NullAwayError extends Error {

  /** Error type for method initialization errors from NullAway in {@code String}. */
  public static final String METHOD_INITIALIZER_ERROR = "METHOD_NO_INIT";
  /** Error type for field initialization errors from NullAway in {@code String}. */
  public static final String FIELD_INITIALIZER_ERROR = "FIELD_NO_INIT";

  public NullAwayError(
      String messageType,
      String message,
      Region region,
      int offset,
      ImmutableSet<Fix> resolvingFixes) {
    super(messageType, message, region, offset, resolvingFixes);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Error)) {
      return false;
    }
    Error error = (Error) o;
    if (!messageType.equals(error.messageType)) {
      return false;
    }
    if (!region.equals(error.region)) {
      return false;
    }
    if (messageType.equals(METHOD_INITIALIZER_ERROR)) {
      // we do not need to compare error messages as it can be the same error with a different error
      // message and should not be treated as a separate error.
      return true;
    }
    return message.equals(error.message)
        && resolvingFixes.equals(error.resolvingFixes)
        && offset == error.offset;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        messageType,
        // to make sure equal objects will produce the same hashcode.
        messageType.equals(METHOD_INITIALIZER_ERROR) ? METHOD_INITIALIZER_ERROR : message,
        region,
        resolvingFixes,
        offset);
  }

  /**
   * Returns true if the error is an initialization error ({@code METHOD_NO_INIT} or {@code
   * FIELD_NO_INIT}).
   *
   * @return true, if the error is an initialization error.
   */
  public boolean isInitializationError() {
    return this.messageType.equals(METHOD_INITIALIZER_ERROR)
        || this.messageType.equals(FIELD_INITIALIZER_ERROR);
  }
}
