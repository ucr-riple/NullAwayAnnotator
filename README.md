
## NullAwayAnnotator  ![Build Status](https://github.com/nimakarimipour/NullAwayAnnotator/actions/workflows/continuous-integration.yml/badge.svg)

`NullAwayAnnotator`, or simply `Annotator`, is a tool that automatically infers nullability types in the given source code and injects the corresponding annotations to pass [NullAway](https://github.com/uber/NullAway) checks.

Applying NullAway to build systems requires manual effort in annotating the source code. Even if the code is free of nullability errors, annotations are still needed to pass NullAway checks. A tool that can automatically infer types in the source code and inject the corresponding annotations to pass NullAway checks can significantly reduce the effort of integrating NullAway into build systems. 

`Annotator` minimizes the number of reported NullAway errors by inferring nullability types of elements in the source code and injecting the corresponding annotations. For errors that are not resolvable with any annotations, Annotator injects appropriate suppression annotations. The final output of Annotator is a source code that passes NullAway checks with no remaining errors.

## Code Example

In the code below, `NullAway` reports five warnings.

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

`Annotator` can infer the nullable types in the code above and inject the corresponding annotations. For unresolved errors, suppression annotations are injected.
The output below shows the result of running `Annotator` on the code above.

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

`Annotator` propagates the effects of a change throughout the entire module and injects several follow-up annotations to fully resolve a specific warning.
It is also capable of processing modules within monorepos, taking into account the modules public APIs and the impacts of annotations on downstream dependencies for improved results.
## Installation
We ship Annotator on [Maven](https://repo.maven.apache.org/maven2/edu/ucr/cs/riple/annotator/annotator-core/1.3.6/annotator-core-1.3.6.jar), as a JAR. You can find the artifact information below - 
```
GROUP: edu.ucr.cs.riple.annotator
ID: annotator-core
ID: annotator-scanner
```

## Using Annotator on a target Java Project

This sections describes how to run `Annotator` on any project.

 - ### Requirements for the Target Project

	#### Dependencies
	-  `NullAway` checker must be activated with version >= `0.10.10`
	- `AnnotatorScanner` checker must be activated with version >= `1.3.6`, see more about `AnnotatorScanner` [here](../type-annotator-scanner/README.md).

	#### Error Prone Flags
	Since Nullaway is built as a plugin for [Error Prone](https://github.com/google/error-prone), we need to set the following flags in our build.gradle, 
	```
	"-Xep:NullAway:ERROR", // to activate NullAway
	"-XepOpt:NullAway:SerializeFixMetadata=true",
	"-XepOpt:NullAway:FixSerializationConfigPath=path_to_nullaway_config.xml",
	"-Xep:AnnotatorScanner:ERROR", // to activate Annotator AnnotatorScanner
	"-XepOpt:AnnotatorScanner:ConfigPath=path_to_scanner_config.xml",
	```


	

	The following code snippet demonstrates how to configure the `JavaCompile` tasks in your `build.gradle` to use NullAway as a plugin for [Error Prone](https://github.com/google/error-prone):
	```groovy
	dependencies {  
		annotationProcessor 'edu.ucr.cs.riple.annotator:annotator-scanner:1.3.6'  
		annotationProcessor "com.uber.nullaway:nullaway:0.10.10"  
		errorprone "com.google.errorprone:error_prone_core:2.4.0"  
		errorproneJavac "com.google.errorprone:javac:9+181-r4173-1"  
		//All other target project dependencies
	}  
	  
	  
	tasks.withType(JavaCompile) {  
		// remove the if condition if you want to run NullAway on test code  
		if (!name.toLowerCase().contains("test")) {  
			options.errorprone {  
			check("NullAway", CheckSeverity.ERROR)  
			check("AnnotatorScanner", CheckSeverity.ERROR)  
			option("NullAway:AnnotatedPackages", "org.example")  
			option("NullAway:SerializeFixMetadata", "true")  
			option("NullAway:FixSerializationConfigPath", "path_to/nullaway.xml")  
			option("AnnotatorScanner:ConfigPath", "path_to/scanner.xml")  
			}  
			options.compilerArgs << "-Xmaxerrs"<< "100000"  
			options.compilerArgs << "-Xmaxwarns" << "100000"  
		}  
	  
	}
	```
	`path_to_nullaway_config.xml` and `path_to_scanner_config.xml` are configuration files that **do not need to be created** during the initial project setup. The script will generate these files, facilitating seamless communication between the script and the analysis. At this point, the target project is prepared for the Annotator to process.
	
	You must provide the Annotator with the paths to `path_to_nullaway_config.xml` and `path_to_scanner_config.xml`. Further details on this process are described in the sections below.

- ### Running Annotator


	`Annotator` necessitates specific flag values for successful execution. You can provide these values through command line arguments or a configuration file. The following sections detail each approach.

	#### Approach 1: Using the CLI options - 

	To run `Annotator` on the target project `P`, the arguments below **must** be passed to `Annotator`:

	| Flag | Description |
	|------|-------------|
	| `-bc,--build-command <arg>` | Command to run `NullAway` on target `P` enclosed in **""**. Please note that this command should be executable from any directory (e.g., `"cd /Absolute/Path/To/P && ./build"`). |
	| `-i,--initializer <arg>` | Fully qualified name of the `@Initializer` annotation. |
	| `-d,--dir <arg>` | Directory where all outputs of `AnnotatorScanner` and `NullAway` are serialized. |
	| `-cp, --config-paths` | Path to a TSV file containing values defined in [Error Prone](./README.md#Error-Prone-Flags) config paths given in the format: (`path_to_nullaway_config.xml \t path_to_scanner_config`). |
	
	
By default, `Annotator` has the configuration below:

1. When a tree of fixes is marked as useful, it only injects the root fix.
2. Annotator will bailout from the search tree as soon as its effectiveness hits zero or less.
3. Performs search to depth level `5`.
4. Uses `javax.annotation.Nullable` as `@Nullable` annotation.
5. Cache reports results and uses them in the next cycles.
6. Parallel processing of fixes is enabled.
7. Downstream dependency analysis is disabled.


Here are the __optional__ arguments that can alter the default configurations mentioned above:

| Flag | Description |
|------|-------------|
| `-ch,--chain` | Injects the complete tree of fixes associated with the fix. |
| `-db,--disable-bailout` | Annotator will not bail out from the search tree as soon as its effectiveness hits zero or less and will completely traverse the tree until no new fix is suggested. |
| `-depth,--depth <arg>` | Sets the depth of the analysis search. |
| `-n,--nullable <arg>` | Sets custom `@Nullable` annotation. |
| `-dc,--disable-cache` | Disables cache usage. |
| `-dpp,--disable-parallel-processing` | Disables parallel processing of fixes within an iteration. |
| `-rboserr, --redirect-build-output-stderr` | Redirects build outputs to `STD Err`. |
| `-exs, --exhaustive-search` | Annotator will perform an exhaustive search, injecting `@Nullable` on all elements involved in an error regardless of their overall effectiveness. (This feature is used mostly in experiments and may not have a practical use.) |
| `-dol, --disable-outer-loop` | Disables outer loop (This feature is used mostly in experiments and may not have a practical use.) |
| `-adda, --activate-downstream-dependencies-analysis` | Activates downstream dependency analysis. |
| `-ddbc, --downstream-dependencies-build-command <arg>` | Command to build all downstream dependencies at once; this command must include changing the directory from root to the target project. |
| `-nlmlp, --nullaway-library-model-loader-path <arg>` | NullAway Library Model loader path. |
| `-fr, --force-resolve <arg>` | Forces remaining unresolved errors to be silenced using suppression annotations. Fully qualified annotation name for `@NullUnmarked` must be passed. |
| `-am, --analysis-mode <arg>` | Analysis mode. Can be [default|upper_bound|lower_bound|strict] |
| `-di, --deactivate-infere` | Disables inference of `@Nullable` annotation. |
| `-drdl, --deactivate-region-detection-lombok` | Deactivates region detection for Lombok. |
| `-nna, --nonnull-annotations <arg>` | Adds a list of non-null annotations separated by a comma to be acknowledged by Annotator (e.g., com.example1.Nonnull,com.example2.Nonnull) |
| `eic, enable-impact-cache` | Enables fixes impacts caching for next cycles. |


Here is a template command you can use to run Annotator from the CLI, using CLI options- 
```bash
java -jar ./path/to/annotator-core.jar -d "/path/to/output/directory" -cp "/path/to/config/paths.tsv" -i com.example.Initializer -bc "cd /path/to/targetProject && ./gradlew build -x test"
```


#### Approach 2: Use config.json file
In this approach, you will initialize all flag values in a single file and pass the path to the `Annotator`. Below is the format of the configuration file with sample values:

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
    "ACTIVATION": true,
    "BUILD_COMMAND": "cd /path/to/dependencies && command to run javac with analysis (e.g. ./gradlew :p:compileJava)",
    "LIBRARY_MODEL_LOADER_PATH": "path to nullaway library model loader jar",
    "ANALYSIS_MODE": "default"
  },
  "FORCE_RESOLVE": false,
  "INFERENCE_ACTIVATION": true
}
```
Here is a template command you can use to run Annotator from the CLI, using config.json - 
```bash
java -jar /path/to/annotator-core.jar -p path_to/config.json
```
Provide the path to the configuration file above with the `-p,--path` argument to `core.jar`, and no other flags are needed.

To view descriptions of all flags, simply run the JAR with the `--help` option.

## NullAway Compatibility

- `Annotator` version `1.3.6` is compatible with `NullAway` version `0.10.10` and above.


## Features 

- Automatically infers nullability types and injects corresponding annotations
- Injects suppression annotations for unresolved NullAway errors
- Minimizes the number of reported NullAway errors
- Reduces manual effort when applying NullAway to build systems


## Contributing 

We welcome contributions to improve `Annotator`. Please follow these guidelines when submitting a pull request:

1. Fork the repository and create your branch from `master`.
2. If you've added code that should be tested, add tests.
3. Ensure the test suite passes.
4. Make sure your code lints.
5. Update documentation if necessary.

## License 

`Annotator` is released under the MIT License. See the [LICENSE](LICENSE) file for more information.