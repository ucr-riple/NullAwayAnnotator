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
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.Report;
import edu.ucr.cs.riple.core.metadata.index.Factory;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
    Path reportsPath = config.dir.resolve("reports.json");
    JSONObject result = new JSONObject();
    JSONArray reportsJson = new JSONArray();
    for (Report report : reports) {
      JSONObject reportJson = report.root.getJson();
      reportJson.put("EFFECT", report.effect);
      reportJson.put("FINISHED", report.finished);
      JSONArray followUps = new JSONArray();
      if (config.chain && report.effect < 1) {
        report.tree.remove(report.root);
        followUps.addAll(
            report.tree.stream().map(fix -> fix.getJson()).collect(Collectors.toList()));
      }
      reportJson.put("TREE", followUps);
      reportsJson.add(reportJson);
    }
    reportsJson.sort(
        (o1, o2) -> {
          int first = (Integer) ((JSONObject) o1).get("EFFECT");
          int second = (Integer) ((JSONObject) o2).get("EFFECT");
          if (first == second) {
            return 0;
          }
          if (first < second) {
            return 1;
          }
          return -1;
        });
    result.put("REPORTS", reportsJson);
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

  public static Set<Fix> readFixesFromOutputDirectory(Config config, Factory<Fix> factory) {
    Path fixesPath = config.dir.resolve("fixes.tsv");
    Set<Fix> fixes = new HashSet<>();
    try {
      try (BufferedReader br = new BufferedReader(new FileReader(fixesPath.toFile()))) {
        String line;
        br.readLine();
        while ((line = br.readLine()) != null) {
          Fix fix = factory.build(line.split("\t"));
          Optional<Fix> existing = fixes.stream().filter(other -> other.equals(fix)).findAny();
          if (existing.isPresent()) {
            existing.get().reasons.addAll(fix.reasons);
          } else {
            fixes.add(fix);
          }
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Exception happened in reading fixes at: " + fixesPath, e);
    }
    return fixes;
  }

  /**
   * Writes the {@link FixSerializationConfig} in {@code XML} format.
   *
   * @param config Config file to write.
   * @param path Path to write the config at.
   */
  public static void writeNullAwayConfigInXMLFormat(FixSerializationConfig config, String path) {
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      Document doc = docBuilder.newDocument();

      // Root
      Element rootElement = doc.createElement("serialization");
      doc.appendChild(rootElement);

      // Suggest
      Element suggestElement = doc.createElement("suggest");
      suggestElement.setAttribute("active", String.valueOf(config.suggestEnabled));
      suggestElement.setAttribute("enclosing", String.valueOf(config.suggestEnclosing));
      rootElement.appendChild(suggestElement);

      // Field Initialization
      Element fieldInitInfoEnabled = doc.createElement("fieldInitInfo");
      fieldInitInfoEnabled.setAttribute("active", String.valueOf(config.fieldInitInfoEnabled));
      rootElement.appendChild(fieldInitInfoEnabled);

      // Method Parameter Protection Test
      Element paramTestElement = doc.createElement("paramTest");
      paramTestElement.setAttribute(
          "active", String.valueOf(config.methodParamProtectionTestEnabled));
      paramTestElement.setAttribute("index", String.valueOf(config.paramTestIndex));
      rootElement.appendChild(paramTestElement);

      // Annotations
      Element annots = doc.createElement("annotation");
      Element nonnull = doc.createElement("nonnull");
      nonnull.setTextContent(config.annotationConfig.getNonNull().getFullName());
      Element nullable = doc.createElement("nullable");
      nullable.setTextContent(config.annotationConfig.getNullable().getFullName());
      annots.appendChild(nullable);
      annots.appendChild(nonnull);
      rootElement.appendChild(annots);

      // Output dir
      Element outputDir = doc.createElement("path");
      outputDir.setTextContent(config.outputDirectory);
      rootElement.appendChild(outputDir);

      // Writings
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      DOMSource source = new DOMSource(doc);
      StreamResult result = new StreamResult(new File(path));
      transformer.transform(source, result);
    } catch (ParserConfigurationException | TransformerException e) {
      throw new RuntimeException("Error happened in writing config.", e);
    }
  }

  public static void setCSSCheckerActivation(Config config, boolean activation) {
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      Document doc = docBuilder.newDocument();

      // Root
      Element rootElement = doc.createElement("css");
      doc.appendChild(rootElement);

      // Method
      Element methodElement = doc.createElement("method");
      methodElement.setAttribute("active", String.valueOf(activation));
      rootElement.appendChild(methodElement);

      // Field
      Element fieldElement = doc.createElement("field");
      fieldElement.setAttribute("active", String.valueOf(activation));
      rootElement.appendChild(fieldElement);

      // Call
      Element callElement = doc.createElement("call");
      callElement.setAttribute("active", String.valueOf(activation));
      rootElement.appendChild(callElement);

      // File
      Element classElement = doc.createElement("class");
      classElement.setAttribute("active", String.valueOf(activation));
      rootElement.appendChild(classElement);

      // Output dir
      Element outputDir = doc.createElement("path");
      outputDir.setTextContent(config.dir.toString());
      rootElement.appendChild(outputDir);

      // Writings
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      DOMSource source = new DOMSource(doc);
      StreamResult result = new StreamResult(config.cssConfigPath.toFile());
      transformer.transform(source, result);
    } catch (ParserConfigurationException | TransformerException e) {
      throw new RuntimeException("Error happened in writing config.", e);
    }
  }

  public static void buildProject(Config config) {
    long timer = config.log.startTimer();
    buildProject(config, false);
    config.log.stopTimerAndCaptureBuildTime(timer);
    config.log.incrementBuildRequest();
  }

  public static void buildProject(Config config, boolean initSerializationEnabled) {
    FixSerializationConfig.Builder nullAwayConfig =
        new FixSerializationConfig.Builder()
            .setSuggest(true, true)
            .setAnnotations(config.nullableAnnot, "UNKNOWN")
            .setOutputDirectory(config.dir.toString())
            .setFieldInitInfo(initSerializationEnabled);
    nullAwayConfig.writeAsXML(config.nullAwayConfigPath.toString());
    try {
      Utility.executeCommand(config.buildCommand);
    } catch (Exception e) {
      throw new RuntimeException("Could not run command: " + config.buildCommand);
    }
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
    File file = config.dir.resolve("log.txt").toFile();
    try (FileOutputStream fos = new FileOutputStream(file)) {
      BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
      bw.write(config.log.toString());
      bw.newLine();
      bw.close();
    } catch (Exception ignored) {
      System.err.println("Could not write log to: " + file.getAbsolutePath());
      System.err.println("Writing here: " + config.log);
    }
  }
}
