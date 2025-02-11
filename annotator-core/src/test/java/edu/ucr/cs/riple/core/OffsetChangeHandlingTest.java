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

package edu.ucr.cs.riple.core;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Collections.singleton;
import static java.util.Map.entry;
import static java.util.function.UnaryOperator.identity;

import com.google.common.collect.ImmutableMap;
import edu.ucr.cs.riple.core.injectors.AnnotationInjector;
import edu.ucr.cs.riple.core.injectors.PhysicalInjector;
import edu.ucr.cs.riple.core.tools.CoreTestHelper;
import edu.ucr.cs.riple.core.tools.Utility;
import edu.ucr.cs.riple.injector.changes.AddMarkerAnnotation;
import edu.ucr.cs.riple.injector.changes.RemoveMarkerAnnotation;
import edu.ucr.cs.riple.injector.location.OnField;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class OffsetChangeHandlingTest {

  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  /** Map of field name to original field location offset. */
  private final ImmutableMap<String, Integer> originalFieldOffsetMap;

  /** Map of field to enclosing flat name. */
  private final ImmutableMap<String, String> fieldClassMap;

  /** Root of tests. */
  private Path root;

  /** Path to input in resources directory. */
  private final Path inputPath;

  private AnnotationInjector injector;
  private Context context;

  @Before
  public void init() {
    root = temporaryFolder.getRoot().toPath();
    CoreTestHelper helper = new CoreTestHelper(root, root).onEmptyProject();
    Path configPath = root.resolve("context.json");
    helper.makeAnnotatorConfigFile(configPath, "javax.annotation.Nullable");
    Utility.runTestWithMockedBuild(
        root,
        () -> {
          Config config = new Config(configPath);
          context = new Context(config);
          injector = new PhysicalInjector(context);
          try {
            Files.copy(inputPath, root.resolve("benchmark.java"));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
  }

  public OffsetChangeHandlingTest() {
    inputPath = Utility.getPathOfResource("offset").resolve("benchmark.java");
    String content;
    try {
      content = Files.readString(inputPath);
      originalFieldOffsetMap =
          IntStream.range(0, 20)
              .mapToObj(value -> "f" + value)
              .collect(toImmutableMap(identity(), content::indexOf));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    fieldClassMap =
        ImmutableMap.ofEntries(
            entry("f0", "test.Main$Inner"),
            entry("f1", "test.Main$1"),
            entry("f2", "test.Main$1$1"),
            entry("f3", "test.Main$1$2"),
            entry("f4", "test.Main$1$2$1"),
            entry("f5", "test.Main$1$2$1$1Helper"),
            entry("f6", "test.Main$1$2$1$1Helper$1"),
            entry("f7", "test.Main$1$2$1$2Helper"),
            entry("f8", "test.Main$1$2$1$2Helper$1"),
            entry("f9", "test.Main$1$2$1$Helper"),
            entry("f10", "test.Main$1$2$1$Helper$1"),
            entry("f11", "test.Main$1$2$1Helper"),
            entry("f12", "test.Main$1$2$1Helper$1"),
            entry("f13", "test.Main$2"),
            entry("f14", "test.Main$1Helper"),
            entry("f15", "test.Main$1Helper$InnerHelper"),
            entry("f16", "test.Main$1Helper$1"),
            entry("f17", "test.Main$3"),
            entry("f18", "test.Main$2Helper"),
            entry("f19", "test.Main$3Helper"));
  }

  @Test
  public void comprehensive() {
    String[] allFields =
        IntStream.range(0, 20).mapToObj(value -> "f" + value).distinct().toArray(String[]::new);
    addAnnotationOn(allFields);
    verifyCalculatedOffsets();
    removeAnnotationOn(allFields);
    verifyCalculatedOffsets();
  }

  @Test
  public void randomOffsetTest() {
    addAnnotationOn("f1", "f2", "f4", "f6");
    verifyCalculatedOffsets();
    addAnnotationOn("f5", "f12");
    verifyCalculatedOffsets();
    removeAnnotationOn("f1", "f2");
    verifyCalculatedOffsets();
    addAnnotationOn("f18", "f19");
    addAnnotationOn("f17", "f1");
    verifyCalculatedOffsets();
    addAnnotationOn("f16", "f17");
    verifyCalculatedOffsets();
    removeAnnotationOn("f18", "12");
    verifyCalculatedOffsets();
    addAnnotationOn("13", "14");
    verifyCalculatedOffsets();
  }

  @Test
  public void loopTest() {
    for (int i = 0; i < 4; i++) {
      addAnnotationOn("f1", "f8", "f19");
      verifyCalculatedOffsets();
      removeAnnotationOn("f1", "f8", "f19");
      verifyCalculatedOffsets();
    }
    addAnnotationOn("f5", "f15");
    removeAnnotationOn("f5");
    for (int i = 0; i < 4; i++) {
      addAnnotationOn("f1", "f8", "f19");
      verifyCalculatedOffsets();
      removeAnnotationOn("f1", "f8", "f19");
      verifyCalculatedOffsets();
    }
  }

  /**
   * Adds annotation on given fields.
   *
   * @param fields Field names.
   */
  private void addAnnotationOn(String... fields) {
    injector.injectAnnotations(
        Arrays.stream(fields)
            .map(
                field ->
                    new AddMarkerAnnotation(
                        new OnField(
                            root.resolve("benchmark.java").toString(),
                            fieldClassMap.get(field),
                            singleton(field)),
                        "javax.annotation.Nullable"))
            .collect(Collectors.toSet()));
  }

  /**
   * Removes annotation from the given fields.
   *
   * @param fields Field names.
   */
  private void removeAnnotationOn(String... fields) {
    injector.removeAnnotations(
        Arrays.stream(fields)
            .map(
                field ->
                    new RemoveMarkerAnnotation(
                        new OnField(
                            root.resolve("benchmark.java").toString(),
                            fieldClassMap.get(field),
                            singleton(field)),
                        "javax.annotation.Nullable"))
            .collect(Collectors.toSet()));
  }

  /** Verifies if the calculated offsets match the original offsets. */
  private void verifyCalculatedOffsets() {
    try {
      String content = Files.readString(root.resolve("benchmark.java"));
      ImmutableMap<String, Integer> calculatedOffsetMap =
          IntStream.range(0, 20)
              .mapToObj(value -> "f" + value)
              .collect(
                  toImmutableMap(
                      identity(),
                      s ->
                          context.offsetHandler.getOriginalOffset(
                              root.resolve("benchmark.java"), content.indexOf(s))));
      Assert.assertEquals(calculatedOffsetMap, originalFieldOffsetMap);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
