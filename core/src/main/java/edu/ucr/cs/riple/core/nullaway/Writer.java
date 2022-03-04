package edu.ucr.cs.riple.core.nullaway;

public class Writer {
  public static final String ERROR = "/tmp/NullAwayFix/errors.csv";
  public static final String METHOD_INFO = "/tmp/NullAwayFix/method_info.csv";
  public static final String CALL_GRAPH = "/tmp/NullAwayFix/call_graph.csv";
  public static final String SUGGEST_FIX = "/tmp/NullAwayFix/fixes.csv";
  public static final String FIELD_GRAPH = "/tmp/NullAwayFix/field_graph.csv";
  public static final String DELIMITER = "$*$";

  public static String getDelimiterRegex() {
    StringBuilder ans = new StringBuilder("(");
    for (int i = 0; i < DELIMITER.length(); i++) {
      ans.append("\\").append(DELIMITER.charAt(i));
    }
    ans.append(")");
    return ans.toString();
  }
}
