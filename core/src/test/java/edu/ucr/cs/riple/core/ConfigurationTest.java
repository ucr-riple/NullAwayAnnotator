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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.util.FixSerializationConfig;
import edu.ucr.cs.riple.core.util.Utility;
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
    // Make dummy config paths for 5 targets
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
    requiredFlagsCli =
        new ArrayList<>(
            List.of(
                new CLIFlagWithValue("bc", "./gradlew compileJava"),
                new CLIFlagWithValue("cp", testDir.resolve("paths.tsv")),
                new CLIFlagWithValue("i", "edu.ucr.Initializer"),
                new CLIFlagWithValue("d", testDir)));
    requiredDownsStreamDependencyFlagsCli =
        new ArrayList<>(
            List.of(
                new CLIFlag("adda"),
                new CLIFlagWithValue("nlmlp", testDir.resolve("library-model.tsv")),
                new CLIFlagWithValue("ddbc", "./gradlew :dep:compileJava")));
  }

  /**
   * Converts list of {@link CLIFlag} to array of string where each entry is flag name followed by
   * its value.
   *
   * @param flags List of flags.
   * @return Array of string consisting flags name and values.
   */
  private static String[] makeCommandLineArguments(List<CLIFlag> flags) {
    return flags.stream().flatMap(CLIFlag::toStream).toArray(String[]::new);
  }

  @Test
  public void testRequiredFlagsMissingCli() {
    // Check if each is missed.
    for (int i = 0; i < requiredFlagsCli.size(); i++) {
      List<CLIFlag> incompleteFlags = new ArrayList<>(requiredFlagsCli);
      CLIFlag missingFlag = incompleteFlags.remove(i);
      String expectedErrorMessage = "Missing required option: " + missingFlag.flag;
      IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> new Config(makeCommandLineArguments(incompleteFlags)));
      // Check the error message.
      assertTrue(ex.getMessage().contains(expectedErrorMessage));
    }
  }

  @Test
  public void testRequiredFlagsCli() {
    Config config = new Config(makeCommandLineArguments(requiredFlagsCli));
    assertEquals("./gradlew compileJava", config.buildCommand);
    assertEquals(testDir, config.globalDir);
    assertEquals("edu.ucr.Initializer", config.initializerAnnot);
    assertEquals(
        new ModuleInfo(0, testDir, Paths.get("0nullaway.xml"), Paths.get("0scanner.xml")),
        config.target);
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
          assertThrows(
              IllegalArgumentException.class, () -> new Config(makeCommandLineArguments(flags)));
      // Check the error message.
      assertTrue(ex.getMessage().contains(expectedErrorMessage));
    }
  }

  @Test
  public void testRequiredFlagsForDownstreamDependencyAnalysisCli() {
    List<CLIFlag> flags = new ArrayList<>(requiredFlagsCli);
    flags.addAll(requiredDownsStreamDependencyFlagsCli);
    Config config = new Config(makeCommandLineArguments(flags));
    assertEquals("./gradlew compileJava", config.buildCommand);
    assertEquals(testDir, config.globalDir);
    assertEquals("edu.ucr.Initializer", config.initializerAnnot);
    assertEquals(
        new ModuleInfo(0, testDir, Paths.get("0nullaway.xml"), Paths.get("0scanner.xml")),
        config.target);
    assertEquals(testDir.resolve("library-model.tsv"), config.nullawayLibraryModelLoaderPath);
    assertTrue(config.downStreamDependenciesAnalysisActivated);
    assertEquals("./gradlew :dep:compileJava", config.downstreamDependenciesBuildCommand);
    ImmutableSet<ModuleInfo> downstreamInfo =
        IntStream.range(1, 5)
            .mapToObj(
                i ->
                    new ModuleInfo(
                        i, testDir, Paths.get(i + "nullaway.xml"), Paths.get(i + "scanner.xml")))
            .collect(ImmutableSet.toImmutableSet());
    assertEquals(downstreamInfo, config.downstreamInfo);
  }

  @Test
  public void testConfigFilesHaveDifferentUUID() {
    Set<String> observed = new HashSet<>();
    // Test for NullAway config
    FixSerializationConfig config = new FixSerializationConfig();
    Path nullawayConfigPath = testDir.resolve("nullaway.xml");
    for (int i = 0; i < 5; i++) {
      Utility.writeNullAwayConfigInXMLFormat(config, nullawayConfigPath.toString());
      String uuid = getValueFromTag(nullawayConfigPath, "/serialization/uuid");
      if (observed.contains(uuid)) {
        throw new IllegalStateException(
            "Duplicate UUID found for NullAway config: " + uuid + " in set: " + observed);
      }
      observed.add(uuid);
    }
    observed.clear();
    // Test for Scanner config
    Path scannerConfig = testDir.resolve("scanner.xml");
    ModuleInfo moduleInfo = new ModuleInfo(0, testDir, nullawayConfigPath, scannerConfig);
    for (int i = 0; i < 5; i++) {
      Utility.setScannerCheckerActivation(moduleInfo, true);
      String uuid = getValueFromTag(scannerConfig, "/scanner/uuid");
      if (observed.contains(uuid)) {
        throw new IllegalStateException(
            "Duplicate UUID found for Scanner config: " + uuid + " in set: " + observed);
      }
      observed.add(uuid);
    }
  }

  @Test
  public void testAnalysisModeFlags() {
    Config config;
    // Check mode downstream dependency off.
    config = new Config(makeCommandLineArguments(requiredFlagsCli));
    assertEquals(AnalysisMode.LOCAL, config.mode);

    List<CLIFlag> baseFlags = new ArrayList<>(requiredFlagsCli);
    baseFlags.addAll(requiredDownsStreamDependencyFlagsCli);

    // Check default mode downstream dependency on.
    config = new Config(makeCommandLineArguments(baseFlags));
    assertEquals(AnalysisMode.LOWER_BOUND, config.mode);

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
          Config c = new Config(makeCommandLineArguments(flags));
          assertEquals(expectedMode, c.mode);
        });
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
