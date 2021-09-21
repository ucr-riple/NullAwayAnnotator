package edu.ucr.cs.riple.autofixer.util;

import com.google.common.primitives.Booleans;
import edu.ucr.cs.riple.autofixer.Report;
import edu.ucr.cs.riple.autofixer.metadata.method.MethodInheritanceTree;
import edu.ucr.cs.riple.autofixer.metadata.method.MethodNode;
import edu.ucr.cs.riple.autofixer.nullaway.Writer;
import edu.ucr.cs.riple.injector.Fix;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

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
      if (report.effectiveNess < 0) {
        report.followups.remove(report.fix);
        followUps.addAll(
            report.followups.stream().map(fix -> fix.getJson()).collect(Collectors.toList()));
      }
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

  public static List<Fix> readAllFixes() {
    List<Fix> fixes = new ArrayList<>();
    try {
      try (BufferedReader br = new BufferedReader(new FileReader(Writer.SUGGEST_FIX))) {
        String line;
        String delimiter = Writer.getDelimiterRegex();
        while ((line = br.readLine()) != null) {
          fixes.add(Fix.fromCSVLine(line, delimiter));
        }
      }
    } catch (IOException e) {
      System.err.println("Exception happened in initializing MethodParamExplorer...");
    }
    return fixes;
  }

  public static boolean isEqual(Fix fix, Fix other) {
    return fix.className.equals(other.className)
        && fix.method.equals(other.method)
        && fix.index.equals(other.index)
        && fix.param.equals(other.param);
  }

  public static int calculateInheritanceViolationError(MethodInheritanceTree mit, Fix fix) {
    int index = Integer.parseInt(fix.index);
    int effect = 0;
    boolean[] thisMethodFlag = mit.findNode(fix.method, fix.className).annotFlags;
    if (index >= thisMethodFlag.length) {
      return 0;
    }
    for (MethodNode subMethod : mit.getSubMethods(fix.method, fix.className, false)) {
      if (!thisMethodFlag[index]) {
        if (!subMethod.annotFlags[index]) {
          effect++;
        }
      }
    }
    List<MethodNode> superMethods = mit.getSuperMethods(fix.method, fix.className, false);
    if (superMethods.size() != 0) {
      MethodNode superMethod = superMethods.get(0);
      if (!thisMethodFlag[index]) {
        if (superMethod.annotFlags[index]) {
          effect--;
        }
      }
    }
    return effect;
  }

  public static void removeCachedFixes(List<Fix> fixes, String out_dir) {
    if (!Files.exists(Paths.get(out_dir + "/reports.json"))) {
      return;
    }
    try {
      System.out.println("Reading cached fixes reports");
      JSONObject cachedObjects =
          (JSONObject) new JSONParser().parse(new FileReader(out_dir + "/reports.json"));
      JSONArray cachedJson = (JSONArray) cachedObjects.get("fixes");
      List<Report> cached = new ArrayList<>();
      for (Object o : cachedJson) {
        JSONObject reportJson = (JSONObject) o;
        int effect = Integer.parseInt(reportJson.get("effect").toString());
        if (effect < 1) {
          cached.add(new Report(Fix.createFromJson(reportJson), effect));
        }
      }
      fixes.removeAll(cached.stream().map(report -> report.fix).collect(Collectors.toList()));
    } catch (Exception ignored) {
    }
    System.out.println("Processing cache fixes finished.");
  }
}
