/*
 * MIT License
 *
 * Copyright (c) 2023 Nima Karimipour
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

import com.github.javaparser.ParserConfiguration;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.ucr.cs.riple.core.module.ModuleConfiguration;
import edu.ucr.cs.riple.core.util.JsonParser;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.scanner.generatedcode.SourceType;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Configuration class which controls all the parameters of the Annotator received either from the
 * command line arguments or the given json file.
 */
public class Config {

  /**
   * If activated, annotator will bail out from the search tree as soon as the effectiveness gets
   * zero or less.
   */
  public final boolean bailout;

  /** If activated, impact of fixes will be computed in parallel. */
  public final boolean useParallelGraphProcessor;

  /** If activated, impact of fixes will be cached. */
  public final boolean useImpactCache;

  /**
   * If activated, all suggested fixes from the checker will be applied to the source code
   * regardless of their effectiveness.
   */
  public final boolean exhaustiveSearch;

  /**
   * If activated, all containing fixes in the fix tree will be applied to the source code,
   * otherwise only the root fix will be applied.
   */
  public final boolean chain;

  /**
   * If enabled, at each iteration fixes with effectiveness will be tagged to exclude them in
   * further iterations.
   */
  public final boolean useCache;

  /** If true, build outputs will be redirected to STD Err. */
  public final boolean redirectBuildOutputToStdErr;

  /** If activated, it will disable the outer loop. */
  public final boolean disableOuterLoop;

  /** Info of target module. */
  public final ModuleConfiguration target;

  /** Path to directory where all outputs of checker and scanner checker is located. */
  public final Path globalDir;

  /** Command to build the target module. */
  public final String buildCommand;

  /** Fully qualified name of the {@code nullable} annotation. */
  public final String nullableAnnot;

  /** Fully qualified name of the {@code initializer} annotation. */
  public final String initializerAnnot;

  /** If enabled, effects of public API on downstream dependencies will be considered. */
  public final boolean downStreamDependenciesAnalysisActivated;

  /** Sets of context path information for all downstream dependencies. */
  public final ImmutableSet<ModuleConfiguration> downstreamConfigurations;

  /**
   * Path to NullAway library model loader resource directory, which enables the communication
   * between annotator and NullAway when processing downstream dependencies. Annotator will write
   * annotation on files in this directory and the using Library Model Loader reads them.
   */
  public final Path nullawayLibraryModelLoaderPath;

  /** Command to build the all downstream dependencies at once. */
  public final String downstreamDependenciesBuildCommand;

  /**
   * Analysis mode. Will impact inference decisions when downstream dependency analysis is
   * activated.
   */
  public final AnalysisMode mode;

  /** Global counter for assigning unique id for each instance. */
  private int moduleCounterID;

  /**
   * If activated, Annotator will try to suppress all remaining errors. The logic is specific to the
   * checker suppression mechanism. Annotator will simply call {@link
   * edu.ucr.cs.riple.core.checkers.Checker#suppressRemainingErrors} methods.
   */
  public final boolean suppressRemainingErrors;

  /** Fully qualified NullUnmarked annotation. */
  public final String nullUnMarkedAnnotation;

  /**
   * Set of {@code @Nonnull} annotations to be acknowledged by Annotator. If an element is annotated
   * with these annotations along any other annotation ending with {@code "nonnull"}, Annotator will
   * acknowledge the annotation and will not add any {@code @Nullable} annotation to the element.
   */
  public final ImmutableSet<String> nonnullAnnotations;

  /** Depth of the analysis. Default to 5 if not set by the user */
  public final int depth;

  /**
   * Activates inference to add {@code @Nullable} qualifiers.
   *
   * <p>Note: Inference mode is mostly deactivated for experiments purposes and the default value is
   * {@code true} in production.
   */
  public final boolean inferenceActivated;

  /**
   * Generated code detectors. Responsible for detecting regions that are not present in the source
   * code and are generated by a processor.
   */
  public final ImmutableSet<SourceType> generatedCodeDetectors;

  /**
   * Checker name to retrieve the {@link edu.ucr.cs.riple.core.checkers.Checker} specific instance.
   */
  public final String checkerName;

  /** Language level to use when parsing Java code. Defaults to Java 17. */
  public final ParserConfiguration.LanguageLevel languageLevel;

  /**
   * Builds context from command line arguments.
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
            "Command to build the target project, this command must include changing directory from root to the target project");
    buildCommandOption.setRequired(true);
    options.addOption(buildCommandOption);

    // Context Path
    Option configPath =
        new Option(
            "p", "path", true, "Path to context file containing all flags values in json format");
    configPath.setRequired(false);
    options.addOption(configPath);

    // Context Paths
    Option configPathsOption =
        new Option(
            "cp",
            "config-paths",
            true,
            "Path to tsv file containing path to checker and scanner context files.");
    configPathsOption.setRequired(true);
    options.addOption(configPathsOption);

    // Checker Name
    Option checkerNameOption =
        new Option("cn", "checker-name", true, "Name of the checker to be used for analysis.");
    checkerNameOption.setRequired(true);
    options.addOption(checkerNameOption);

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

    // Parallel Processing
    Option disableParallelProcessingOption =
        new Option(
            "dpp", "disable-parallel-processing", false, "Disables parallel processing of fixes");
    disableParallelProcessingOption.setRequired(false);
    options.addOption(disableParallelProcessingOption);

    // Fix impact cache
    Option enableFixImpactCacheOption =
        new Option("eic", "enable-impact-cache", false, "Enables fix impact cache");
    enableFixImpactCacheOption.setRequired(false);
    options.addOption(enableFixImpactCacheOption);

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

    // Down stream analysis
    // Activation
    Option downstreamDependenciesActivationOption =
        new Option(
            "adda",
            "activate-downstream-dependencies-analysis",
            false,
            "Activates downstream dependency analysis");
    downstreamDependenciesActivationOption.setRequired(false);
    options.addOption(downstreamDependenciesActivationOption);
    // Down stream analysis: Build Command.
    Option downstreamDependenciesBuildCommandOption =
        new Option(
            "ddbc",
            "downstream-dependencies-build-command",
            true,
            "Command to build all downstream dependencies at once, this command must include changing directory from root to the target project");
    downstreamDependenciesBuildCommandOption.setRequired(false);
    options.addOption(downstreamDependenciesBuildCommandOption);
    // Down stream analysis: NullAway Library Model Path.
    Option nullawayLibraryModelLoaderPathOption =
        new Option(
            "nlmlp",
            "nullaway-library-model-loader-path",
            true,
            "NullAway Library Model loader resource directory path");
    nullawayLibraryModelLoaderPathOption.setRequired(false);
    options.addOption(nullawayLibraryModelLoaderPathOption);
    // Down stream analysis: Analysis mode.
    Option analysisMode =
        new Option(
            "am",
            "analysis-mode",
            true,
            "Analysis mode. Can be [default|upper_bound|lower_bound|strict]");
    analysisMode.setRequired(false);
    options.addOption(analysisMode);

    // Suppress remaining error activation
    Option suppressRemainingErrorsOption =
        new Option("sre", "suppress-remaining-errors", true, "Suppresses remaining errors");
    suppressRemainingErrorsOption.setRequired(false);
    options.addOption(suppressRemainingErrorsOption);

    // Disable inference
    Option deactivateInference =
        new Option("di", "deactivate-inference", false, "Deactivates inference.");
    deactivateInference.setRequired(false);
    options.addOption(deactivateInference);

    // Region detection for code generators
    // Lombok
    Option disableRegionDetectionByLombok =
        new Option(
            "drdl",
            "deactivate-region-detection-lombok",
            false,
            "Deactivates region detection for lombok generated code");
    disableRegionDetectionByLombok.setRequired(false);
    options.addOption(disableRegionDetectionByLombok);

    // Nonnull annotations.
    Option nonnullAnnotationsOption =
        new Option(
            "nna",
            "nonnull-annotations",
            true,
            "Adds a list of nonnull annotations separated by comma to be acknowledged by Annotator (e.g. com.example1.Nonnull,com.example2.Nonnull)");
    nonnullAnnotationsOption.setRequired(false);
    nonnullAnnotationsOption.setValueSeparator(',');
    options.addOption(nonnullAnnotationsOption);

    // Language level to use when parsing
    Option languageLevelOption =
        new Option(
            "ll",
            "language-level",
            true,
            "Java language level to use when parsing code. Supported values are 11 and 17.  Defaults to 17.");
    languageLevelOption.setRequired(false);
    options.addOption(languageLevelOption);

    HelpFormatter formatter = new HelpFormatter();
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd;

    if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
      showHelp(formatter, options);
      // Exit
      System.exit(0);
    }

    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      showHelp(formatter, options);
      throw new IllegalArgumentException("Error in reading context flags: " + e.getMessage(), e);
    }

    // Check if either all flags are available or none is present
    if (!(cmd.hasOption(downstreamDependenciesActivationOption)
            == cmd.hasOption(downstreamDependenciesBuildCommandOption)
        && cmd.hasOption(downstreamDependenciesActivationOption)
            == cmd.hasOption(nullawayLibraryModelLoaderPathOption))) {
      throw new IllegalArgumentException(
          "To activate downstream dependency analysis, all flags [--activate-downstream-dependencies-analysis, --downstream-dependencies-build-command (arg), --nullaway-library-model-loader-path (arg)] must be present!");
    }

    // Below is only to guide IDE that cmd is nonnull at this point.
    Preconditions.checkNotNull(
        cmd,
        "cmd cannot be null at this point, as that will cause CommandLineParser.parse to throw ParseException, and the handler above should stop execution in that case.");

    this.buildCommand = cmd.getOptionValue(buildCommandOption.getLongOpt());
    this.nullableAnnot =
        cmd.hasOption(nullableOption.getLongOpt())
            ? cmd.getOptionValue(nullableOption.getLongOpt())
            : "javax.annotation.Nullable";
    this.checkerName = cmd.getOptionValue(checkerNameOption);
    this.initializerAnnot = cmd.getOptionValue(initializerOption.getLongOpt());
    this.depth =
        Integer.parseInt(
            cmd.hasOption(depthOption.getLongOpt())
                ? cmd.getOptionValue(depthOption.getLongOpt())
                : "5");
    this.globalDir = Paths.get(cmd.getOptionValue(dirOption.getLongOpt()));
    List<ModuleConfiguration> moduleConfigurationList;
    moduleConfigurationList =
        Utility.readFileLines(Paths.get(cmd.getOptionValue(configPathsOption))).stream()
            .map(
                line -> {
                  String[] info = line.split("\\t");
                  return new ModuleConfiguration(
                      getNextModuleUniqueID(),
                      this.globalDir,
                      Paths.get(info[0]),
                      Paths.get(info[1]));
                })
            .collect(Collectors.toList());
    Preconditions.checkArgument(
        moduleConfigurationList.size() > 0, "Target module context paths not found.");
    // First line is information for the target module.
    this.target = moduleConfigurationList.get(0);
    this.chain = cmd.hasOption(chainOption.getLongOpt());
    this.redirectBuildOutputToStdErr =
        cmd.hasOption(redirectBuildOutputToStdErrOption.getLongOpt());
    this.bailout = !cmd.hasOption(disableBailoutOption.getLongOpt());
    this.useCache = !cmd.hasOption(disableCacheOption.getLongOpt());
    this.disableOuterLoop = cmd.hasOption(disableOuterLoopOption.getLongOpt());
    this.useParallelGraphProcessor = !cmd.hasOption(disableParallelProcessingOption.getLongOpt());
    this.useImpactCache = cmd.hasOption(enableFixImpactCacheOption.getLongOpt());
    this.exhaustiveSearch = cmd.hasOption(exhaustiveSearchOption.getLongOpt());
    this.downStreamDependenciesAnalysisActivated =
        cmd.hasOption(downstreamDependenciesActivationOption.getLongOpt());
    this.mode =
        AnalysisMode.parseMode(
            this.downStreamDependenciesAnalysisActivated,
            cmd.getOptionValue(analysisMode, "default"));

    if (this.downStreamDependenciesAnalysisActivated) {
      moduleConfigurationList.remove(0);
      this.downstreamConfigurations = ImmutableSet.copyOf(moduleConfigurationList);
      this.nullawayLibraryModelLoaderPath =
          Paths.get(cmd.getOptionValue(nullawayLibraryModelLoaderPathOption));
      this.downstreamDependenciesBuildCommand =
          cmd.getOptionValue(downstreamDependenciesBuildCommandOption.getLongOpt());
    } else {
      this.nullawayLibraryModelLoaderPath = null;
      this.downstreamConfigurations = ImmutableSet.of();
      this.downstreamDependenciesBuildCommand = null;
    }
    this.inferenceActivated = !cmd.hasOption(deactivateInference);
    this.suppressRemainingErrors =
        !this.inferenceActivated || cmd.hasOption(suppressRemainingErrorsOption);
    this.nullUnMarkedAnnotation =
        this.suppressRemainingErrors
            ? cmd.getOptionValue(suppressRemainingErrorsOption)
            : "org.jspecify.annotations.NullUnmarked";
    this.moduleCounterID = 0;
    this.generatedCodeDetectors =
        cmd.hasOption(disableRegionDetectionByLombok)
            ? ImmutableSet.of()
            : Sets.immutableEnumSet(SourceType.LOMBOK);
    this.languageLevel = getLanguageLevel(cmd.getOptionValue(languageLevelOption, "17"));
    this.nonnullAnnotations =
        !cmd.hasOption(nonnullAnnotationsOption)
            ? ImmutableSet.of()
            : ImmutableSet.copyOf(cmd.getOptionValue(nonnullAnnotationsOption).split(","));
  }

  /**
   * Gets the language level from the string representation. "11" for Java 11 and "17" for Java 17.
   *
   * @param languageLevelString string representation of the language level.
   * @return the language level.
   */
  private ParserConfiguration.LanguageLevel getLanguageLevel(String languageLevelString) {
    switch (languageLevelString) {
      case "11":
        return ParserConfiguration.LanguageLevel.JAVA_11;
      case "17":
        return ParserConfiguration.LanguageLevel.JAVA_17;
      default:
        throw new IllegalArgumentException("Unsupported language level: " + languageLevelString);
    }
  }

  /**
   * Builds context from json context file.
   *
   * @param configPath path to context file.
   */
  public Config(Path configPath) {
    Preconditions.checkNotNull(configPath);
    JsonParser parser = new JsonParser(configPath);
    this.checkerName = parser.getValueFromKey("CHECKER").orElse(null).getAsString();
    this.depth = parser.getValueFromKey("DEPTH").orElse(5).getAsInt();
    this.chain = parser.getValueFromKey("CHAIN").orElse(false).getAsBoolean();
    this.redirectBuildOutputToStdErr =
        parser.getValueFromKey("REDIRECT_BUILD_OUTPUT_TO_STDERR").orElse(false).getAsBoolean();
    this.useCache = parser.getValueFromKey("CACHE").orElse(true).getAsBoolean();
    this.useParallelGraphProcessor =
        parser.getValueFromKey("PARALLEL_PROCESSING").orElse(true).getAsBoolean();
    this.useImpactCache =
        parser.getValueFromKey("CACHE_IMPACT_ACTIVATION").orElse(false).getAsBoolean();
    this.exhaustiveSearch = parser.getValueFromKey("EXHAUSTIVE_SEARCH").orElse(true).getAsBoolean();
    this.disableOuterLoop = !parser.getValueFromKey("OUTER_LOOP").orElse(false).getAsBoolean();
    this.bailout = parser.getValueFromKey("BAILOUT").orElse(true).getAsBoolean();
    this.nullableAnnot =
        parser
            .getValueFromKey("ANNOTATION:NULLABLE")
            .orElse("javax.annotation.Nullable")
            .getAsString();
    this.initializerAnnot =
        parser
            .getValueFromKey("ANNOTATION:INITIALIZER")
            .orElse("com.uber.nullaway.annotations.Initializer")
            .getAsString();
    this.globalDir = Paths.get(parser.getValueFromKey("OUTPUT_DIR").orElse(null).getAsString());
    List<ModuleConfiguration> moduleConfigurationList =
        parser
            .getArrayValueFromKey(
                "CONFIG_PATHS",
                moduleInfo ->
                    ModuleConfiguration.buildFromJson(
                        getNextModuleUniqueID(), globalDir, moduleInfo))
            .orElse(Collections.emptyList());
    this.target = moduleConfigurationList.get(0);
    this.buildCommand = parser.getValueFromKey("BUILD_COMMAND").orElse(null).getAsString();
    this.downStreamDependenciesAnalysisActivated =
        parser
            .getValueFromKey("DOWNSTREAM_DEPENDENCY_ANALYSIS:ACTIVATION")
            .orElse(false)
            .getAsBoolean();
    this.downstreamDependenciesBuildCommand =
        parser
            .getValueFromKey("DOWNSTREAM_DEPENDENCY_ANALYSIS:BUILD_COMMAND")
            .orElse(null)
            .getAsString();
    String nullawayLibraryModelLoaderPathString =
        parser
            .getValueFromKey("DOWNSTREAM_DEPENDENCY_ANALYSIS:LIBRARY_MODEL_LOADER_PATH")
            .orElse(null)
            .getAsString();
    this.nullawayLibraryModelLoaderPath =
        nullawayLibraryModelLoaderPathString == null
            ? null
            : Paths.get(nullawayLibraryModelLoaderPathString);
    moduleConfigurationList.remove(0);
    this.mode =
        AnalysisMode.parseMode(
            this.downStreamDependenciesAnalysisActivated,
            parser
                .getValueFromKey("DOWNSTREAM_DEPENDENCY_ANALYSIS:ANALYSIS_MODE")
                .orElse("default")
                .getAsString());
    this.downstreamConfigurations = ImmutableSet.copyOf(moduleConfigurationList);
    this.moduleCounterID = 0;
    this.suppressRemainingErrors =
        parser.getValueFromKey("SUPPRESS_REMAINING_ERRORS").orElse(false).getAsBoolean();
    this.inferenceActivated =
        parser.getValueFromKey("INFERENCE_ACTIVATION").orElse(true).getAsBoolean();
    this.nullUnMarkedAnnotation =
        parser
            .getValueFromKey("ANNOTATION:NULL_UNMARKED")
            .orElse("org.jspecify.annotations.NullUnmarked")
            .getAsString();
    boolean lombokCodeDetectorActivated =
        parser
            .getValueFromKey("PROCESSORS:" + SourceType.LOMBOK.name() + ":ACTIVATION")
            .orElse(true)
            .getAsBoolean();
    this.generatedCodeDetectors =
        lombokCodeDetectorActivated ? Sets.immutableEnumSet(SourceType.LOMBOK) : ImmutableSet.of();
    this.languageLevel =
        getLanguageLevel(parser.getValueFromKey("LANGUAGE_LEVEL").orElse("17").getAsString());
    this.nonnullAnnotations =
        ImmutableSet.copyOf(
            parser
                .getArrayValueFromKey(
                    "ANNOTATION:NONNULL", json -> json.get("NONNULL").getAsString())
                .orElse(List.of()));
  }

  /**
   * Getter for nonnull annotations.
   *
   * @return Immutable set of fully qualified name of nonnull annotations.
   */
  public ImmutableSet<String> getNonnullAnnotations() {
    return nonnullAnnotations;
  }

  /**
   * Returns the latest id associated to a module, used to create unique ids for each module and
   * increments it.
   *
   * @return last id value used in integer.
   */
  private int getNextModuleUniqueID() {
    return moduleCounterID++;
  }

  private static void showHelp(HelpFormatter formatter, Options options) {
    formatter.printHelp("Annotator context Flags", options);
  }

  /** Builder for creating a {@link Config} instance on json file. */
  public static class Builder {

    public String buildCommand;
    public String initializerAnnotation;
    public String nullableAnnotation;
    public String outputDir;

    /**
     * List of modules, did not use {@link java.util.Set} to preserve order. First project is the
     * target project.
     */
    public List<ModuleConfiguration> configPaths;

    public boolean chain = false;
    public boolean useParallelProcessor = true;
    public boolean exhaustiveSearch = false;
    public boolean cache = true;
    public boolean bailout = true;
    public boolean redirectBuildOutputToStdErr = false;
    public boolean outerLoopActivation = true;
    public boolean downStreamDependenciesAnalysisActivated = false;
    public Path nullawayLibraryModelLoaderPath;
    public AnalysisMode mode = AnalysisMode.LOCAL;
    public String downstreamBuildCommand;
    public boolean suppressRemainingErrors = false;
    public String nullUnmarkedAnnotation = "org.jspecify.annotations.NullUnmarked";
    public boolean inferenceActivated = true;
    public boolean useCacheImpact = false;
    public Set<SourceType> sourceTypes = new HashSet<>();
    public int depth = 1;
    public String checker;
    public ParserConfiguration.LanguageLevel languageLevel;

    public void write(Path path) {
      Preconditions.checkNotNull(
          buildCommand, "Build command must be initialized to construct the context.");
      Preconditions.checkNotNull(
          initializerAnnotation,
          "Initializer Annotation must be initialized to construct the context.");
      Preconditions.checkNotNull(
          outputDir, "Output Directory must be initialized to construct the context.");
      Preconditions.checkNotNull(
          nullableAnnotation, "Nullable Annotation must be initialized to construct the context.");
      JsonObject json = new JsonObject();
      json.addProperty("BUILD_COMMAND", buildCommand);
      json.addProperty("CHECKER", checker);
      JsonObject annotation = new JsonObject();
      annotation.addProperty("INITIALIZER", initializerAnnotation);
      annotation.addProperty("NULLABLE", nullableAnnotation);
      annotation.addProperty("NULL_UNMARKED", nullUnmarkedAnnotation);
      json.add("ANNOTATION", annotation);
      json.addProperty("OUTER_LOOP", outerLoopActivation);
      json.addProperty("OUTPUT_DIR", outputDir);
      json.addProperty("CHAIN", chain);
      json.addProperty("PARALLEL_PROCESSING", useParallelProcessor);
      json.addProperty("CACHE_IMPACT_ACTIVATION", useCacheImpact);
      json.addProperty("CACHE", cache);
      json.addProperty("BAILOUT", bailout);
      json.addProperty("DEPTH", depth);
      json.addProperty("EXHAUSTIVE_SEARCH", exhaustiveSearch);
      json.addProperty("REDIRECT_BUILD_OUTPUT_TO_STDERR", redirectBuildOutputToStdErr);
      json.addProperty("SUPPRESS_REMAINING_ERRORS", suppressRemainingErrors);
      json.addProperty("INFERENCE_ACTIVATION", inferenceActivated);
      json.addProperty("LANGUAGE_LEVEL", languageLevel.name().split("_")[1]);
      JsonArray configPathsJson = new JsonArray();
      configPaths.forEach(
          info -> {
            JsonObject res = new JsonObject();
            res.addProperty("CHECKER", info.checkerConfig.toString());
            res.addProperty("SCANNER", info.scannerConfig.toString());
            configPathsJson.add(res);
          });
      json.add("CONFIG_PATHS", configPathsJson);
      JsonObject downstreamDependency = new JsonObject();
      downstreamDependency.addProperty("ACTIVATION", downStreamDependenciesAnalysisActivated);
      if (downStreamDependenciesAnalysisActivated) {
        Preconditions.checkNotNull(
            nullawayLibraryModelLoaderPath,
            "nullawayLibraryModelLoaderPath cannot be null to enable down stream dependency analysis.");
        Preconditions.checkArgument(
            !mode.equals(AnalysisMode.LOCAL),
            "Cannot perform downstream dependencies analysis with mode: \"Local\", use one of [default|lower_bound|upper_bound].");
        downstreamDependency.addProperty(
            "LIBRARY_MODEL_LOADER_PATH", nullawayLibraryModelLoaderPath.toString());
        Preconditions.checkNotNull(downstreamBuildCommand);
        downstreamDependency.addProperty("BUILD_COMMAND", downstreamBuildCommand);
        downstreamDependency.addProperty("ANALYSIS_MODE", mode.name());
      }
      json.add("DOWNSTREAM_DEPENDENCY_ANALYSIS", downstreamDependency);
      JsonObject processors = new JsonObject();
      sourceTypes.forEach(
          sourceType -> {
            JsonObject st = new JsonObject();
            st.addProperty("ACTIVATION", true);
            processors.add(sourceType.name(), st);
          });
      json.add("PROCESSORS", processors);
      try (BufferedWriter file =
          Files.newBufferedWriter(path.toFile().toPath(), Charset.defaultCharset())) {
        file.write(json.toString());
      } catch (IOException e) {
        System.err.println("Error happened in writing context json: " + e);
      }
    }
  }
}
