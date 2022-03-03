package edu.ucr.cs.css;

import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.Tree;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Writer {
  public final Path ERROR;
  public final Path METHOD_INFO;
  public final Path CALL_GRAPH;
  public final Path SUGGEST_FIX;
  public final Path FIELD_GRAPH;
  public final String DELIMITER = "$*$";

  public Writer(Config config) {
    String outputDirectory = config.outputDirectory;
    this.ERROR = Paths.get(outputDirectory, "errors.csv");
    this.METHOD_INFO = Paths.get(outputDirectory, "method_info.csv");
    this.CALL_GRAPH = Paths.get(outputDirectory, "call_graph.csv");
    this.SUGGEST_FIX = Paths.get(outputDirectory, "fixes.csv");
    this.FIELD_GRAPH = Paths.get(outputDirectory, "field_graph.csv");
    reset(config);
  }

  public void saveFieldTrackerNode(Tree tree, VisitorState state) {
    TrackerNode node = new TrackerNode(ASTHelpers.getSymbol(tree), state.getPath());
    appendToFile(node, FIELD_GRAPH);
  }

  public void saveMethodTrackerNode(Tree tree, VisitorState state) {
    TrackerNode node = new TrackerNode(ASTHelpers.getSymbol(tree), state.getPath());
    appendToFile(node, CALL_GRAPH);
  }

  private void resetFile(Path path, String header) {
    try {
      Files.deleteIfExists(path);
      OutputStream os = new FileOutputStream(path.toFile());
      header += "\n";
      os.write(header.getBytes(Charset.defaultCharset()), 0, header.length());
      os.flush();
      os.close();
    } catch (IOException e) {
      throw new RuntimeException(
          "Could not finish resetting File at Path: " + path + ", Exception: " + e);
    }
  }

  private void reset(Config config) {
    try {
      Files.createDirectories(Paths.get(config.outputDirectory));
      if (config.methodTrackerIsActive) {
        resetFile(CALL_GRAPH, TrackerNode.header(DELIMITER));
      }
      if (config.fieldTrackerIsActive) {
        resetFile(FIELD_GRAPH, TrackerNode.header(DELIMITER));
      }
    } catch (IOException e) {
      throw new RuntimeException("Could not finish resetting writer: " + e);
    }
  }

  private void appendToFile(SeperatedValueDisplay value, Path path) {
    OutputStream os;
    String display = value.display(DELIMITER);
    if (display == null || display.equals("")) {
      return;
    }
    display = display.replaceAll("\\R+", " ").replaceAll("\t", "") + "\n";
    try {
      os = new FileOutputStream(path.toFile(), true);
      os.write(display.getBytes(Charset.defaultCharset()), 0, display.length());
      os.flush();
      os.close();
    } catch (Exception e) {
      System.err.println("Error happened for writing at file: " + path);
    }
  }
}
