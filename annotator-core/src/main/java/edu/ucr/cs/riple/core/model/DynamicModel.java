/*
 * MIT License
 *
 * Copyright (c) 2022 Nima Karimipour
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

package edu.ucr.cs.riple.core.model;

import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.injector.location.Location;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DynamicModel<T extends Impact> implements Model {

  protected final Map<Location, T> store;
  protected final Config config;

  public DynamicModel(Config config) {
    this.store = new HashMap<>();
    this.config = config;
  }

  public Set<Fix> getUnknownFixImpacts(Set<Fix> fixes) {
    return fixes.stream()
        .filter(fix -> !store.containsKey(fix.toLocation()))
        .collect(Collectors.toSet());
  }

  public Set<Error> computeRemainingErrorsOnInjection(Set<Fix> fixes) {
    return fixes.stream()
        .flatMap(fix -> store.get(fix.toLocation()).triggeredErrors.stream())
        .filter(error -> !error.isResolvableWith(fixes))
        .collect(Collectors.toSet());
  }

  public void updateModelStore(Collection<T> newData) {
    newData.forEach(t -> store.put(t.toLocation(), t));
  }

  public void updateModelOnInjection(Collection<Fix> newFixes) {
    this.store.values().forEach(t -> t.updateStatusAfterInjection(newFixes));
  }

  @Override
  public boolean isUnknown(Fix fix) {
    return !this.store.containsKey(fix.toLocation());
  }

  @Override
  public Set<Error> getTriggeredErrors(Fix fix) {
    return null;
  }

  @Override
  public void updateImpactsAfterInjection(Set<Fix> fixes) {}
}
