package edu.ucr.cs.riple.scanner;

import static org.powermock.api.mockito.PowerMockito.when;

import com.sun.source.tree.CompilationUnitTree;
import edu.ucr.cs.riple.scanner.tools.MethodInfoDisplay;
import edu.ucr.cs.riple.scanner.tools.SerializationTestHelper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class BuildSystemSpecificTest {

  private Path root;
  private Path configPath;

  @Before
  public void setup() {
    try {
      root = Files.createTempDirectory("temp");
      configPath = root.resolve("scanner.xml");
      Files.createDirectories(root);
      ErrorProneCLIFlagsConfig.Builder builder =
          new ErrorProneCLIFlagsConfig.Builder()
              .setCallTrackerActivation(true)
              .setClassTrackerActivation(true)
              .setFieldTrackerActivation(true)
              .setMethodTrackerActivation(true)
              .setOutput(root);
      Files.createFile(configPath);
      builder.writeAsXML(configPath);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Test
  public void testJarFilesNoURL() {
    try (AutoCloseable closeable = MockitoAnnotations.openMocks(CompilationUnitTree.class)) {
      CompilationUnitTree tree = PowerMockito.mock(CompilationUnitTree.class);
      when(tree.getSourceFile()).thenReturn(null);
    } catch (Exception e) {

    }
    SerializationTestHelper<MethodInfoDisplay> tester =
        new SerializationTestHelper<MethodInfoDisplay>(root)
            .setArgs(
                Arrays.asList(
                    "-d",
                    root.toString(),
                    "-Xep:TypeAnnotatorScanner:ERROR",
                    "-XepOpt:TypeAnnotatorScanner:ConfigPath=" + configPath.toString()))
            .setOutputFileNameAndHeader(MethodInfoTest.FILE_NAME, MethodInfoTest.HEADER)
            .setFactory(MethodInfoTest.METHOD_DISPLAY_FACTORY);
  }
}
