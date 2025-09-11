
## NullAwayAnnotator  ![Build Status](https://github.com/nimakarimipour/NullAwayAnnotator/actions/workflows/continuous-integration.yml/badge.svg)

`NullAwayAnnotator`, or simply `Annotator`, is a tool that automatically infers nullability types in the given source code and injects the corresponding annotations to pass [NullAway](https://github.com/uber/NullAway) checks.

Applying NullAway to build systems requires manual effort in annotating the source code.
Even if the code is free of nullability errors, annotations are still needed to pass NullAway checks. 
A tool that can automatically infer types in the source code
and inject the corresponding annotations to pass NullAway checks can significantly reduce the effort
of integrating NullAway into build systems.

`Annotator` minimizes the number of reported NullAway errors by inferring nullability types of elements in the source code and injecting the corresponding annotations. 
For errors that are not resolvable with any annotations, Annotator injects appropriate suppression annotations. 
The final output of Annotator is a source code that passes NullAway checks with no remaining errors.

## Code Example

In the code below, `NullAway` reports five warnings.

```java
package com.example;
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

`Annotator` can infer the nullable types in the code above and inject the corresponding annotations. 
For unresolved errors, suppression annotations are injected.
The output below shows the result of running `Annotator` on the code above.

```java
package com.example;
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
We ship [Annotator](https://repo.maven.apache.org/maven2/edu/ucr/cs/riple/annotator/) on [Maven](https://repo.maven.apache.org/) as a JAR. 
You can find the artifact information below:
```
GROUP: edu.ucr.cs.riple.annotator
ID: annotator-core
ID: annotator-scanner
```

## Using Annotator on a target Java Project

This sections describes how to run `Annotator` on any project.

### Requirements for the Target Project

#### Dependencies
- `NullAway` checker must be activated with a version >= `0.10.10`
- `AnnotatorScanner` checker must be activated with a version >= `1.3.6`, see more about `AnnotatorScanner` [here](../type-annotator-scanner/README.md).

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

    // add required annotation dependencies
    // Initializer
    compileOnly 'com.uber.nullaway:nullaway-annotations:0.10.10'
    // Nullable annotations
    compileOnly "com.google.code.findbugs:jsr305:3.0.2"
    // JSpecify annotations for NullUnmarked
    compileOnly "org.jspecify:jspecify:0.3.0"
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
	
You must provide the Annotator with the absolute paths to `path_to_nullaway_config.xml` and `path_to_scanner_config.xml`. 
Further details on this process are described in the sections below.

### Running Annotator
`Annotator` necessitates specific flag values for successful execution. You can provide these values through command line arguments.

To run `Annotator` on the target project `P`, the arguments below **must** be passed to `Annotator`:

| Flag | Description                                                                                                                                                                      |
|------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `-bc,--build-command <arg>` | Command to run `NullAway` on target `P` enclosed in **""**. Please note that this command should be executable from any directory (e.g., `"cd /Absolute/Path/To/P && ./build"`). |
| `-i,--initializer <arg>` | Fully qualified name of the `@Initializer` annotation.                                                                                                                           |
| `-d,--dir <arg>` | Absolute path of an **Empty** Directory where all outputs of `AnnotatorScanner` and `NullAway` are serialized.                                                                   |
| `-cp, --config-paths` | Path to a TSV file containing value of config paths given in the format: (`path_to_nullaway_config.xml \t path_to_scanner_config`).                                              |
| `-cn, --checker-name` | Checker name to be used for the analysis. (use `NULLAWAY` to request inference for NullAway.)                                                                                    |
| `-sre, --supress-remaning-errors` <arg> | Suppress remaining errors in the code with the given `@NullUnmared` annotation (e.g. `org.jspecify.annotations.NullUnmarked`)                                                    |

By default, `Annotator` has the configuration below:

1. When a tree of fixes is marked as useful, it only injects the root fix.
2. Annotator will bailout from the search tree as soon as its effectiveness hits zero or less.
3. Performs search to depth level `5`.
4. Uses `javax.annotation.Nullable` as `@Nullable` annotation.
5. Cache reports results and uses them in the next cycles.
6. Parallel processing of fixes is enabled.
7. Downstream dependency analysis is disabled.


Here are some useful __optional__ arguments that can alter the default configurations mentioned above:

| Flag                                                   | Description |
|--------------------------------------------------------|-------------|
| `-n,--nullable <arg>`                                  | Sets custom `@Nullable` annotation. |
| `-rboserr, --redirect-build-output-stderr`             | Redirects build outputs to `STD Err`. |


To learn more about all the __optional__ arguments, please refer to [OPTIONS.md](./OPTIONS.md)


Here is a template command you can use to run Annotator from the CLI, using CLI options-
```bash
curl -O https://repo.maven.apache.org/maven2/edu/ucr/cs/riple/annotator/annotator-core/1.3.15/annotator-core-1.3.15.jar 
java -jar annotator-core-1.3.15.jar \ 
    -bc "cd project && command_to_compile_target_project_using_javac" \
    -d "path_to_selected_annotator_out_dir" \
    -n javax.annotation.Nullable \
    -cp sample/annotator-out/paths.tsv \
    -cn NULLAWAY \
    -i com.uber.nullaway.annotations.Initializer \
    -sre org.jspecify.annotations.NullUnmarked
```

## Running Annotator on the [example project](#code-example)
The example in this readme is available in module `sample` in this project.
To run Annotator on the example project, you can use the following command:
```bash
./annotator-sample-command.sh
```
It will run annotator on the sample project and will produce the output shown in this readme.

To view descriptions of all flags, simply run the JAR with the `--help` option.

## NullAway Compatibility

- `Annotator` version `1.3.6` is compatible with `NullAway` version `0.10.10` and above.

## Annotator with Auto Fix Capability

To run the annotator with automatic fix support, use the flag -rrem ADVANCED.
You must also set an API key in the environment variable OPENAI_KEY. The annotator will use this key to access the OpenAI API and generate fixes for reported errors.

This feature additionally requires NullAway version `0.12.4-SNAPSHOT` from this [repp](https://github.com/nimakarimipour/NullAway.git) built at this commit: `d0c7d8390964ee5dc95d6bf93ae76b85913b2342`

## License

`Annotator` is released under the MIT License. See the [LICENSE](LICENSE) file for more information.
