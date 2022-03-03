package edu.ucr.cs.riple.core.util;

import edu.ucr.cs.riple.core.AutoFixer;
import com.google.common.primitives.Booleans;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.metadata.method.MethodInheritanceTree;
import edu.ucr.cs.riple.core.metadata.method.MethodNode;
import edu.ucr.cs.riple.core.nullaway.Writer;
import edu.ucr.cs.riple.injector.Fix;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
  public static void writeReports(List<Report> reports) {
    JSONObject result = new JSONObject();
    JSONArray reportsJson = new JSONArray();
    for (Report report : reports) {
      JSONObject reportJson = report.fix.getJson();
      reportJson.put("jump", report.effectiveNess);
      reportJson.put("finished", report.finished);
      JSONArray followUps = new JSONArray();
      if (report.effectiveNess < 1) {
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
        br.readLine();
        while ((line = br.readLine()) != null) {
          Fix fix = Fix.fromCSVLine(line, delimiter);
          Optional<Fix> existing = fixes.stream().filter(other -> other.equals(fix)).findAny();
          if (existing.isPresent()) {
            existing.get().referred++;
          } else {
            fix.referred = 1;
            fixes.add(fix);
          }
          fixes.add(Fix.fromCSVLine(line, delimiter));
        }
      }
    } catch (IOException e) {
      System.err.println("Exception happened in initializing MethodParamExplorer...");
    }
    return fixes;
  }

  public static int calculateInheritanceViolationError(MethodInheritanceTree mit, Fix fix) {
    int index = Integer.parseInt(fix.index);
    MethodNode methodNode = mit.findNode(fix.method, fix.className);
    if (methodNode == null) {
      return 0;
    }
    boolean[] thisMethodFlag = methodNode.annotFlags;
    if (index >= thisMethodFlag.length) {
      return 0;
    }
    int effect = 0;
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
      System.out.println("Found no report at: " + out_dir + "/reports.json");
      return;
    }
    try {
      System.out.println("Reading cached fixes reports");
      JSONObject cachedObjects =
          (JSONObject) new JSONParser().parse(new FileReader(out_dir + "/reports.json"));
      JSONArray cachedJson = (JSONArray) cachedObjects.get("reports");
      System.out.println("Found " + cachedJson.size() + " number of reports");
      List<Fix> cached = new ArrayList<>();
      for (Object o : cachedJson) {
        JSONObject reportJson = (JSONObject) o;
        if (Integer.parseInt(reportJson.get("jump").toString()) > 0) {
          cached.add(Fix.createFromJson(reportJson));
        }
      }
      System.out.println(
          "Cached items size: " + cached.size() + " total fix size: " + fixes.size());
      fixes.removeAll(cached);
    } catch (Exception exception) {
      System.out.println("EXCEPTION: " + exception);
    }
    System.out.println("Processing cache fixes finished. Reduced down to: " + fixes.size());
  }

  public static List<Fix> readFixesJson(String filePath) {
    List<Fix> fixes = new ArrayList<>();
    try {
      BufferedReader bufferedReader =
          Files.newBufferedReader(Paths.get(filePath), Charset.defaultCharset());
      JSONObject obj = (JSONObject) new JSONParser().parse(bufferedReader);
      JSONArray fixesJson = (JSONArray) obj.get("fixes");
      bufferedReader.close();
      for (Object o : fixesJson) {
        JSONObject fixJson = (JSONObject) o;
        fixes.add(Fix.createFromJson(fixJson));
        if (fixJson.containsKey("followups")) {
          JSONArray followUps = (JSONArray) fixJson.get("followups");
          for (Object followup : followUps) {
            fixes.add(Fix.createFromJson((JSONObject) followup));
          }
        }
      }
    } catch (FileNotFoundException ex) {
      throw new RuntimeException("Unable to open file: " + filePath);
    } catch (IOException ex) {
      throw new RuntimeException("Error reading file: " + filePath);
    } catch (ParseException e) {
      throw new RuntimeException("Error in parsing object: " + e);
    }
    return fixes;
  }

  public static ProgressBar createProgressBar(String taskName, int steps) {
    return new ProgressBar(
        taskName,
        steps,
        1000,
        System.out,
        ProgressBarStyle.ASCII,
        "",
        1,
        false,
        null,
        ChronoUnit.SECONDS,
        0L,
        Duration.ZERO);
  }

  public static void writeLog(AutoFixer.Log log) {
    String path = "/tmp/NullAwayFix/log.txt";
    try {
      FileWriter fw = new FileWriter(path, true);
      BufferedWriter bw = new BufferedWriter(fw);
      bw.write(log.toString());
      bw.newLine();
      bw.close();
    } catch (Exception ignored) {
    }
  }
}
