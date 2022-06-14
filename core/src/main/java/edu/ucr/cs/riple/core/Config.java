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

package edu.ucr.cs.riple.core;

import com.google.common.base.Preconditions;
import edu.ucr.cs.riple.core.log.Log;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Config {
  public final boolean bailout;
  public final boolean optimized;
  public final boolean lexicalPreservationDisabled;
  public final boolean chain;
  public final boolean useCache;

  public final boolean disableOuterLoop;
  public final Path dir;
  public final Path nullAwayConfigPath;
  public final Path cssConfigPath;
  public final String buildCommand;
  public final String nullableAnnot;
  public final String initializerAnnot;

  public final Log log;
  public final int depth;

  /**
   * Builds config from command line arguments.
   *
   * @param args arguments.
   */
  public Config(String[] args) {

    Options options = new Options();

    // Help
    Option helpOption = new Option("h", "help", false, "Shows all flags");
    helpOption.setRequired(false);
    options.addOption(helpOption);

    // Build
    Option buildCommandOption =
        new Option(
            "bc",
            "build-command",
            true,
            "Command to Run NullAway on the target project, this command must include changing directory from root to the target project");
    buildCommandOption.setRequired(true);
    options.addOption(buildCommandOption);

    // Config Path
    Option configPath =
        new Option(
            "p", "path", true, "Path to config file containing all flags values in json format");
    configPath.setRequired(false);
    options.addOption(configPath);

    // Nullable Annotation
    Option nullableOption =
        new Option("n", "nullable", true, "Fully Qualified name of the Nullable annotation");
    nullableOption.setRequired(false);
    options.addOption(nullableOption);

    // Initializer Annotation
    Option initializerOption =
        new Option("i", "initializer", true, "Fully Qualified name of the Initializer annotation");
    initializerOption.setRequired(true);
    options.addOption(initializerOption);

    // Format
    Option disableLexicalPreservationOption =
        new Option("dlp", "disable-lexical-preservation", false, "Disables lexical preservation");
    disableLexicalPreservationOption.setRequired(false);
    options.addOption(disableLexicalPreservationOption);

    // Bailout
    Option disableBailoutOption =
        new Option(
            "db",
            "disable-bailout",
            false,
            "Disables bailout, Annotator will not bailout from the search tree as soon as its effectiveness hits zero or less and completely traverses the tree until no new fix is suggested");
    disableBailoutOption.setRequired(false);
    options.addOption(disableBailoutOption);

    // Depth
    Option depthOption = new Option("depth", "depth", true, "Depth of the analysis");
    depthOption.setRequired(false);
    options.addOption(depthOption);

    // Cache
    Option disableCacheOption = new Option("dc", "disable-cache", false, "Disables cache usage");
    disableCacheOption.setRequired(false);
    options.addOption(disableCacheOption);

    // Chain
    Option chainOption =
        new Option(
            "ch", "chain", false, "Injects the complete tree of fixes associated to the fix");
    chainOption.setRequired(false);
    options.addOption(chainOption);

    // Optimized
    Option disableOptimizationOption =
        new Option("do", "disable-optimization", false, "Disables optimizations");
    disableOptimizationOption.setRequired(false);
    options.addOption(disableOptimizationOption);

    // Outer Loop
    Option disableOuterLoopOption =
        new Option("dol", "disable-outer-loop", false, "Disables Outer Loop");
    disableOuterLoopOption.setRequired(false);
    options.addOption(disableOuterLoopOption);

    // Dir
    Option dirOption = new Option("d", "dir", true, "Directory of the output files");
    dirOption.setRequired(true);
    options.addOption(dirOption);

    // NullAway Config Path
    Option nullAwayConfigPathOption =
        new Option("ncp", "nullaway-config-path", true, "Path to the NullAway Config");
    nullAwayConfigPathOption.setRequired(true);
    options.addOption(nullAwayConfigPathOption);

    // CSS Config Path
    Option cssConfigPathOption =
        new Option("ccp", "css-config-path", true, "Path to the CSS Config");
    cssConfigPathOption.setRequired(true);
    options.addOption(cssConfigPathOption);

    HelpFormatter formatter = new HelpFormatter();
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = null;

    if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
      showHelpAndQuit(formatter, options);
    }

    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      System.out.println(e.getMessage());
      showHelpAndQuit(formatter, options);
    }
    Preconditions.checkNotNull(cmd, "Error parsing cmd, cmd cannot bu null");

    this.buildCommand = cmd.getOptionValue(buildCommandOption.getLongOpt());
    this.nullableAnnot =
        cmd.hasOption(nullableOption.getLongOpt())
            ? cmd.getOptionValue(nullableOption.getLongOpt())
            : "javax.annotation.Nullable";
    this.initializerAnnot = cmd.getOptionValue(initializerOption.getLongOpt());
    this.depth =
        Integer.parseInt(
            cmd.hasOption(depthOption.getLongOpt())
                ? cmd.getOptionValue(depthOption.getLongOpt())
                : "5");
    this.dir = Paths.get(cmd.getOptionValue(dirOption.getLongOpt()));
    this.nullAwayConfigPath = Paths.get(cmd.getOptionValue(nullAwayConfigPathOption.getLongOpt()));
    this.cssConfigPath = Paths.get(cmd.getOptionValue(cssConfigPathOption.getLongOpt()));
    this.lexicalPreservationDisabled = cmd.hasOption(disableLexicalPreservationOption.getLongOpt());
    this.chain = cmd.hasOption(chainOption.getLongOpt());
    this.bailout = !cmd.hasOption(disableBailoutOption.getLongOpt());
    this.useCache = !cmd.hasOption(disableCacheOption.getLongOpt());
    this.disableOuterLoop = cmd.hasOption(disableOuterLoopOption.getLongOpt());
    this.optimized = !cmd.hasOption(disableOptimizationOption.getLongOpt());
    this.log = new Log();
    this.log.reset();
  }

  /**
   * Builds config from json config file.
   *
   * @param configPath path to config file.
   */
  public Config(String configPath) {
    Preconditions.checkNotNull(configPath);
    JSONObject jsonObject;
    try {
      Object obj =
          new JSONParser()
              .parse(Files.newBufferedReader(Paths.get(configPath), Charset.defaultCharset()));
      jsonObject = (JSONObject) obj;
    } catch (Exception e) {
      throw new RuntimeException("Error in reading/parsing config at path: " + configPath, e);
    }
    this.depth = getValueFromKey(jsonObject, "DEPTH", Long.class).orElse((long) 1).intValue();
    this.chain = getValueFromKey(jsonObject, "CHAIN", Boolean.class).orElse(false);
    this.useCache = getValueFromKey(jsonObject, "CACHE", Boolean.class).orElse(true);
    this.lexicalPreservationDisabled =
        !getValueFromKey(jsonObject, "LEXICAL_PRESERVATION", Boolean.class).orElse(false);
    this.optimized = getValueFromKey(jsonObject, "OPTIMIZED", Boolean.class).orElse(true);
    this.disableOuterLoop = !getValueFromKey(jsonObject, "OUTER_LOOP", Boolean.class).orElse(false);
    this.bailout = getValueFromKey(jsonObject, "BAILOUT", Boolean.class).orElse(true);
    this.nullableAnnot =
        getValueFromKey(jsonObject, "ANNOTATION:NULLABLE", String.class)
            .orElse("javax.annotation.Nullable");
    this.initializerAnnot =
        getValueFromKey(jsonObject, "ANNOTATION:INITIALIZER", String.class)
            .orElse("javax.annotation.Nullable");
    this.nullAwayConfigPath =
        Paths.get(
            getValueFromKey(jsonObject, "NULLAWAY_CONFIG_PATH", String.class)
                .orElse("/tmp/NullAwayFix/config.xml"));
    this.cssConfigPath =
        Paths.get(
            getValueFromKey(jsonObject, "CSS_CONFIG_PATH", String.class)
                .orElse("/tmp/NullAwayFix/css.xml"));
    this.dir =
        Paths.get(
            getValueFromKey(jsonObject, "OUTPUT_DIR", String.class).orElse("/tmp/NullAwayFix"));
    this.buildCommand = getValueFromKey(jsonObject, "BUILD_COMMAND", String.class).orElse(null);
    this.log = new Log();
    log.reset();
  }

  private static void showHelpAndQuit(HelpFormatter formatter, Options options) {
    formatter.printHelp("Annotator config Flags", options);
    System.exit(1);
  }

  static class OrElse<T> {
    final Object value;
    final Class<T> klass;

    OrElse(Object value, Class<T> klass) {
      this.value = value;
      this.klass = klass;
    }

    T orElse(T other) {
      return value == null ? other : klass.cast(this.value);
    }
  }

  private <T> OrElse<T> getValueFromKey(JSONObject json, String key, Class<T> klass) {
    if (json == null) {
      return new OrElse<>(null, klass);
    }
    try {
      ArrayList<String> keys = new ArrayList<>(Arrays.asList(key.split(":")));
      while (keys.size() != 1) {
        if (json.containsKey(keys.get(0))) {
          json = (JSONObject) json.get(keys.get(0));
          keys.remove(0);
        } else {
          return new OrElse<>(null, klass);
        }
      }
      return json.containsKey(keys.get(0))
          ? new OrElse<>(json.get(keys.get(0)), klass)
          : new OrElse<>(null, klass);
    } catch (Exception e) {
      return new OrElse<>(null, klass);
    }
  }

  public static class Builder {

    public String buildCommand;
    public String initializerAnnotation;
    public String nullableAnnotation;
    public String outputDir;
    public String nullAwayConfigPath;
    public String cssConfigPath;
    public boolean chain = false;
    public boolean optimized = true;
    public boolean cache = true;
    public boolean bailout = true;
    public boolean lexicalPreservationActivation = true;
    public boolean outerLoopActivation = true;
    public int depth = 1;

    public void write(Path path) {
      Preconditions.checkNotNull(
          buildCommand, "Build command must be initialized to construct the config.");
      Preconditions.checkNotNull(
          initializerAnnotation,
          "Initializer Annotation must be initialized to construct the config.");
      Preconditions.checkNotNull(
          outputDir, "Output Directory must be initialized to construct the config.");
      Preconditions.checkNotNull(
          nullAwayConfigPath, "NulLAway Config Path must be initialized to construct the config.");
      Preconditions.checkNotNull(
          cssConfigPath, "CSS Config Path must be initialized to construct the config.");
      Preconditions.checkNotNull(
          nullableAnnotation, "Nullable Annotation must be initialized to construct the config.");
      JSONObject json = new JSONObject();
      json.put("BUILD_COMMAND", buildCommand);
      JSONObject annotation = new JSONObject();
      annotation.put("INITIALIZER", initializerAnnotation);
      annotation.put("NULLABLE", nullableAnnotation);
      json.put("ANNOTATION", annotation);
      json.put("LEXICAL_PRESERVATION", lexicalPreservationActivation);
      json.put("OUTER_LOOP", outerLoopActivation);
      json.put("OUTPUT_DIR", outputDir);
      json.put("NULLAWAY_CONFIG_PATH", nullAwayConfigPath);
      json.put("CSS_CONFIG_PATH", cssConfigPath);
      json.put("CHAIN", chain);
      json.put("OPTIMIZED", optimized);
      json.put("CACHE", cache);
      json.put("BAILOUT", bailout);
      json.put("DEPTH", depth);
      try (FileWriter file = new FileWriter(path.toFile())) {
        file.write(json.toJSONString());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
