## Runner
This sections describes how to run [Annotator](../README.md) on any project.

### Installation
Annotator is written entirely in Java and delivered via a `jar` file located in `jars` directory. 
To re-create/update the `jar` file, run `./update.sh`

### Requirements for the Target Project

Below are the instructions to prepare the target project:

#### Dependencies
1. `NullAway` checker must be activated with version >= `0.9.7`
2. `CSS` checker must be activated with version >= `1.2.5-LOCAL`, see more about `CSS` [here](../css/README.md).

#### Error Prone Flags
```
"-Xep:NullAway:ERROR", // to activate NullAway
"-XepOpt:NullAway:SerializeFixMetadata=true",
"-XepOpt:NullAway:FixSerializationConfigPath=path_to_nullaway_config.xml",
"-Xep:CSS:ERROR", // to activate CSS
"-XepOpt:CSS:ConfigPath=path_to_css_config.xml",
```

`path_to_nullaway_config.xml` and `path_to_css_config.xml` are config files which **are not necessary** to create at the time of preparing the project. 
These two files will be created by the script and enables the communication between the script and the analysis.

Please find a sample project setup below:
```groovy
dependencies {
    annotationProcessor "edu.ucr.cs.riple:nullaway:0.9.6"
    annotationProcessor "edu.ucr.cs.riple.nullawayannotator:css:1.1.1-LOCAL"
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
                                              "-Xep:CSS:ERROR",
                                              "-XepOpt:CSS:ConfigPath=/tmp/NullAwayFix/css.xml",
        ]
    }
}
```
At this moment, the target project is ready for the Annotator to process. 

We need to inform the `Annotator` about `path_to_nullaway_config.xml` and `path_to_css_config.xml` (number 2 and 3 in the next section), please read the section below.

### Running Annotator
`Annotator` is delivered via a `jar` file. To run the `jar` file simply run the command below:
```shell
cd jars && java -jar core.jar
```

### Command Line Arguments

In order to run `Annotator` on target project `P`, arguments below must be passed to `Annotator`:
1. `-bc,--build-command`: Command to run `NullAway` on target `P`. Please note that this command should be executable from any directory (e.g. `cd /Absolute /Path /To /P && ./build`).
2. `-ccp,--css-config-path`: Path to the `CSS` Config (value used in previous section (`path_to_css_config.xml`)).
3. `-ncp,--nullaway-config-path`: Path to the `NullAway` Config (value used in previous section (`path_to_nullaway_config.xml`)).
4. `-i,--initializer`: Fully qualified name of the `@Initializer` annotation.
5. `-d,--dir`: Directory where all outputs of `CSS|NullAway` are serialized.



### Script Config

Configurations are written inside the `config.json` file. Please find a sample below:
```json
{
  "BUILD_COMMAND": "cd Absolute/ Path/ to/ Target && Command to run NullAway",
  "ANNOTATION": {
    "INITIALIZER": "foo.bar.Initializer",
    "NULLABLE": "javax.annotation.Nullable"
  },
  "FORMAT": false,
  "OUTPUT_DIR": "/tmp/NullAwayFix",
  "NULLAWAY_CONFIG_PATH": "/tmp/NullAwayFix/config.xml",
  "CSS_CONFIG_PATH": "/tmp/NullAwayFix/css.xml",
  "CHAIN": true,
  "OPTIMIZED": true,
  "CACHE": true,
  "BAILOUT": true,
  "DEPTH": 1
}
```
Below is the description of each setting:
1. `PROJECT_PATH`: The path to the project directory (if a subproject needs to be analyzied, this path needs to point to the subproject not the root project)
2. `BUILD_COMMAND`: The command to execute `NullAway` for the project. (This command must include changing directory to target project from root) 
3. `INITIALIZER`: Fully qualified name of the `Initializer` annotation to inject on detected initializer methods.
4. `NULLABLE`: Fully qualified name of the `Nullable` annotation.
5. `FORMAT`: If set to `true` the format of the code will be preserved at the end of execution.
6. `OUTPUT_DIR`: Directory where the serialized output of NullAway should be written.
7. `NULLAWAY_CONFIG_PATH`: `path_to_config.xml` given to project in time of preparing the project (previous section).
8. `CSS_CONFIG_PATH`: `path_to_css.xml` given to project in time of preparing the process (previous section).
9. `OPTIMIZED`: Enables the optimization technique.
10. `CACHE`: if set to `true`, cache usage will be enabled.
11. `BAILOUT`: if set to `true`, Annotator will bailout from the search tree as soon as its effectiveness hits zero or less, otherwise it will completely travers the tree until no new fix is suggested
12. `DEPTH`: The depth of the analysis.

### Running the script

Before running, make sure that all the changes in the `Requirements for the Target Project` section has been applied to the target project.

There are two ways to run `Annotator`:
1. Run the `core.jar` directly with `java -jar core.jar`.
2. Run the helper script `start.sh` with `./start.sh`, requires the `core.jar` the relative path: `./jars/core.jar` to execute. To recreate/update the `core.jar` run `./update.sh`.

Both ways require config flags to be sent as command line arguments.

There are two possible ways to pass the command line arguments:
1. run the jar file and pass `--path or -p` followed by absolute path to `config.json` which contains all flag values.
2. run the jar file with default values and customize others with command line arguments.

Below is the list of all command line flags
```cmd
-bc,--build-command <arg>           Command to Run NullAway on the target
                                     project, this command must include
                                     changing directory from root to the
                                     target project
 -ccp,--css-config-path <arg>        Path to the CSS Config
 -ch,--chain                         Injects the complete tree of fixes
                                     associated to the fix
 -d,--dir <arg>                      Directory of the output files
 -db,--disable-bailout               Disables bailout, Annotator will not
                                     bailout from the search tree as soon
                                     as its effectiveness hits zero or
                                     less and completely traverses the
                                     tree until no new fix is suggested
 -dc,--disable-cache                 Disables cache usage
 -do,--disable-optimization          Disables optimizations
 -h,--help                           Shows all flags
 -i,--initializer <arg>              Fully Qualified name of the
                                     Initializer annotation
 -n,--nullable <arg>                 Fully Qualified name of the Nullable
                                     annotation
 -ncp,--nullaway-config-path <arg>   Path to the NullAway Config
 -p,--path <arg>                     Path to config file containing all
                                     flags values in json format
 -dpf,--disable-preserve-format               disable lexical preservation
```
```cmd
python3 run.py preprocess
python3 run.py explore
python3 run.py apply
python3 run.py clean
python3 run.py run
```

After the instructions above are followed, to annotate the target project, simply run `python3 run.py run`

### Output

All outputs will be stored at `OUTPUT_DIR` in `config.json` directory. To delete all outputs, run `python run.py clean`.


### Annotator Depth Level

Regarding `Annotator Depth level`, the number of remaining warnings will reduce as the depth increases. However, in our experiments,
level 4 is the sweet spot for having the best performance. Please look at the chart below, running the core from level 0 to 10 over 20 open source projects. As you can see, on level 4 we reach the optimal solution.

![image info](./../pics/depth.png)
