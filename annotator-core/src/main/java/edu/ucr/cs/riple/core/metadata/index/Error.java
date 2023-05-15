/*
 * MIT License
 *
 * Copyright (c) 2020 Nima Karimipour
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package edu.ucr.cs.riple.core.metadata.index;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

/** Represents an error reported by NullAway. */
@SuppressWarnings("JavaLangClash")
public class Error {

  /** Error Type. */
  public final String messageType;
  /** Error message. */
  public final String message;
  /** The fixes which can resolve this error (possibly empty). */
  private final ImmutableSet<Fix> resolvingFixes;
  /** Offset of program point in original version where error is reported. */
  private final int offset;
  /** Containing region. */
  protected final Region region;
  /** Error type for method initialization errors from NullAway in {@code String}. */
  public static final String METHOD_INITIALIZER_ERROR = "METHOD_NO_INIT";
  /** Error type for field initialization errors from NullAway in {@code String}. */
  public static final String FIELD_INITIALIZER_ERROR = "FIELD_NO_INIT";

  public Error(
      String messageType,
      String message,
      Region region,
      int offset,
      ImmutableSet<Fix> resolvingFixes) {
    this.region = region;
    this.messageType = messageType;
    this.message = message;
    this.offset = offset;
    this.resolvingFixes = resolvingFixes;
  }

  public Error(
      String messageType, String message, Region region, int offset, @Nullable Fix resolvingFix) {
    this(
        messageType,
        message,
        region,
        offset,
        resolvingFix == null ? ImmutableSet.of() : ImmutableSet.of(resolvingFix));
  }

  /**
   * Checks if error is resolvable.
   *
   * @return true if error is resolvable and false otherwise.
   */
  public boolean hasFix() {
    return this.resolvingFixes.size() > 0;
  }

  public ImmutableSet<Fix> getResolvingFixes() {
    return this.resolvingFixes;
  }

  /**
   * Checks if error is resolvable with only one fix.
   *
   * @return true if error is resolvable with only one fix and false otherwise.
   */
  public boolean isSingleFix() {
    return this.resolvingFixes.size() == 1;
  }

  /**
   * Returns the location the single fix that resolves this error.
   *
   * @return Location of the fix resolving this error.
   */
  public Location toResolvingLocation() {
    Preconditions.checkArgument(resolvingFixes.size() == 1);
    // no get() method, have to use iterator.
    return resolvingFixes.iterator().next().toLocation();
  }

  /**
   * Returns the location of the parameter that can resolve this error.
   *
   * @return Location of parameter that can resolve this error.
   */
  public OnParameter toResolvingParameter() {
    Location resolvingLocation = toResolvingLocation();
    Preconditions.checkArgument(resolvingLocation.isOnParameter());
    return resolvingLocation.toParameter();
  }

  /**
   * Fully qualified name of the containing region.
   *
   * @return Fully qualified name the class.
   */
  public String encClass() {
    return this.region.clazz;
  }

  /**
   * Representative member of the containing region as {@code String}.
   *
   * @return Member symbol in {@code String}.
   */
  public String encMember() {
    return this.region.member;
  }

  /**
   * Getter for region.
   *
   * @return region instance.
   */
  public Region getRegion() {
    return this.region;
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

  /**
   * Checks if error is resolvable and all suggested fixes must be applied to an element in target
   * module.
   *
   * @param config Annotator config instance.
   * @return true, if error is resolvable via fixes on target module.
   */
  public boolean isFixableOnTarget(Config config) {
    return resolvingFixes.size() > 0
        && this.resolvingFixes.stream()
            .allMatch(fix -> config.targetModuleInfo.declaredInModule(fix.toLocation()));
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

  @Override
  public String toString() {
    return "Type='"
        + messageType
        + '\''
        + ", message='"
        + message
        + '\''
        + ", offset='"
        + offset
        + '\'';
  }

  /**
   * Collects all resolving fixes in given collection of errors as an immutable set.
   *
   * @param errors Collection of errors.
   * @return Immutable set of fixes which can resolve all given errors.
   */
  public static ImmutableSet<Fix> getResolvingFixesOfErrors(Collection<Error> errors) {
    // Each error has a set of resolving fixes and each fix has a set of reasons as why the fix has
    // been suggested. The final returned set of fixes should contain all the reasons it has been
    // suggested across the given collection. Map below stores all the set of reasons each fix is
    // suggested in the given collection.

    // Collect all reasons each fix is suggested across the given collection.
    Map<Fix, Set<String>> fixReasonsMap = new HashMap<>();
    errors.stream()
        .flatMap(error -> error.resolvingFixes.stream())
        .forEach(
            fix -> {
              if (fixReasonsMap.containsKey(fix)) {
                fixReasonsMap.get(fix).addAll(fix.reasons);
              } else {
                fixReasonsMap.put(fix, new HashSet<>(fix.reasons));
              }
            });

    ImmutableSet.Builder<Fix> builder = ImmutableSet.builder();
    for (Fix key : fixReasonsMap.keySet()) {
      // To avoid mutating fixes stored in the given collection, we create new instances.
      // which contain the full set of reasons.
      builder.add(
          new Fix(
              key.change, ImmutableSet.copyOf(fixReasonsMap.get(key)), key.fixSourceIsInTarget));
    }
    return builder.build();
  }

  /**
   * Checks if this error is resolvable with the given collection of fixes.
   *
   * @param fixes Collection fixes.
   * @return true, if this error is resolvable.
   */
  public boolean isResolvableWith(Collection<Fix> fixes) {
    if (resolvingFixes.size() == 0) {
      return false;
    }
    return fixes.containsAll(this.resolvingFixes);
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
