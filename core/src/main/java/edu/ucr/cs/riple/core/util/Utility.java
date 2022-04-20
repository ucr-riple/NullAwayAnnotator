/*
 * MIT License
 *
 * Copyright (c) 2020 Nima Karimipour
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

package edu.ucr.cs.riple.core.util;

import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Booleans;
import edu.ucr.cs.riple.core.Annotator;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.FixType;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.metadata.method.MethodInheritanceTree;
import edu.ucr.cs.riple.core.metadata.method.MethodNode;
import edu.ucr.cs.riple.injector.Location;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
      throw new RuntimeException("Exception happened in executing command: " + command, e);
    }
  }

  @SuppressWarnings("ALL")
  public static void writeReports(Config config, ImmutableSet<Report> reports) {
    Path reportsPath = config.dir.resolve("diagnose_report.json");
    JSONObject result = new JSONObject();
    JSONArray reportsJson = new JSONArray();
    for (Report report : reports) {
      JSONObject reportJson = report.root.getJson();
      reportJson.put("effect", report.effectiveNess);
      reportJson.put("finished", report.finished);
      JSONArray followUps = new JSONArray();
      if (config.chain && report.effectiveNess < 1) {
        report.tree.remove(report.root);
        followUps.addAll(
            report.tree.stream().map(fix -> fix.getJson()).collect(Collectors.toList()));
      }
      reportJson.put("followups", followUps);
      reportsJson.add(reportJson);
    }
    reportsJson.sort(
        (o1, o2) -> {
          int first = (Integer) ((JSONObject) o1).get("effect");
          int second = (Integer) ((JSONObject) o2).get("effect");
          if (first == second) {
            return 0;
          }
          if (first < second) {
            return 1;
          }
          return -1;
        });
    result.put("reports", reportsJson);
    try {
      FileWriter writer = new FileWriter(reportsPath.toFile());
      writer.write(result.toJSONString().replace("\\/", "/").replace("\\\\\\", "\\"));
      writer.flush();
    } catch (IOException e) {
      throw new RuntimeException(
          "Could not create the Annotator report json file: " + reportsPath, e);
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

  public static String[] convertStringToStringArray(String content) {
    if (content == null) {
      return new String[0];
    }
    content = content.substring(1, content.length() - 1);
    if (content.length() == 0) {
      return new String[0];
    }
    content = content.replaceAll("\\s", "");
    return Arrays.stream(content.split(",")).toArray(String[]::new);
  }

  public static ImmutableSet<Location> readAllFixes(Path path) {
    Set<Location> fixes = new HashSet<>();
    try {
      try (BufferedReader br = new BufferedReader(new FileReader(path.toFile()))) {
        String line;
        br.readLine();
        while ((line = br.readLine()) != null) {
          Location fix = Location.fromCSVLine(line, "\t");
          Optional<Location> existing = fixes.stream().filter(other -> other.equals(fix)).findAny();
          if (existing.isPresent()) {
            existing.get().referred++;
          } else {
            fix.referred = 1;
            fixes.add(fix);
          }
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Exception happened in reading fixes at: " + path, e);
    }
    return ImmutableSet.copyOf(fixes);
  }

  public static void removeCachedFixes(ImmutableSet<Location> fixes, Config config) {
    Path reportsPath = config.dir.resolve("reports.json");
    if (!Files.exists(reportsPath)) {
      return;
    }
    try {
      JSONObject cachedObjects =
          (JSONObject) new JSONParser().parse(new FileReader(reportsPath.toFile()));
      JSONArray cachedJson = (JSONArray) cachedObjects.get("reports");
      List<Location> cached = new ArrayList<>();
      for (Object o : cachedJson) {
        JSONObject reportJson = (JSONObject) o;
        if (Integer.parseInt(reportJson.get("effect").toString()) > 0) {
          cached.add(Location.createFromJson(reportJson));
        }
      }
      System.out.print(
          "Cached items size: " + cached.size() + " total location size: " + fixes.size());
      fixes.removeAll(cached);
    } catch (Exception exception) {
      throw new RuntimeException("Exception happened in removing cached fixes", exception);
    }
    System.out.println(". Reduced down to: " + fixes.size());
  }

  public static List<Location> readFixesJson(Path filePath) {
    List<Location> fixes = new ArrayList<>();
    try {
      BufferedReader bufferedReader = Files.newBufferedReader(filePath, Charset.defaultCharset());
      JSONObject obj = (JSONObject) new JSONParser().parse(bufferedReader);
      JSONArray fixesJson = (JSONArray) obj.get("fixes");
      bufferedReader.close();
      for (Object o : fixesJson) {
        JSONObject fixJson = (JSONObject) o;
        fixes.add(Location.createFromJson(fixJson));
        if (fixJson.containsKey("followups")) {
          JSONArray followUps = (JSONArray) fixJson.get("followups");
          for (Object followup : followUps) {
            fixes.add(Location.createFromJson((JSONObject) followup));
          }
        }
      }
    } catch (FileNotFoundException e) {
      throw new RuntimeException("Unable to open file: " + filePath, e);
    } catch (IOException e) {
      throw new RuntimeException("Error reading file: " + filePath, e);
    } catch (ParseException e) {
      throw new RuntimeException("Error in parsing object", e);
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

  public static void writeLog(Config config) {
    try {
      FileWriter fw = new FileWriter(config.dir.resolve("log.txt").toFile(), true);
      BufferedWriter bw = new BufferedWriter(fw);
      bw.write(Annotator.log.toString());
      bw.newLine();
      bw.close();
    } catch (Exception ignored) {
    }
  }

  public static int calculateMethodInheritanceViolationError(
      MethodInheritanceTree tree, Location fix, List<Location> fixesInOneRound) {
    MethodNode superMethod = tree.getClosestSuperMethod(fix.method, fix.clazz);
    if (superMethod == null) {
      return 0;
    }
    if (superMethod.hasNullableAnnotation) {
      return 0;
    }
    if (fixesInOneRound
        .stream()
        .anyMatch(
            fix1 ->
                fix1.method.equals(superMethod.method)
                    && fix1.clazz.equals(superMethod.clazz)
                    && fix1.location.equals(FixType.METHOD.name))) {
      return 1;
    }
    return 0;
  }

  public static boolean correspondingFixExists(
      String clazz, String method, String param, String location, List<Location> fixes) {
    return fixes
        .stream()
        .anyMatch(
            fix ->
                fix.location.equals(location)
                    && fix.clazz.equals(clazz)
                    && fix.method.equals(method)
                    && fix.variable.equals(param));
  }

  public static int calculateParamInheritanceViolationError(
      MethodInheritanceTree mit, Location fix, List<Location> fixesInOneRound) {
    int index = Integer.parseInt(fix.index);
    MethodNode methodNode = mit.findNode(fix.method, fix.clazz);
    if (methodNode == null) {
      return 0;
    }
    boolean[] thisMethodFlag = methodNode.annotFlags;
    if (index >= thisMethodFlag.length) {
      return 0;
    }
    int effect = 0;
    for (MethodNode subMethod : mit.getSubMethods(fix.method, fix.clazz, false)) {
      if (!thisMethodFlag[index]) {
        if (!subMethod.annotFlags[index]
            && !correspondingFixExists(
                subMethod.clazz,
                subMethod.method,
                subMethod.parameterNames[index],
                FixType.PARAMETER.name,
                fixesInOneRound)) {
          effect++;
        }
      }
    }
    List<MethodNode> superMethods = mit.getSuperMethods(fix.method, fix.clazz, false);
    if (superMethods == null || superMethods.size() == 0) {
      return effect;
    }
    MethodNode superMethod = superMethods.get(0);
    if (superMethod != null && !thisMethodFlag[index]) {
      if (superMethod.annotFlags[index]) {
        effect--;
      }
    }
    return effect;
  }
}
