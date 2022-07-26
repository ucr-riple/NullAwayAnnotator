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
import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.log.Log;
import edu.ucr.cs.riple.core.metadata.submodules.Module;
import edu.ucr.cs.riple.core.util.Utility;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Config {
  public final boolean bailout;
  public final boolean optimized;
  public final boolean exhaustiveSearch;
  public final boolean lexicalPreservationDisabled;
  public final boolean chain;
  public final boolean useCache;
  public final boolean redirectBuildOutputToStdErr;
  public final boolean disableOuterLoop;
  public final Path dir;
  public final Path nullAwayConfigPath;
  public final Path scannerConfigPath;
  public final String buildCommand;
  public final String nullableAnnot;
  public final String initializerAnnot;
  public final ImmutableSet<String> downstreamModulesBuildCommands;
  public final Path nullawayLibraryModelLoaderPath;
  public final boolean downStreamDependenciesAnalysisActivated;
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

    // Exhaustive
    Option exhaustiveSearchOption =
        new Option("exs", "exhaustive-search", false, "Performs Exhaustive Search");
    exhaustiveSearchOption.setRequired(false);
    options.addOption(exhaustiveSearchOption);

    // Build Output Redirect
    Option redirectBuildOutputToStdErrOption =
        new Option(
            "rboserr", "redirect-build-output-stderr", false, "Redirects Build outputs to STD Err");
    redirectBuildOutputToStdErrOption.setRequired(false);
    options.addOption(redirectBuildOutputToStdErrOption);

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

    // Scanner Config Path
    Option scannerConfigPathOption =
        new Option("scp", "scanner-config-path", true, "Path to the Scanner Config");
    scannerConfigPathOption.setRequired(true);
    options.addOption(scannerConfigPathOption);

    // Down stream analysis
    Option downstreamDependenciesBuildCommandOption =
        new Option(
            "ddbc",
            "downstream-dependencies-build-command",
            true,
            "Path to a file containing all downstream dependencies build commands separated by new line.");
    downstreamDependenciesBuildCommandOption.setRequired(false);
    options.addOption(downstreamDependenciesBuildCommandOption);
    Option nullawayLibraryModelLoaderPathOption =
        new Option(
            "nlmlp",
            "nullaway-library-model-loader-path",
            true,
            "NullAway Library Model loader path");
    nullawayLibraryModelLoaderPathOption.setRequired(false);
    options.addOption(nullawayLibraryModelLoaderPathOption);

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

    // Below is only to guide IDE that cmd is nonnull at this point.
    Preconditions.checkNotNull(
        cmd, "cmd cannot be null, error in CommandLineParser for returning null.");

    if (cmd.hasOption(downstreamDependenciesBuildCommandOption)
        != cmd.hasOption(nullawayLibraryModelLoaderPathOption)) {
      throw new IllegalArgumentException(
          "To enable downstream dependencies analysis, both --nullaway-library-model-loader-path and --downstream-modules-build-command options must be passed");
    }

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
    this.scannerConfigPath = Paths.get(cmd.getOptionValue(scannerConfigPathOption.getLongOpt()));
    this.lexicalPreservationDisabled = cmd.hasOption(disableLexicalPreservationOption.getLongOpt());
    this.chain = cmd.hasOption(chainOption.getLongOpt());
    this.redirectBuildOutputToStdErr =
        cmd.hasOption(redirectBuildOutputToStdErrOption.getLongOpt());
    this.bailout = !cmd.hasOption(disableBailoutOption.getLongOpt());
    this.useCache = !cmd.hasOption(disableCacheOption.getLongOpt());
    this.disableOuterLoop = cmd.hasOption(disableOuterLoopOption.getLongOpt());
    this.optimized = !cmd.hasOption(disableOptimizationOption.getLongOpt());
    this.exhaustiveSearch = cmd.hasOption(exhaustiveSearchOption.getLongOpt());

    this.downStreamDependenciesAnalysisActivated =
        cmd.hasOption(downstreamDependenciesBuildCommandOption);
    if (this.downStreamDependenciesAnalysisActivated) {
      this.downstreamModulesBuildCommands =
          Utility.readFileLines(
                  Paths.get(cmd.getOptionValue(downstreamDependenciesBuildCommandOption)))
              .collect(ImmutableSet.toImmutableSet());
      this.nullawayLibraryModelLoaderPath =
          Paths.get(cmd.getOptionValue(nullawayLibraryModelLoaderPathOption));
    } else {
      this.nullawayLibraryModelLoaderPath = null;
      this.downstreamModulesBuildCommands = null;
    }

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
    this.redirectBuildOutputToStdErr =
        getValueFromKey(jsonObject, "REDIRECT_BUILD_OUTPUT_TO_STDERR", Boolean.class).orElse(false);
    this.useCache = getValueFromKey(jsonObject, "CACHE", Boolean.class).orElse(true);
    this.lexicalPreservationDisabled =
        !getValueFromKey(jsonObject, "LEXICAL_PRESERVATION", Boolean.class).orElse(false);
    this.optimized = getValueFromKey(jsonObject, "OPTIMIZED", Boolean.class).orElse(true);
    this.exhaustiveSearch =
        getValueFromKey(jsonObject, "EXHAUSTIVE_SEARCH", Boolean.class).orElse(true);
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
    this.scannerConfigPath =
        Paths.get(
            getValueFromKey(jsonObject, "SCANNER_CONFIG_PATH", String.class)
                .orElse("/tmp/NullAwayFix/scanner.xml"));
    this.dir = Paths.get(getValueFromKey(jsonObject, "OUTPUT_DIR", String.class).orElse(null));
    this.buildCommand = getValueFromKey(jsonObject, "BUILD_COMMAND", String.class).orElse(null);
    this.downstreamModulesBuildCommands =
        ImmutableSet.copyOf(
            getArrayValueFromKey(
                    jsonObject, "DOWNSTREAM_DEPENDENCY_ANALYSIS:BUILD_COMMANDS", String.class)
                .orElse(Collections.emptyList()));
    String nullawayLibraryModelLoaderPathString =
        getValueFromKey(
                jsonObject,
                "DOWNSTREAM_DEPENDENCY_ANALYSIS:LIBRARY_MODEL_LOADER_PATH",
                String.class)
            .orElse(null);
    this.nullawayLibraryModelLoaderPath =
        nullawayLibraryModelLoaderPathString == null
            ? null
            : Paths.get(nullawayLibraryModelLoaderPathString);
    this.downStreamDependenciesAnalysisActivated =
        getValueFromKey(jsonObject, "DOWNSTREAM_DEPENDENCY_ANALYSIS:ACTIVATION", Boolean.class)
            .orElse(false);
    this.log = new Log();
    log.reset();
  }

  private static void showHelpAndQuit(HelpFormatter formatter, Options options) {
    formatter.printHelp("Annotator config Flags", options);
    System.exit(1);
  }

  /**
   * Returns the set of downstream dependencies, returns empty list if {@link
   * Config#downStreamDependenciesAnalysisActivated} is false.
   *
   * @return ImmutableSet of {@link edu.ucr.cs.riple.core.metadata.submodules.Module}.
   */
  public ImmutableSet<Module> getDownstreamDependencies() {
    if (!this.downStreamDependenciesAnalysisActivated) {
      return ImmutableSet.of();
    }
    // Variables inside lambda must be final, need to wrap it a final array.
    final int[] id = {1};
    return downstreamModulesBuildCommands.stream()
        .map(command -> new Module("module-" + id[0]++, command))
        .collect(ImmutableSet.toImmutableSet());
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

  private <T> CollectionOrElse<T> getArrayValueFromKey(
      JSONObject json, String key, Class<T> klass) {
    if (json == null) {
      return new CollectionOrElse<>(null, klass);
    }
    OrElse<T> jsonValue = getValueFromKey(json, key, klass);
    if (jsonValue.value == null) {
      return new CollectionOrElse<>(null, klass);
    } else {
      if (jsonValue.value instanceof JSONArray) {
        return new CollectionOrElse<>((JSONArray) jsonValue.value, klass);
      }
      throw new IllegalStateException(
          "Expected type to be json array, found: " + jsonValue.value.getClass());
    }
  }

  private static class OrElse<T> {
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

  private static class CollectionOrElse<T> {
    final Collection<?> value;
    final Class<T> klass;

    CollectionOrElse(Collection<?> value, Class<T> klass) {
      this.value = value;
      this.klass = klass;
    }

    Collection<T> orElse(Collection<T> other) {
      if (value == null) {
        return other;
      } else {
        return this.value.stream().map(klass::cast).collect(Collectors.toList());
      }
    }
  }

  public static class Builder {

    public String buildCommand;
    public String initializerAnnotation;
    public String nullableAnnotation;
    public String outputDir;
    public String nullAwayConfigPath;
    public String scannerConfigPath;
    public boolean chain = false;
    public boolean optimized = true;
    public boolean exhaustiveSearch = false;
    public boolean cache = true;
    public boolean bailout = true;
    public boolean redirectBuildOutputToStdErr = false;
    public boolean lexicalPreservationActivation = true;
    public boolean outerLoopActivation = true;

    public boolean downStreamDependenciesAnalysisActivated = false;
    public Set<String> downstreamModulesBuildCommands;
    public String nullawayLibraryModelLoaderPath;
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
          scannerConfigPath, "Scanner Config Path must be initialized to construct the config.");
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
      json.put("SCANNER_CONFIG_PATH", scannerConfigPath);
      json.put("CHAIN", chain);
      json.put("OPTIMIZED", optimized);
      json.put("CACHE", cache);
      json.put("BAILOUT", bailout);
      json.put("DEPTH", depth);
      json.put("EXHAUSTIVE_SEARCH", exhaustiveSearch);
      json.put("REDIRECT_BUILD_OUTPUT_TO_STDERR", redirectBuildOutputToStdErr);
      JSONObject downstreamDependency = new JSONObject();
      downstreamDependency.put("ACTIVATION", downStreamDependenciesAnalysisActivated);
      if (downStreamDependenciesAnalysisActivated) {
        Preconditions.checkNotNull(
            nullawayLibraryModelLoaderPath,
            "nullawayLibraryModelLoaderPath cannot be null to enable down stream dependency analysis.");
        downstreamDependency.put("LIBRARY_MODEL_LOADER_PATH", nullawayLibraryModelLoaderPath);
        JSONArray downstreamBuildCommandsJSON = new JSONArray();
        Preconditions.checkNotNull(
            downstreamModulesBuildCommands,
            "downstreamModulesBuildCommands cannot be null to enable down stream dependency analysis.");
        downstreamBuildCommandsJSON.addAll(downstreamModulesBuildCommands);
        downstreamDependency.put("BUILD_COMMANDS", downstreamBuildCommandsJSON);
      }
      json.put("DOWNSTREAM_DEPENDENCY_ANALYSIS", downstreamDependency);
      try (FileWriter file = new FileWriter(path.toFile())) {
        file.write(json.toJSONString());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
