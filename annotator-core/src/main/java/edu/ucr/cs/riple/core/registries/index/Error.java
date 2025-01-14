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
import edu.ucr.cs.riple.core.checkers.DiagnosticPosition;
import edu.ucr.cs.riple.core.registries.region.Region;
import edu.ucr.cs.riple.injector.changes.AddAnnotation;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/** Represents an error reported by NullAway. */
@SuppressWarnings("JavaLangClash")
public abstract class Error {

  /** Error Type. */
  public final String messageType;

  /** Error message. */
  public final String message;

  /** Position where this error is reported */
  public final DiagnosticPosition position;

  /** Path to the file where the error is reported. */
  public final Path path;

  /** The fixes which can resolve this error (possibly empty). */
  protected final ImmutableSet<Fix> resolvingFixes;

  /** Containing region. */
  protected final Region region;

  /** Error type for method initialization errors from NullAway in {@code String}. */
  public Error(
      String messageType,
      String message,
      Region region,
      Path path,
      DiagnosticPosition position,
      Set<AddAnnotation> annotations) {
    this.region = region;
    this.messageType = messageType;
    this.message = message;
    this.path = path;
    this.position = position;
    this.resolvingFixes = computeFixesFromAnnotations(annotations);
  }

  /**
   * Creates a set of {@link Fix} instances from the provided set of annotations that resolve the
   * error. A fix instance can contain multiple annotations, which are grouped for evaluation. A fix
   * is an input to the search algorithm, and if approved, all its contained annotations will be
   * applied to the source code.
   *
   * @param annotations A set of annotations that, if fully applied, resolve the error. Each fix can
   *     contain a subset of these annotations.
   * @return A set of fix instances, each representing a possible group of annotations.
   */
  protected abstract ImmutableSet<Fix> computeFixesFromAnnotations(Set<AddAnnotation> annotations);

  /**
   * Getter for the set of fixes that resolves this error.
   *
   * @return Set of fixes that resolves this error.
   */
  public ImmutableSet<Fix> getResolvingFixes() {
    return this.resolvingFixes;
  }

  /**
   * Returns a stream of resolving fixes for this error.
   *
   * @return Stream of resolving fixes.
   */
  public Stream<Fix> getResolvingFixesStream() {
    return this.resolvingFixes.stream();
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
    Preconditions.checkArgument(isSingleAnnotationFix());
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
        && position.equals(other.position);
  }

  /**
   * Checks if error is resolvable and all suggested fixes must be applied to an element in target
   * module.
   *
   * @param context Annotator context instance.
   * @return true, if error is resolvable via fixes on target module.
   */
  public boolean isFixableOnTarget(Context context) {
    return this.resolvingFixes.stream().allMatch(context.targetModuleInfo::declaredInModule);
  }

  @Override
  public int hashCode() {
    return Objects.hash(messageType, message, region, resolvingFixes, position);
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
        + position
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
