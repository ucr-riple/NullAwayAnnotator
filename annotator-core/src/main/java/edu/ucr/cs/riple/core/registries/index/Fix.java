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
import com.google.gson.JsonObject;
import edu.ucr.cs.riple.injector.changes.ASTChange;
import edu.ucr.cs.riple.injector.changes.AddAnnotation;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.LocationKind;
import edu.ucr.cs.riple.injector.location.LocationToJsonVisitor;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnMethod;
import edu.ucr.cs.riple.injector.location.OnParameter;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Stores information suggesting adding @Nullable on an element in source code. These suggestions
 * are coming form NullAway.
 */
public class Fix {

  /** The set of suggested changes that should be evaluated together by this fix instance. */
  public final Set<AddAnnotation> changes;

  public Fix(AddAnnotation change) {
    this(ImmutableSet.of(change));
  }

  public Fix(ImmutableSet<AddAnnotation> change) {
    this.changes = change;
  }

  /**
   * Returns the set of locations targeted by this fix instance.
   *
   * @return Set of locations targeted by this fix instance.
   */
  public Set<Location> toLocations() {
    return changes.stream().map(ASTChange::getLocation).collect(Collectors.toSet());
  }

  /**
   * Checks if fix contains only one change and the change is on a method.
   *
   * @return true, if fix is targeting a method.
   */
  public boolean isOnMethod() {
    return changes.size() == 1 && changes.iterator().next().getLocation().isOnMethod();
  }

  /**
   * Returns the targeted method location if this fix contains only one change and that change is on
   * method.
   *
   * @return Target method information.
   */
  public OnMethod toMethod() {
    Preconditions.checkArgument(
        isOnMethod(), "This fix contains more than one change or the change is not on method.");
    return changes.iterator().next().getLocation().toMethod();
  }

  /**
   * Visit this fix if it is targeting an element of kind {@link LocationKind#METHOD}
   *
   * @param consumer Consumer instance.
   */
  public void ifOnMethod(Consumer<OnMethod> consumer) {
    if (isOnMethod()) {
      changes.iterator().next().getLocation().ifMethod(consumer);
    }
  }

  /**
   * Checks if fix contains only one change and the change is on a parameter.
   *
   * @return true, if fix is targeting a parameter.
   */
  public boolean isOnParameter() {
    return changes.size() == 1 && changes.iterator().next().getLocation().isOnParameter();
  }

  /**
   * Returns the targeted parameter location if this fix contains only one change and that change is
   * on a parameter.
   *
   * @return Target method parameter.
   */
  public OnParameter toParameter() {
    Preconditions.checkArgument(
        isOnParameter(),
        "This fix contains more than one change or the change is not on parameter.");
    return changes.iterator().next().getLocation().toParameter();
  }

  /**
   * Visit this fix if it is targeting an element of kind {@link LocationKind#PARAMETER}
   *
   * @param consumer Consumer instance.
   */
  public void ifOnParameter(Consumer<OnParameter> consumer) {
    if (isOnParameter()) {
      changes.iterator().next().getLocation().ifParameter(consumer);
    }
  }

  /**
   * Checks if fix contains only one change and the change is on a field.
   *
   * @return true, if fix is targeting a field.
   */
  public boolean isOnField() {
    return changes.size() == 1 && changes.iterator().next().getLocation().isOnField();
  }

  /**
   * Returns the targeted field location if this fix contains only one change and that change is on
   * a field.
   *
   * @return Target field information.
   */
  public OnField toField() {
    Preconditions.checkArgument(
        isOnField(), "This fix contains more than one change or the change is not on field.");
    return changes.iterator().next().getLocation().toField();
  }

  /**
   * Visit this fix if it is targeting an element of kind {@link LocationKind#FIELD}
   *
   * @param consumer Consumer instance.
   */
  public void ifOnField(Consumer<OnField> consumer) {
    if (isOnField()) {
      changes.iterator().next().getLocation().ifField(consumer);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Fix)) {
      return false;
    }
    Fix fix = (Fix) o;
    return changes.equals(fix.changes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(changes);
  }

  /**
   * Returns json representation of fix.
   *
   * @return Json instance.
   */
  public JsonObject getJson() {
    return changes.iterator().next().getLocation().accept(new LocationToJsonVisitor(), null);
  }

  /**
   * Checks if fix is a single change and is modifying constructor (parameter or method).
   *
   * @return true, if fix is modifying constructor.
   */
  public boolean isModifyingConstructor() {
    if (!(isOnMethod() || isOnParameter())) {
      return false;
    }
    return (isOnMethod() ? toMethod() : toParameter().enclosingMethod).isOnConstructor();
  }

  @Override
  public String toString() {
    return changes.toString();
  }
}
