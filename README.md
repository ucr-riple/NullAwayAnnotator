## NullAwayAnnotator  ![Build Status](https://github.com/nimakarimipour/NullAwayAnnotator/actions/workflows/continuous-integration.yml/badge.svg)
```NullAwayAnnotator``` or simply (Annotator) is a tool that can automatically infer nullability types in the given source code and injects the 
corresponding annotations to pass [NullAway](https://github.com/uber/NullAway) checks.

Applying NullAway to build systems will require manual effort of applying annotations to source code.
Even if a code is free of nullability errors, it is still required to annotate the code to pass NullAway checks. 
A tool that can automatically infer types in the source code and inject the corresponding annotations to pass NullAway checks, can significantly reduce the effort of applying NullAway to build systems.

```Annotator``` minimizes the number of reported nullaway errors by inferring nullability types of elements in the source code and injecting 
the corresponding annotations. For errors that are not resolvable with any annotations, annotator injects appropriate suppression annotations.
The final output of Annotator, is a source code that passes NullAway checks leaving no remaining errors.

## Code Example

In the code below, ```NullAway``` will report five warnings.

```java
class Test{
    Object f1 = null; // warning: assigning @Nullable expression to @NonNull field
    Object f2 = null; // warning: assigning @Nullable expression to @NonNull field
    Object f3 = null; // warning: assigning @Nullable expression to @NonNull field
    Object f4 = null; // warning: assigning @Nullable expression to @NonNull field
    Object f5 = f4;
    Object f6 = new Object();
    
    String m1(){
        return f1 != null ? f1.toString() : f2.toString() + f6.toString();
    }
    
    int m2(){
        return f3 != null ? f3.hashCode() : f2.hashCode() + f6.hashCode();
    }
    
    Object m3(){
        return f5;
    }
    
    void m4(){
         f6 = null; // warning: assigning @Nullable expression to @NonNull field
    }
}
```

```Annotator``` can infer the nullable types in the code above and injects the corresponding annotations. For unresolved errors, suppression annotations are injected.
The output below is the result of running ```Annotator``` on the code above.

```java
import javax.annotation.Nullable; // added by Annotator
import org.jspecify.annotations.NullUnmarked; // added by Annotator

class Test{
    @Nullable Object f1 = null;
    @SuppressWarnings("NullAway") Object f2 = null; // inferred to be @Nonnull, and null assignment is suppressed.
    @Nullable Object f3 = null;
    @Nullable Object f4 = null;
    @Nullable Object f5 = f4;
    Object f6 = new Object();  // inferred to be @Nonnull
    
    String m1(){
        return f1 != null ? f1.toString() : f2.toString() + f6.toString();
    }
    
    int m2(){
        return f3 != null ? f3.hashCode() : f2.hashCode() + f6.hashCode();
    }
    
    @Nullable Object m3(){ // inferred to be @Nullable as a result of f5 being @Nullable.
        return f5;
    }
    
    @NullUnmarked //f6 is inferred to be @Nonnull, but it is assigned to null. The error is suppressed by @NullUnmarked.
    void m4(){
         f6 = null; 
    }
}
```

```Annotator``` propagates effects of a change through the entire module and injects several followups annotations to fully resolve one specific warning.
Annotator is also capable of processing modules within monorepos considering modules public APIs and the impacts of annotations on downstream dependencies for better results.

## Installation
No installation is required, annotator jar file is provided via maven central repository. Find the artifact id below:
```
GROUP: edu.ucr.cs.riple.annotator
ID: annotator-core
ID: annotator-scanner
```

## NullAway Compatibility

- `Annotator` version `1.3.6` is compatible with `NullAway` version `0.10.10` and above.

## Running Annotator

This sections describes how to run `Annotator` on any project.

### Requirements for the Target Project

Below are the instructions to prepare the target project:

#### Dependencies
1. `NullAway` checker must be activated with version >= `0.10.10`
2. `AnnotatorScanner` checker must be activated with version >= `1.3.6`, see more about `AnnotatorScanner` [here](../type-annotator-scanner/README.md).

#### Error Prone Flags
```
"-Xep:NullAway:ERROR", // to activate NullAway
"-XepOpt:NullAway:SerializeFixMetadata=true",
"-XepOpt:NullAway:FixSerializationConfigPath=path_to_nullaway_config.xml",
"-Xep:AnnotatorScanner:ERROR", // to activate Annotator AnnotatorScanner
"-XepOpt:AnnotatorScanner:ConfigPath=path_to_scanner_config.xml",
```

`path_to_nullaway_config.xml` and `path_to_scanner_config.xml` are context files which **are not necessary** to create at the time of preparing the project.
These two files will be created by the script and enables the communication between the script and the analysis.

Please find a sample project setup below:
```groovy
dependencies {
    annotationProcessor "edu.ucr.cs.riple:nullaway:0.9.6"
    annotationProcessor "edu.ucr.cs.riple.annotator:annotator-scanner:1.3.5"
    compileOnly "com.google.code.findbugs:jsr305:3.0.2"
    errorprone "com.google.errorprone:error_prone_core:2.3.2"
    errorproneJavac "com.google.errorprone:javac:9+181-r4173-1"
}

tasks.withType(JavaCompile) {
    if (!name.toLowerCase().contains("test")) {
        options.errorprone.errorproneArgs += ["-XepDisableAllChecks",
                                              "-Xep:NullAway:ERROR",
                                              "-XepOpt:NullAway:AnnotatedPackages=com.uber",
                                              "-XepOpt:NullAway:SerializeFixMetadata=true",
                                              "-XepOpt:NullAway:FixSerializationConfigPath=/tmp/NullAwayFix/context.xml",
                                              "-Xep:AnnotatorScanner:ERROR",
                                              "-XepOpt:AnnotatorScanner:ConfigPath=/tmp/NullAwayFix/scanner.xml",
        ]
    }
}
```
At this moment, the target project is ready for the Annotator to process.

We need to inform the `Annotator` about `path_to_nullaway_config.xml` and `path_to_scanner_config.xml` (number 2 and 3 in the next section), please read the section below.

### Running Annotator

`Annotator` requires certain flag values to be able to run successfully. We can pass these values via command line arguments or a context file, we will describe each approach in next sections.
#### Use Command Line Arguments

In order to run `Annotator` on target project `P`, arguments below **must** be passed to `Annotator`:
1. `-bc,--build-command <arg>`: Command to run `NullAway` on target `P` enclosed in **""**. Please note that this command should be executable from any directory (e.g. `"cd /Absolute /Path /To /P && ./build"`).
2. `-i,--initializer <arg>`: Fully qualified name of the `@Initializer` annotation.
3. `-d,--dir <arg>`: Directory where all outputs of `AnnotatorScanner|NullAway` are serialized.
4. `-cp, --context-paths`: Path to a tsv file containing values defined in [Error Prone](./README.md#Error-Prone-Flags) context paths given in the format: (`path_to_nullaway_config.xml \t path_to_scanner_config`)

By default, `Annotator` has the configuration below:

1. When a tree of fixes is marked as useful, it only injects the root fix.
2. Annotator will bailout from the search tree as soon as its effectiveness hits zero or less.
3. Performs search to depth level `5`.
4. Uses `javax.annotation.Nullable` as `@Nullable` annotation.
5. Cache reports results and uses them in the next cycles.
6. Parallel processing of fixes is enabled.
7. Downstream dependency analysis is disabled.

Here are __optional__ arguments which can alter default configurations above:
1. `-ch,--chain`: Injects the complete tree of fixes associated to the fix.
2. `-db,--disable-bailout`: Annotator will not bailout from the search tree as soon as its effectiveness hits zero or less and completely traverses the tree until no new fix is suggested.
3. `-depth,--depth <arg>`: Sets the depth of the analysis search.
4. `-n,--nullable <arg>`: Sets custom `@Nullable` annotation.
5. `-dc,--disable-cache`: Disables cache usage.
6. `-dpp,--disable-parallel-processing`: Disables parallel processing of fixes within an iteration.
7. `-rboserr, --redirect-build-output-stderr`: Redirects Build outputs to `STD Err`.
8. `-exs, --exhaustive-search`: Annotator will perform an exhaustive search which it injects `@Nullable` on all elements involved in an error regardless of their overall effectiveness. (This feature is used mostly in experiments and may not have a practical use.)
9. `-dol`, `--disbale-outer-loop`: Disables Outer Loop (This feature is used mostly in experiments and may not have a practical use.)
10. `-adda`, `--activate-downstream-dependencies-analysis`: Activates downstream dependency analysis.
11. `-ddbc`, `--downstream-dependencies-build-command <arg>`: Command to build all downstream dependencies at once, this command must include changing directory from root to the target project.
12. `-nlmlp`, `--nullaway-library-model-loader-path <arg>`: NullAway Library Model loader path.
13. `-fr`, `--force-resolve <arg>`: Forces remaining unresolved errors to be silenced using suppression annotations. Fully qualified annotation name for `@NullUnmarked` must be passed.
14. `-am`, `--analysis-mode <arg>`: Analysis mode. Can be [default|upper_bound|lower_bound|strict]
15. `-di`, `--deactivate-infere`: Disabled inference of `@Nullable` annotation.
16. `-drdl`, `--deactivate-region-detection-lombok`: Deactivates region detection for Lombok.
17. `-nna`, `--nonnull-annotations <arg>`: Adds a list of nonnull annotations separated by comma to be acknowledged by Annotator (e.g. com.example1.Nonnull,com.example2.Nonnull)
18. `eic`, `enable-impact-cache`: Enables fixes impacts caching for next cycles.


#### Use context file

In this approach we will initialize all flag values in one single file, and pass the path to the `Annotator`.
See the format of the context file below with sample values:
```json
{
  "BUILD_COMMAND": "cd /path/to/target && command to run javac with analysis (e.g. ./gradlew :p:compileJava)",
  "ANNOTATION": {
    "INITIALIZER": "com.example.Initializer",
    "NULLABLE": "javax.annotation.Nullable",
    "NULL_UNMARKED": "com.example.NullUnmarked",
    "NONNULL": [
        "com.example.Nonnull"
    ]
  },
  "OUTPUT_DIR": "/tmp/NullAwayFix",
  "CHAIN": false,
  "PARALLEL_PROCESSING": true,
  "CACHE_IMPACT_ACTIVATION": false,
  "CACHE": true,
  "BAILOUT": true,
  "REDIRECT_BUILD_OUTPUT_TO_STDERR": false,
  "EXHAUSTIVE_SEARCH": false,
  "OUTER_LOOP": true,
  "CONFIG_PATHS": [
    {
      "NULLAWAY": "path_to_nullaway_config.xml",
      "SCANNER": "path_to_scanner_config.xml"
    }
  ],
  "PROCESSORS": {
    "LOMBOK": true
  },
  "DEPTH": 1,
  "DOWNSTREAM_DEPENDENCY": {
    "ACTIVATION":true,
    "BUILD_COMMAND": "cd /path/to/dependencies && command to run javac with analysis (e.g. ./gradlew :p:compileJava)",
    "LIBRARY_MODEL_LOADER_PATH": "path to nullaway library model loader jar",
    "ANALYSIS_MODE": "default"
  },
  "FORCE_RESOLVE": false,
  "INFERENCE_ACTIVATION": true
}
```
Below is the description of each setting:

1. `BUILD_COMMAND`: The command to execute `NullAway` for the project. (This command must include changing directory to target project from root)
2. `INITIALIZER`: Fully qualified name of the `Initializer` annotation to inject on detected initializer methods.
3. `NULLABLE`: Fully qualified name of the `Nullable` annotation.
4. `OUTPUT_DIR`: Directory where the serialized output of NullAway should be written.
5. `CONFIG_PATHS`: Array of JSON objects where each contains the path to nullaway and scanner Error Prone checker. The first element in this array will be considered as the target module and rest as downstream dependencies.
6. `PARALLEL_PROCESSING`: Enables parallel processing of fixes within an iteration.
7. `CACHE_IMPACT_ACTIVATION`: Enables fixes impacts caching for next cycles.
8. `CACHE`: if set to `true`, cache usage will be enabled.
9. `BAILOUT`: if set to `true`, Annotator will bailout from the search tree as soon as its effectiveness hits zero or less, otherwise it will completely travers the tree until no new fix is suggested
10. `DEPTH`: The depth of the analysis.
11. `REDIRECT_BUILD_OUTPUT_TO_STDERR`: If true, build output will be redirected to `STD Error`.
12. `EXHAUSTIVE_SEARCH`: If true, the exhaustive search will be activated.
13. `OUTER_LOOP`: If true, the outer loop will be enabled.
14. `DOWNSTREAM_DEPENDENCY:ACTIVATION`: Controls downstream dependency feature activation.
15. `DOWNSTREAM_DEPENDENCY:BUILD_COMMAND`: Single command to build all downstream dependencies.
16. `DOWNSTREAM_DEPENDENCY:LIBRARY_MODEL_LOADER_PATH`: Path to NullAway library models loader.
17. `DOWNSTREAM_DEPENDENCY:ANALYSIS_MODE`: Analysis mode for downstream dependencies. Can be [default|upper_bound|lower_bound|strict]
18. `FORCE_RESOLVE`: If true, remaining unresolved errors will be silenced using suppression annotations.
19. `INFERENCE_ACTIVATION`: If true, inference of `@Nullable` annotation will be activated.
20. `PROCESSORS:LOMBOK:ACTIVATION`: If true, potentially impacted regions will be extended with generated code by Lombok.

Pass the path to the context file above with `-p,--path` argument to `core.jar` and no other flag is required.


To see all flags description simply run the jar with `--help`.
