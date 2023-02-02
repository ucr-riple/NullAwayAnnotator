/*
 * MIT License
 *
 * Copyright (c) 2020 anonymous
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

package com.example.tool.core.util;

import com.example.tool.core.ModuleInfo;
import com.example.tool.core.metadata.field.FieldDeclarationStore;
import com.google.common.collect.ImmutableSet;
import com.example.tool.core.Config;
import com.example.tool.core.Report;
import com.example.tool.core.metadata.index.Error;
import com.example.tool.core.metadata.index.Fix;
import com.example.too.scanner.AnnotatorScanner;
import com.example.too.scanner.ScannerConfigWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

/** Utility class. */
public class Utility {

  /**
   * Executes a shell command in a subprocess. If {@link Config#redirectBuildOutputToStdErr} is
   * activated, it will write the command's output in std error.
   *
   * @param config Annotator config.
   * @param command The shell command to run.
   */
  public static void executeCommand(Config config, String command) {
    try {
      Process p = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", command});
      BufferedReader reader =
          new BufferedReader(new InputStreamReader(p.getErrorStream(), Charset.defaultCharset()));
      String line;
      while ((line = reader.readLine()) != null) {
        if (config.redirectBuildOutputToStdErr) {
          System.err.println(line);
        }
      }
      p.waitFor();
    } catch (Exception e) {
      throw new RuntimeException("Exception happened in executing command: " + command, e);
    }
  }

  /**
   * Writes reports content in json format in reports.json file in the output directory.
   *
   * @param config Annotator config.
   * @param reports Immutable set of reports.
   */
  @SuppressWarnings("unchecked")
  public static void writeReports(Config config, ImmutableSet<Report> reports) {
    Path reportsPath = config.globalDir.resolve("reports.json");
    JSONObject result = new JSONObject();
    JSONArray reportsJson = new JSONArray();
    for (Report report : reports) {
      JSONObject reportJson = report.root.getJson();
      reportJson.put("LOCAL EFFECT", report.localEffect);
      reportJson.put("OVERALL EFFECT", report.getOverallEffect(config));
      reportJson.put("Upper Bound EFFECT", report.getUpperBoundEffectOnDownstreamDependencies());
      reportJson.put("Lower Bound EFFECT", report.getLowerBoundEffectOnDownstreamDependencies());
      reportJson.put("FINISHED", !report.requiresFurtherProcess(config));
      JSONArray followUps = new JSONArray();
      followUps.addAll(report.tree.stream().map(Fix::getJson).collect(Collectors.toList()));
      followUps.remove(report.root.getJson());
      reportJson.put("TREE", followUps);
      reportsJson.add(reportJson);
    }
    // Sort by overall effect.
    reportsJson.sort(
        (o1, o2) -> {
          int first = (Integer) ((JSONObject) o1).get("OVERALL EFFECT");
          int second = (Integer) ((JSONObject) o2).get("OVERALL EFFECT");
          return Integer.compare(second, first);
        });
    result.put("REPORTS", reportsJson);
    try (BufferedWriter writer =
        Files.newBufferedWriter(reportsPath.toFile().toPath(), Charset.defaultCharset())) {
      writer.write(result.toJSONString().replace("\\/", "/").replace("\\\\\\", "\\"));
      writer.flush();
    } catch (IOException e) {
      throw new RuntimeException(
          "Could not create the Annotator report json file: " + reportsPath, e);
    }
  }

  /**
   * Reads serialized errors "errors.tsv" file in the output directory, and returns the collected
   * set of resolving fixes for read errors.
   *
   * @param config Annotator config.
   * @param store Field Declaration store.
   * @return Set of collected fixes.
   */
  public static Set<Fix> readFixesFromOutputDirectory(Config config, FieldDeclarationStore store) {
    Set<Error> errors = readErrorsFromOutputDirectory(config, config.target, store);
    return Error.getResolvingFixesOfErrors(errors);
  }

  /**
   * Reads serialized errors of passed module in "errors.tsv" file in the output directory,
   *
   * @param info Module info.
   * @param fieldDeclarationStore Field Declaration store.
   * @return Set of serialized errors.
   */
  public static Set<Error> readErrorsFromOutputDirectory(
          Config config, ModuleInfo info, FieldDeclarationStore fieldDeclarationStore) {
    Path errorsPath = info.dir.resolve("errors.tsv");
    Set<Error> errors = new LinkedHashSet<>();
    try {
      try (BufferedReader br =
          Files.newBufferedReader(errorsPath.toFile().toPath(), Charset.defaultCharset())) {
        String line;
        // Skip header.
        br.readLine();
        while ((line = br.readLine()) != null) {
          errors.add(config.getAdapter().deserializeError(line.split("\t"), fieldDeclarationStore));
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Exception happened in reading errors at: " + errorsPath, e);
    }
    return errors;
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

      // Output dir
      Element outputDir = doc.createElement("path");
      outputDir.setTextContent(config.outputDirectory);
      rootElement.appendChild(outputDir);

      // UUID
      Element uuid = doc.createElement("uuid");
      uuid.setTextContent(UUID.randomUUID().toString());
      rootElement.appendChild(uuid);

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

  /**
   * Activates/Deactivates {@link AnnotatorScanner} features by updating the {@link
   * com.example.too.scanner.Config} in {@code XML} format for the given modules.
   *
   * @param config Annotator config.
   * @param modules Immutable set of modules that their configuration files need to be updated.
   * @param activation activation flag for all features of the scanner.
   */
  public static void setScannerCheckerActivation(
      Config config, ImmutableSet<ModuleInfo> modules, boolean activation) {
    modules.forEach(info -> setScannerCheckerActivation(config, info, activation));
  }

  /**
   * Activates/Deactivates {@link AnnotatorScanner} features by updating the {@link
   * com.example.too.scanner.Config} in {@code XML} format for the given module.
   *
   * @param config Annotator config.
   * @param info module that its configuration file need to be updated.
   * @param activation activation flag for all features of the scanner.
   */
  public static void setScannerCheckerActivation(
      Config config, ModuleInfo info, boolean activation) {
    ScannerConfigWriter writer = new ScannerConfigWriter();
    writer
        .setCallTrackerActivation(activation)
        .setClassTrackerActivation(activation)
        .setFieldTrackerActivation(activation)
        .setMethodTrackerActivation(activation)
        .addGeneratedCodeDetectors(config.generatedCodeDetectors)
        .setOutput(info.dir)
        .setNonnullAnnotations(config.getNonnullAnnotations())
        .writeAsXML(info.scannerConfig);
  }

  /**
   * Builds all downstream dependencies.
   *
   * @param config Annotator config.
   */
  public static void buildDownstreamDependencies(Config config) {
    config.downstreamInfo.forEach(
        module -> {
          FixSerializationConfig.Builder nullAwayConfig =
              new FixSerializationConfig.Builder()
                  .setSuggest(true, true)
                  .setOutputDirectory(module.dir.toString())
                  .setFieldInitInfo(false);
          nullAwayConfig.writeAsXML(module.nullawayConfig.toString());
        });
    build(config, config.downstreamDependenciesBuildCommand);
    config.log.incrementDownstreamBuildRequest();
  }

  /**
   * Builds target.
   *
   * @param config Annotator config.
   */
  public static void buildTarget(Config config) {
    buildTarget(config, false);
  }

  /**
   * Builds target with control on field initialization serialization.
   *
   * @param config Annotator config.
   * @param initSerializationEnabled Activation flag for field initialization serialization.
   */
  public static void buildTarget(Config config, boolean initSerializationEnabled) {
    FixSerializationConfig.Builder nullAwayConfig =
        new FixSerializationConfig.Builder()
            .setSuggest(true, true)
            .setOutputDirectory(config.target.dir.toString())
            .setFieldInitInfo(initSerializationEnabled);
    nullAwayConfig.writeAsXML(config.target.nullawayConfig.toString());
    build(config, config.buildCommand);
    config.log.incrementBuildRequest();
  }

  /**
   * Builds module(s).
   *
   * @param config Annotator config.
   * @param command Command to run to build module(s).
   */
  private static void build(Config config, String command) {
    try {
      long timer = config.log.startTimer();
      Utility.executeCommand(config, command);
      config.log.stopTimerAndCaptureBuildTime(timer);
    } catch (Exception e) {
      throw new RuntimeException("Could not run command: " + command);
    }
  }

  /**
   * Returns a progress bar with the given task name.
   *
   * @param taskName Task name.
   * @param steps Number of total steps to show in the progress bar.
   * @return Progress bar instance.
   */
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

  /**
   * Writes log in the `log.txt` file at the output directory.
   *
   * @param config Annotator config.
   */
  public static void writeLog(Config config) {
    Path path = config.globalDir.resolve("log.txt");
    try {
      Files.write(path, Collections.singleton(config.log.toString()), Charset.defaultCharset());
    } catch (IOException exception) {
      System.err.println("Could not write log to: " + path);
      System.err.println("Writing in STD Error:\n" + config.log);
    }
  }

  /**
   * Read all lines from a file and returns as a List.
   *
   * @param path The path to the file.
   * @return The lines from the file as a Stream.
   */
  public static List<String> readFileLines(Path path) {
    try (Stream<String> stream = Files.lines(path)) {
      return stream.collect(Collectors.toList());
    } catch (IOException e) {
      throw new RuntimeException("Exception while reading file: " + path, e);
    }
  }
}