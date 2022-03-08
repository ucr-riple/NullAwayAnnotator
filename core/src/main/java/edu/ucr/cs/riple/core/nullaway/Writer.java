package edu.ucr.cs.riple.core.nullaway;

public class Writer {
  public static final String ERROR = "/tmp/NullAwayFix/errors.tsv";
  public static final String METHOD_INFO = "/tmp/NullAwayFix/method_info.tsv";
  public static final String CALL_GRAPH = "/tmp/NullAwayFix/call_graph.tsv";
  public static final String SUGGEST_FIX = "/tmp/NullAwayFix/fixes.tsv";
  public static final String FIELD_GRAPH = "/tmp/NullAwayFix/field_graph.tsv";
  public static final String DELIMITER = "\t";

  public static String getDelimiterRegex() {
    return "\t";
  }
}
