package edu.ucr.cs.riple.autofixer.nullaway;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Writer {
  public static final String ERROR = "/tmp/NullAwayFix/errors.csv";
  public static final String METHOD_INFO = "/tmp/NullAwayFix/method_info.csv";
  public static final String CALL_GRAPH = "/tmp/NullAwayFix/call_graph.csv";
  public static final String SUGGEST_FIX = "/tmp/NullAwayFix/fixes.csv";
  public static final String DELIMITER = "$*$";

  public static String getDelimiterRegex() {
    StringBuilder ans = new StringBuilder("(");
    for (int i = 0; i < DELIMITER.length(); i++) {
      ans.append("\\").append(DELIMITER.charAt(i));
    }
    ans.append(")");
    return ans.toString();
  }

  public static void reset(AutoFixConfig config) {
    try {
      Files.createDirectories(Paths.get("/tmp/NullAwayFix/"));
      if (config.SUGGEST_ENABLED) {
        Files.deleteIfExists(Paths.get(SUGGEST_FIX));
      }
      if (config.LOG_ERROR_ENABLED) {
        Files.deleteIfExists(Paths.get(ERROR));
      }
      if (config.MAKE_METHOD_TREE_INHERITANCE_ENABLED) {
        Files.deleteIfExists(Paths.get(METHOD_INFO));
      }
      if (config.MAKE_CALL_GRAPH_ENABLED) {
        Files.deleteIfExists(Paths.get(CALL_GRAPH));
      }
    } catch (IOException e) {
      throw new RuntimeException("Could not finish resetting writer: " + e);
    }
  }
}
