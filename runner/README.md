## Runner
This sections describes how to run [Annotator](../README.md) on any project.

### Installation
Annotator is written entirely in Java and delivered via a `jar` file located in `jars` directory. 
To re-create/update the `jar` file, run `./update.sh`

### Requirements for the Target Project

Below are the instructions to prepare the target project:

#### Dependencies
1. `NullAway` checker must be activated with version >= `0.9.7`
2. `TypeAnnotatorScanner` checker must be activated with version >= `1.2.6-LOCAL`, see more about `TypeAnnotatorScanner` [here](../type-annotator-scanner/README.md).

#### Error Prone Flags
```
"-Xep:NullAway:ERROR", // to activate NullAway
"-XepOpt:NullAway:SerializeFixMetadata=true",
"-XepOpt:NullAway:FixSerializationConfigPath=path_to_nullaway_config.xml",
"-Xep:TypeAnnotatorScanner:ERROR", // to activate Annotator TypeAnnotatorScanner
"-XepOpt:TypeAnnotatorScanner:ConfigPath=path_to_scanner_config.xml",
```

`path_to_nullaway_config.xml` and `path_to_scanner_config.xml` are config files which **are not necessary** to create at the time of preparing the project. 
These two files will be created by the script and enables the communication between the script and the analysis.

Please find a sample project setup below:
```groovy
dependencies {
    annotationProcessor "edu.ucr.cs.riple:nullaway:0.9.6"
    annotationProcessor "edu.ucr.cs.riple.nullawayannotator:type-annotator-scanner:1.1.1-LOCAL"
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
                                              "-XepOpt:NullAway:FixSerializationConfigPath=/tmp/NullAwayFix/config.xml",
                                              "-Xep:TypeAnnotatorScanner:ERROR",
                                              "-XepOpt:TypeAnnotatorScanner:ConfigPath=/tmp/NullAwayFix/scanner.xml",
        ]
    }
}
```
At this moment, the target project is ready for the Annotator to process. 

We need to inform the `Annotator` about `path_to_nullaway_config.xml` and `path_to_scanner_config.xml` (number 2 and 3 in the next section), please read the section below.

### Running Annotator
`Annotator` is delivered via a `jar` file. To run the `jar` file simply run the command below:
```shell
cd jars && java -jar core.jar
```
`Annotator` requires certain flag values to be able to run successfully. We can pass these values via command line arguments or config files, we will describe each approach in next sections.
#### Use Command Line Arguments

In order to run `Annotator` on target project `P`, arguments below **must** be passed to `Annotator`:
1. `-bc,--build-command <arg>`: Command to run `NullAway` on target `P` enclosed in **""**. Please note that this command should be executable from any directory (e.g. `"cd /Absolute /Path /To /P && ./build"`).
2. `-i,--initializer <arg>`: Fully qualified name of the `@Initializer` annotation.
3. `-d,--dir <arg>`: Directory where all outputs of `TypeAnnotatorScanner|NullAway` are serialized.
4. `-cp, --config-paths`: Path to a tsv file containing values defined in [Error Prone](./README.md#Error-Prone-Flags) config paths given in the format: (`path_to_nullaway_config.xml \t path_to_scanner_config`)

By default, `Annotator` has the configuration below:
1. Lexical Preservation is enabled.
2. When a tree of fixes is marked as useful, it only injects the root fix.
3. Annotator will bailout from the search tree as soon as its effectiveness hits zero or less.
4. Performs search to depth level `5`.
5. Uses `javax.annotation.Nullable` as `@Nullable` annotation.
6. Caches iterations results and uses them in the next cycles.
7. Uses optimization techniques to parallelize search.
8. Downstream dependency analysis is disabled.

Here are __optional__ arguments which can alter default configurations above:
1. `-dlp,--disable-lexical-preservation`: Disables Lexical Preservation.
2. `-ch,--chain`: Injects the complete tree of fixes associated to the fix.
3. `-db,--disable-bailout`: Annotator will not bailout from the search tree as soon as its effectiveness hits zero or less and completely traverses the tree until no new fix is suggested.
4. `-depth,--depth <arg>`: Sets the depth of the analysis search.
5. `-n,--nullable <arg>`: Sets custom `@Nullable` annotation.
6. `-dc,--disable-cache`: Disables cache usage.
7. `-do,--disable-optimization`: Disables optimizations.
8. `-rboserr, --redirect-build-output-stderr`: Redirects Build outputs to `STD Err`.
9. `-exs, --exhaustive-search`: Annotator will perform an exhaustive search which it injects `@Nullable` on all elements involved in an error regardless of their overall effectiveness. (This feature is used mostly in experiments and may not have a practical use.)
10. `-dol`, `--disbale-outer-loop`: Disables Outer Loop (This feature is used mostly in experiments and may not have a practical use.)
11. `-adda`, `--activate-downstream-dependencies-analysis`: Activates downstream dependency analysis.
12. `-ddbc`, `--downstream-dependencies-build-command`: Command to build all downstream dependencies at once, this command must include changing directory from root to the target project.
13. `-nlmlp`, `--nullaway-library-model-loader-path`: NullAway Library Model loader path.

#### Example
```shell
cd jars && java -jar core.jar -bc "cd /Path /To /P && ./gradlew compileJava" -cp path_to_configs.tsv -i com.custom.Initializer -d /tmp --disable-optimization -dlp
```

The command above will process project `P` in non-optimized mode and disables lexical preservation features.


#### Use config file

In this approach we will initialize all flag values in one single file, and pass the path to the `Annotator`.
See the format of the config file below with sample values:
```json
{
  "BUILD_COMMAND": "cd /path/to/target && command to run javac with analysis (e.g. ./gradlew :p:compileJava)",
  "ANNOTATION": {
    "INITIALIZER": "com.badlogic.gdx.Initializer",
    "NULLABLE": "javax.annotation.Nullable"
  },
  "LEXICAL_PRESERVATION": true,
  "OUTPUT_DIR": "/tmp/NullAwayFix",
  "CHAIN": false,
  "OPTIMIZED": true,
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
  "DEPTH": 1,
  "DOWNSTREAM_DEPENDENCY": {
    "ACTIVATION":true,
    "BUILD_COMMAND": "cd /path/to/dependencies && command to run javac with analysis (e.g. ./gradlew :p:compileJava)",
    "LIBRARY_MODEL_LOADER_PATH": "path to nullaway library model loader jar"
  }
}
```
Below is the description of each setting:

1. `BUILD_COMMAND`: The command to execute `NullAway` for the project. (This command must include changing directory to target project from root) 
2. `INITIALIZER`: Fully qualified name of the `Initializer` annotation to inject on detected initializer methods.
3. `NULLABLE`: Fully qualified name of the `Nullable` annotation.
4. `LEXICAL_PRESERVATION`: If set to `true`, activates lexical preservation.
5. `OUTPUT_DIR`: Directory where the serialized output of NullAway should be written.
6. `CONFIG_PATHS`: Array of JSON objects where each contains the path to nullaway and scanner Error Prone checker. The first element in this array will be considered as the target module and rest as downstream dependencies.
7. `OPTIMIZED`: Enables the optimization technique.
8. `CACHE`: if set to `true`, cache usage will be enabled.
9. `BAILOUT`: if set to `true`, Annotator will bailout from the search tree as soon as its effectiveness hits zero or less, otherwise it will completely travers the tree until no new fix is suggested
10. `DEPTH`: The depth of the analysis.
11. `REDIRECT_BUILD_OUTPUT_TO_STDERR`: If true, build output will be redirected to `STD Error`.
12. `EXHAUSTIVE_SEARCH`: If true, the exhaustive search will be activated.
13. `OUTER_LOOP`: If true, the outer loop will be enabled.
14. `DOWNSTREAM_DEPENDENCY:ACTIVATION`: Controls downstream dependency feature activation.
15. `DOWNSTREAM_DEPENDENCY:BUILD_COMMAND`: Single command to build all downstream dependencies.
16. `DOWNSTREAM_DEPENDENCY:LIBRARY_MODEL_LOADER_PATH`: Path to NullAway library models loader.

Pass the path to the config file above with `-p,--path` argument to `core.jar` and no other flag is required.

#### Example
```shell
cd jars && java -jar core.jar --path config.json
```


### Helper script

`start.sh` script is provided, you can run `Annotator` and simply pass desired arguments to `start.sh`.
#### Example
```shell
./start.sh --path config.json

or

./start.sh -bc "cd /Path /To /P && ./gradlew compileJava" -cp path_to_configs.tsv -i com.custom.Initializer -d /tmp --disable-optimization -dlp
```

To see all flags description simply run `./start.sh --help`
```cmd
usage: Annotator config Flags
 -adda,--activate-downstream-dependencies-analysis     Activates
                                                       downstream
                                                       dependency analysis
 -bc,--build-command <arg>                             Command to build
                                                       the target project,
                                                       this command must
                                                       include changing
                                                       directory from root
                                                       to the target
                                                       project
 -ch,--chain                                           Injects the
                                                       complete tree of
                                                       fixes associated to
                                                       the fix
 -d,--dir <arg>                                        Directory of the
                                                       output files
 -db,--disable-bailout                                 Disables bailout,
                                                       Annotator will not
                                                       bailout from the
                                                       search tree as soon
                                                       as its
                                                       effectiveness hits
                                                       zero or less and
                                                       completely
                                                       traverses the tree
                                                       until no new fix is
                                                       suggested
 -dc,--disable-cache                                   Disables cache
                                                       usage
 -ddbc,--downstream-dependencies-build-command <arg>   Command to build
                                                       all downstream
                                                       dependencies at
                                                       once, this command
                                                       must include
                                                       changing directory
                                                       from root to the
                                                       target project
 -depth,--depth <arg>                                  Depth of the
                                                       analysis
 -dlp,--disable-lexical-preservation                   Disables lexical
                                                       preservation
 -do,--disable-optimization                            Disables
                                                       optimizations
 -dol,--disable-outer-loop                             Disables Outer Loop
 -exs,--exhaustive-search                              Performs Exhaustive
                                                       Search
 -h,--help                                             Shows all flags
 -i,--initializer <arg>                                Fully Qualified
                                                       name of the
                                                       Initializer
                                                       annotation
 -n,--nullable <arg>                                   Fully Qualified
                                                       name of the
                                                       Nullable annotation
 -nlmlp,--nullaway-library-model-loader-path <arg>     NullAway Library
                                                       Model loader path
 -p,--path <arg>                                       Path to config file
                                                       containing all
                                                       flags values in
                                                       json format
 -rboserr,--redirect-build-output-stderr               Redirects Build
                                                       outputs to STD Err
 -x,--config-paths <arg>                               Path to tsv file
                                                       containing path to
                                                       nullaway and
                                                       scanner config
                                                       files.
```

### Annotator Depth Level

Regarding `Annotator Depth level`, the number of remaining warnings will reduce as the depth increases. However, in our experiments,
level 5 is the sweet spot for having the best performance. Please look at the chart below, running the core from level 0 to 10 over 20 open source projects. As you can see, on level 4 we reach the optimal solution.

![image info](./../pics/depth.png)


### Downstream Dependency Analysis

This feature is designed to help Annotator compute the effect of annotating public methods in downstream dependencies.
Annotator will use this info in its decision process and will prevent making changes that makes more errors on downstream dependencies than it helps to remove errors from the target module.

(Full instructions on How to use this feature will be available soon...)
