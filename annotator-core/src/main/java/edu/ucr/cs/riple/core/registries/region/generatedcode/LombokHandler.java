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

package edu.ucr.cs.riple.core.registries.region.generatedcode;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.evaluators.graph.processors.ParallelConflictGraphProcessor;
import edu.ucr.cs.riple.core.module.ModuleInfo;
import edu.ucr.cs.riple.core.registries.index.Fix;
import edu.ucr.cs.riple.core.registries.method.MethodRecord;
import edu.ucr.cs.riple.core.registries.region.Region;
import edu.ucr.cs.riple.injector.changes.AddMarkerAnnotation;
import edu.ucr.cs.riple.injector.changes.AnnotationChange;
import edu.ucr.cs.riple.scanner.generatedcode.SourceType;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handler for lombok generated code. It can extend potentially impacted regions for elements which
 * will be use in generated code by <a href="https://projectlombok.org">Lombok</a>. Lombok
 * automatically propagates {@code @Nullable} annotation on fields to getter methods, therefore,
 * extends the set of potentially impacted regions to all callers of that method as well. This
 * region registry, will include all callers of any method region in lombok generated code. This
 * will guarantee that {@link ParallelConflictGraphProcessor} will catch any triggered errors by an
 * annotation including all copied annotations by lombok as well.
 */
public class LombokHandler implements AnnotationProcessorHandler {

  /** Module this handler is associated with. */
  private final ModuleInfo moduleInfo;

  public LombokHandler(ModuleInfo moduleInfo) {
    this.moduleInfo = moduleInfo;
  }

  @Override
  public Set<Region> extendForGeneratedRegions(Set<Region> regions) {
    return regions.stream()
        // filter regions which are created by lombok
        .filter(region -> region.sourceType.equals(SourceType.LOMBOK) && region.isOnMethod())
        // find the corresponding method for the region.
        .map(
            methodRecord ->
                moduleInfo
                    .getMethodRegistry()
                    .findMethodByName(methodRecord.clazz, methodRecord.member))
        .filter(Objects::nonNull)
        // get method location.
        .map(methodNode -> methodNode.location)
        // add potentially impacted regions for the collected methods.
        .flatMap(
            onMethod ->
                moduleInfo
                    .getRegionRegistry()
                    .getMethodRegionRegistry()
                    .getImpactedRegions(onMethod)
                    .stream())
        .collect(Collectors.toSet());
  }

  @Override
  public ImmutableSet<Fix> extendForGeneratedFixes(Set<Fix> fixes) {
    // For now, we support only annotation on fields that are propagated to their corresponding
    // getter methods.
    ImmutableSet.Builder<Fix> builder = ImmutableSet.builder();
    fixes.forEach(
        fix ->
            fix.ifOnField(
                onField ->
                    onField.variables.forEach(
                        name -> {
                          // Expected getter method signature.
                          String getterSignature =
                              "get"
                                  + Character.toUpperCase(name.charAt(0))
                                  + name.substring(1)
                                  + "()";
                          // Check if method is lombok generated.
                          MethodRecord getterMethod =
                              moduleInfo
                                  .getMethodRegistry()
                                  .findMethodByName(onField.clazz, getterSignature);
                          if (getterMethod == null) {
                            // Getter method is not declared. skip.
                            // Note: If the getter method is generated, it should still be in the
                            // registry.
                            return;
                          }
                          if (isLombokGenerated(getterMethod.annotations)) {
                            // Method is lombok generated, add a fix to add the annotation on the
                            // method.
                            if (!(fix.change instanceof AnnotationChange)) {
                              // Only annotation changes are supported for now.
                              return;
                            }
                            AnnotationChange change = (AnnotationChange) fix.change;
                            builder.add(
                                new Fix(
                                    new AddMarkerAnnotation(
                                        getterMethod.location, change.getAnnotationName().fullName),
                                    fix.reasons));
                          }
                        })));
    return builder.build();
  }

  /**
   * Checks if the given annotations contains {@code lombok.generated} annotation. This annotation
   * denotes the annotated node is generated by lombok.
   *
   * @param annotations Nodes annotations.
   * @return True if the given annotations contains {@code lombok.generated} annotation.
   */
  private static boolean isLombokGenerated(ImmutableSet<String> annotations) {
    return annotations.stream().anyMatch(name -> name.equals("lombok.Generated"));
  }
}
