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
package edu.ucr.cs.riple.core.registries.index;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Context;
import edu.ucr.cs.riple.core.registries.region.Region;
import edu.ucr.cs.riple.injector.changes.AddAnnotation;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/** Represents an error reported by NullAway. */
@SuppressWarnings("JavaLangClash")
public abstract class Error {

  /** Error Type. */
  public final String messageType;
  /** Error message. */
  public final String message;
  /** The fixes which can resolve this error (possibly null). */
  protected final Set<Fix> resolvingFixes;
  /** Offset of program point in original version where error is reported. */
  protected final int offset;
  /** Containing region. */
  protected final Region region;

  /** Error type for method initialization errors from NullAway in {@code String}. */
  public Error(
      String messageType,
      String message,
      Region region,
      int offset,
      Set<AddAnnotation> annotations) {
    this.region = region;
    this.messageType = messageType;
    this.message = message;
    this.offset = offset;
    this.resolvingFixes = computeFixesFromAnnotations(annotations);
  }

  protected abstract Set<Fix> computeFixesFromAnnotations(Set<AddAnnotation> annotations);

  public Set<Fix> getFixes() {
    return this.resolvingFixes;
  }

  /**
   * Checks if error is resolvable with only one annotation.
   *
   * @return true if error is resolvable with only one annotation and false otherwise.
   */
  public boolean isSingleAnnotationFix() {
    return !resolvingFixes.isEmpty() && resolvingFixes.iterator().next().changes.size() == 1;
  }

  /**
   * Returns the location the single fix that resolves this error.
   *
   * @return Location of the fix resolving this error.
   */
  public Location toResolvingLocation() {
    Preconditions.checkArgument(
        !resolvingFixes.isEmpty() && resolvingFixes.iterator().next().changes.size() == 1);
    // no get() method, have to use iterator.
    return resolvingFixes.iterator().next().changes.iterator().next().getLocation();
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
    Error other = (Error) o;
    return messageType.equals(other.messageType)
        && region.equals(other.region)
        && message.equals(other.message)
        && resolvingFixes.equals(other.resolvingFixes)
        && offset == other.offset;
  }

  /**
   * Checks if error is resolvable and all suggested fixes must be applied to an element in target
   * module.
   *
   * @param context Annotator context instance.
   * @return true, if error is resolvable via fixes on target module.
   */
  public boolean isFixableOnTarget(Context context) {
    return this.resolvingFixes.stream()
        .flatMap(fix -> fix.changes.stream())
        .allMatch(change -> context.targetModuleInfo.declaredInModule(change.getLocation()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(messageType, message, region, resolvingFixes, offset);
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
  public static <T extends Error> ImmutableSet<Fix> getResolvingFixesOfErrors(
      Collection<T> errors) {
    return errors.stream()
        .flatMap(t -> t.resolvingFixes.stream())
        .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * Checks if this error is resolvable with the given collection of fixes.
   *
   * @param fixes Collection fixes.
   * @return true, if this error is resolvable.
   */
  public boolean isResolvableWith(Collection<Fix> fixes) {
    if (this.resolvingFixes.isEmpty()) {
      return false;
    }
    return fixes.containsAll(this.resolvingFixes);
  }
}
