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

import static edu.ucr.cs.riple.core.tools.Utility.runTestWithMockedBuild;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javaparser.utils.Pair;
import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.util.FixSerializationConfig;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.scanner.ScannerConfigWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class ConfigurationTest {

  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();
  private Path testDir;
  private ArrayList<CLIFlag> requiredFlagsCli;
  private ArrayList<CLIFlag> requiredDownsStreamDependencyFlagsCli;

  @Before
  public void init() {
    testDir = temporaryFolder.getRoot().toPath();
    // Make dummy context paths for 5 targets
    try (OutputStream os = new FileOutputStream(testDir.resolve("paths.tsv").toFile())) {
      for (int i = 0; i < 5; i++) {
        String row = i + "nullaway.xml" + "\t" + i + "scanner.xml" + "\n";
        os.write(row.getBytes(Charset.defaultCharset()), 0, row.length());
      }
      os.flush();
    } catch (IOException e) {
      throw new RuntimeException(
          "Error happened for writing at file: " + testDir.resolve("paths.tsv"), e);
    }
    try {
      Files.createDirectory(testDir.resolve("0"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    requiredFlagsCli =
        new ArrayList<>(
            List.of(
                new CLIFlagWithValue("bc", "./gradlew compileJava"),
                new CLIFlagWithValue("cp", testDir.resolve("paths.tsv")),
                new CLIFlagWithValue("i", "edu.ucr.Initializer"),
                new CLIFlagWithValue("d", testDir),
                new CLIFlagWithValue("cn", Checker.NULLAWAY.name())));
    requiredDownsStreamDependencyFlagsCli =
        new ArrayList<>(
            List.of(
                new CLIFlag("adda"),
                new CLIFlagWithValue("nlmlp", testDir.resolve("library-model.tsv")),
                new CLIFlagWithValue("ddbc", "./gradlew :dep:compileJava")));
  }

  @Test
  public void testRequiredFlagsMissingCli() {
    // Check if each is missed.
    for (int i = 0; i < requiredFlagsCli.size(); i++) {
      List<CLIFlag> incompleteFlags = new ArrayList<>(requiredFlagsCli);
      CLIFlag missingFlag = incompleteFlags.remove(i);
      String expectedErrorMessage = "Missing required option: " + missingFlag.flag;
      IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> makeConfigWithFlags(incompleteFlags));
      // Check the error message.
      assertTrue(ex.getMessage().contains(expectedErrorMessage));
    }
  }

  @Test
  public void testRequiredFlagsCli() {
    runTestWithMockedBuild(
        testDir,
        () -> {
          Context context = makeConfigWithFlags(requiredFlagsCli);
          assertEquals("./gradlew compileJava", context.buildCommand);
          assertEquals(testDir, context.globalDir);
          assertEquals("edu.ucr.Initializer", context.initializerAnnot);
          assertEquals(Paths.get("0nullaway.xml"), context.target.nullawayConfig);
          assertEquals(Paths.get("0scanner.xml"), context.target.scannerConfig);
        });
  }

  @Test
  public void testRequiredFlagsMissingForDownstreamDependencyAnalysisCli() {
    String expectedErrorMessage =
        "To activate downstream dependency analysis, all flags [--activate-downstream-dependencies-analysis, --downstream-dependencies-build-command (arg), --nullaway-library-model-loader-path (arg)] must be present!";
    // Check if each is missed.
    for (int i = 0; i < requiredDownsStreamDependencyFlagsCli.size(); i++) {
      List<CLIFlag> incompleteFlags = new ArrayList<>(requiredDownsStreamDependencyFlagsCli);
      incompleteFlags.remove(i);
      List<CLIFlag> flags = new ArrayList<>(requiredFlagsCli);
      flags.addAll(incompleteFlags);
      IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> makeConfigWithFlags(flags));
      // Check the error message.
      assertTrue(ex.getMessage().contains(expectedErrorMessage));
    }
  }

  @Test
  public void testRequiredFlagsForDownstreamDependencyAnalysisCli() {
    runTestWithMockedBuild(
        testDir,
        () -> {
          List<CLIFlag> flags = new ArrayList<>(requiredFlagsCli);
          flags.addAll(requiredDownsStreamDependencyFlagsCli);
          Context context = makeConfigWithFlags(flags);
          assertEquals("./gradlew compileJava", context.buildCommand);
          assertEquals(testDir, context.globalDir);
          assertEquals("edu.ucr.Initializer", context.initializerAnnot);
          assertEquals(Paths.get("0nullaway.xml"), context.target.nullawayConfig);
          assertEquals(Paths.get("0scanner.xml"), context.target.scannerConfig);
          assertEquals(
              testDir.resolve("library-model.tsv"), context.nullawayLibraryModelLoaderPath);
          assertTrue(context.downStreamDependenciesAnalysisActivated);
          assertEquals("./gradlew :dep:compileJava", context.downstreamDependenciesBuildCommand);
          // Compute expected downstream context paths for nullaway and scanner context file paths
          // for
          // downstream dependencies.
          ImmutableSet<Pair<Path, Path>> expectedDownstreamConfigPaths =
              IntStream.range(1, 5)
                  .mapToObj(
                      i -> new Pair<>(Paths.get(i + "nullaway.xml"), Paths.get(i + "scanner.xml")))
                  .collect(ImmutableSet.toImmutableSet());
          // Retrieve actual downstream context paths for nullaway and scanner context file paths
          // for
          // downstream dependencies.
          ImmutableSet<Pair<Path, Path>> actualDownstreamConfigPaths =
              context.downstreamInfo.stream()
                  .map(
                      moduleInfo -> new Pair<>(moduleInfo.nullawayConfig, moduleInfo.scannerConfig))
                  .collect(ImmutableSet.toImmutableSet());
          assertEquals(actualDownstreamConfigPaths, expectedDownstreamConfigPaths);
        });
  }

  @Test
  public void testConfigFilesHaveDifferentUUID() {
    Set<String> observed = new HashSet<>();
    // Test for NullAway context
    FixSerializationConfig config = new FixSerializationConfig();
    Path nullawayConfigPath = testDir.resolve("nullaway.xml");
    for (int i = 0; i < 5; i++) {
      Utility.writeNullAwayConfigInXMLFormat(config, nullawayConfigPath.toString());
      String uuid = getValueFromTag(nullawayConfigPath, "/serialization/uuid");
      if (observed.contains(uuid)) {
        throw new IllegalStateException(
            "Duplicate UUID found for NullAway context: " + uuid + " in set: " + observed);
      }
      observed.add(uuid);
    }
    observed.clear();
    // Test for Scanner context
    Path scannerConfig = testDir.resolve("scanner.xml");
    for (int i = 0; i < 5; i++) {
      ScannerConfigWriter writer = new ScannerConfigWriter();
      writer.setOutput(testDir).writeAsXML(scannerConfig);
      String uuid = getValueFromTag(scannerConfig, "/scanner/uuid");
      if (observed.contains(uuid)) {
        throw new IllegalStateException(
            "Duplicate UUID found for Scanner context: " + uuid + " in set: " + observed);
      }
      observed.add(uuid);
    }
  }

  @Test
  public void testAnalysisModeFlags() {
    runTestWithMockedBuild(
        testDir,
        () -> {
          Context context;
          // Check mode downstream dependency off.
          context = makeConfigWithFlags(requiredFlagsCli);
          assertEquals(AnalysisMode.LOCAL, context.mode);

          List<CLIFlag> baseFlags = new ArrayList<>(requiredFlagsCli);
          baseFlags.addAll(requiredDownsStreamDependencyFlagsCli);

          // Check default mode downstream dependency on.
          context = makeConfigWithFlags(baseFlags);
          assertEquals(AnalysisMode.LOWER_BOUND, context.mode);

          Map<String, AnalysisMode> modes =
              Map.of(
                  "upper_bound",
                  AnalysisMode.UPPER_BOUND,
                  "lower_bound",
                  AnalysisMode.LOWER_BOUND,
                  "default",
                  AnalysisMode.LOWER_BOUND,
                  "strict",
                  AnalysisMode.STRICT);

          modes.forEach(
              (flagValue, expectedMode) -> {
                CLIFlag flag = new CLIFlagWithValue("am", flagValue);
                ArrayList<CLIFlag> flags = new ArrayList<>(baseFlags);
                flags.add(flag);
                Context c = makeConfigWithFlags(flags);
                assertEquals(expectedMode, c.mode);
              });
        });
  }

  @Test
  public void testForceResolveFlag() {
    runTestWithMockedBuild(
        testDir,
        () -> {
          Context context;

          List<CLIFlag> baseFlags = new ArrayList<>(requiredFlagsCli);
          baseFlags.addAll(requiredDownsStreamDependencyFlagsCli);

          // Check default mode.
          context = makeConfigWithFlags(baseFlags);
          assertFalse(context.forceResolveActivated);

          CLIFlag flag = new CLIFlagWithValue("fr", "edu.ucr.example.NullUnmarked");
          baseFlags.add(flag);
          context = makeConfigWithFlags(baseFlags);
          assertTrue(context.forceResolveActivated);
          assertEquals(context.nullUnMarkedAnnotation, "edu.ucr.example.NullUnmarked");
        });
  }

  /**
   * Helper method for creating a {@link Context} object with the given flags. Before creating the
   * context file, it cleans up the existing module output directories.
   *
   * @param flags Flags to create the context object.
   * @return Context instance.
   */
  private Context makeConfigWithFlags(List<CLIFlag> flags) {
    IntStream.of(0, 5)
        .forEach(
            id -> {
              try {
                Path path = testDir.resolve(String.valueOf(id));
                if (!path.toFile().exists()) {
                  return;
                }
                FileUtils.cleanDirectory(path.toFile());
                FileUtils.deleteDirectory(path.toFile());
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
    return new Context(flags.stream().flatMap(CLIFlag::toStream).toArray(String[]::new));
  }

  /**
   * Helper method for reading value of a node located at /key_1/key_2/.../key_n (in the form of
   * {@code Xpath} query) from a xml document at the given path.
   *
   * @param path Path to xml file.
   * @param key Key to locate the value, can be nested in the form of {@code Xpath} query (e.g.
   *     /key1/key2/.../key_n).
   * @return The value in the specified keychain as {@code String}.
   */
  private static String getValueFromTag(Path path, String key) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = builder.parse(Files.newInputStream(path));
      doc.normalize();
      XPath xPath = XPathFactory.newInstance().newXPath();
      return ((Node) xPath.compile(key).evaluate(doc, XPathConstants.NODE)).getTextContent();
    } catch (XPathExpressionException
        | ParserConfigurationException
        | IOException
        | SAXException ex) {
      throw new RuntimeException("Could not extract value from tag: " + key, ex);
    }
  }

  /** Container class for CLI Flag. */
  private static class CLIFlag {
    /** Flag name; */
    protected final String flag;

    public CLIFlag(String flag) {
      this.flag = flag;
    }

    public Stream<String> toStream() {
      return Stream.of("-" + flag);
    }
  }

  /** Container class for CLI Flag with value. */
  private static class CLIFlagWithValue extends CLIFlag {
    /** Flag Value. */
    private final Object value;

    public CLIFlagWithValue(String flag, Object value) {
      super(flag);
      this.value = value;
    }

    @Override
    public Stream<String> toStream() {
      return Stream.of("-" + flag, value.toString());
    }
  }
}
