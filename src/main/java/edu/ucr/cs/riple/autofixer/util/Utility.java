package edu.ucr.cs.riple.autofixer.util;

import com.google.common.primitives.Booleans;
import com.uber.nullaway.autofix.Writer;
import edu.ucr.cs.riple.autofixer.Report;
import edu.ucr.cs.riple.injector.Fix;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Utility {

  @SuppressWarnings("StatementWithEmptyBody")
  public static void executeCommand(String command) {
    try {
      Process p = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", command});
      BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
      while ((reader.readLine()) != null) {}
      p.waitFor();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @SuppressWarnings("ALL")
  public static void writeReports(List<Report> finishedReports) {
    JSONObject result = new JSONObject();
    JSONArray reportsJson = new JSONArray();
    for (Report report : finishedReports) {
      JSONObject reportJson = report.fix.getJson();
      reportJson.put("jump", report.effectiveNess);
      JSONArray followUps = new JSONArray();
      followUps.addAll(report.chain);
      reportJson.put("followups", followUps);
      reportsJson.add(reportJson);
    }
    reportsJson.sort(
        (o1, o2) -> {
          Integer first = (Integer) ((JSONObject) o1).get("jump");
          Integer second = (Integer) ((JSONObject) o2).get("jump");
          if (first.equals(second)) {
            return 0;
          }
          if (first < second) {
            return 1;
          }
          return -1;
        });
    result.put("reports", reportsJson);
    try {
      FileWriter writer = new FileWriter("/tmp/NullAwayFix/diagnose_report.json");
      writer.write(result.toJSONString().replace("\\/", "/").replace("\\\\\\", "\\"));
      writer.flush();
    } catch (IOException e) {
      throw new RuntimeException("Could not create the autoFixer report json file");
    }
  }

  @SuppressWarnings("ALL")
  public static void convertCSVToJSON(String csvPath, String jsonPath) {
    JSONArray fixes = new JSONArray();
    BufferedReader reader;
    FileWriter writer;
    try {
      reader = Files.newBufferedReader(Paths.get(csvPath), Charset.defaultCharset());
      String line = reader.readLine();
      if (line != null) line = reader.readLine();
      while (line != null) {
        Fix fix = Fix.fromCSVLine(line, Writer.getDelimiterRegex());
        fixes.add(fix.getJson());
        line = reader.readLine();
      }
      reader.close();
      JSONObject res = new JSONObject();
      JSONArray fixesArray = new JSONArray();
      fixesArray.addAll(fixes);
      res.put("fixes", fixesArray);
      writer = new FileWriter(jsonPath);
      writer.write(res.toJSONString().replace("\\/", "/").replace("\\\\\\", "\\"));
      writer.flush();
    } catch (IOException e) {
      System.err.println("Error happened in converting csv to json!");
    }
  }

  public static boolean[] convertStringToBooleanArray(String content) {
    if (content == null) {
      return new boolean[0];
    }
    content = content.substring(1, content.length() - 1);
    if (content.length() == 0) {
      return new boolean[0];
    }
    content = content.replaceAll("\\s", "");
    return Booleans.toArray(
        Arrays.stream(content.split(",")).map(Boolean::parseBoolean).collect(Collectors.toList()));
  }

  public static boolean isEqual(Fix fix, Fix other) {
    return fix.className.equals(other.className)
        && fix.method.equals(other.method)
        && fix.index.equals(other.index)
        && fix.param.equals(other.param);
  }
}
