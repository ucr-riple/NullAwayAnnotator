## Runner
Script to run [Annotator](../README.md). The script `run.py` can be used to run the `Annotator`, after the below configurations are set.

### Installation
All dependencies are provided via a `jar` file located in `jars` directory. 
To re-create/update the jar file, run `python3 updatejar.py`

### Requirements for the Target Project

Below are the instructions to prepare the target project:

#### Dependencies
1. `NullAway` checker must be activated with version >= `0.9.6`
2. `CSS` checker must be activated located [here](../css/README.md).

#### Error Prone Flags
```
"-Xep:NullAway:ERROR", // to activate NullAway
"-XepOpt:NullAway:SerializeFixMetadata=true",
"-XepOpt:NullAway:FixSerializationConfigPath=path_to_config.xml",
"-Xep:CSS:ERROR", // to activate CSS
"-XepOpt:CSS:ConfigPath=path_to_css.xml",
```

`path_to_config.xml` and `path_to_css.xml` are config files which **are not necessary** to create at the time of preparing the project. 
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

We need to inform the script about `path_to_config.xml` and `path_to_css.xml` (number 8 and 9 in the next section), please read the section below.

### Script Config

Configurations are written inside the `config.json` file. Please find a sample below:
```json
{
  "PROJECT_PATH": "/Path/To/Root/Of/Project",
  "BUILD_COMMAND": "./gradlew build -x test",
  "ANNOTATION": {
    "INITIALIZER": "com.uber.Initializer",
    "NONNULL": "javax.annotation.Nonnull",
    "NULLABLE": "javax.annotation.Nullable"
  },
  "FORMAT": true,
  "OUTPUT_DIR": "/tmp/NullAwayFix",
  "NULLAWAY_CONFIG_PATH": "/tmp/NullAwayFix/config.xml",
  "CSS_CONFIG_PATH": "/tmp/NullAwayFix/css.xml",
  "DEPTH": 10
}
```
Below is the description of each setting:
1. `PROJECT_PATH`: The path to the project directory (if a subproject needs to be analyzied, this path needs to point to the subproject not the root project)
2. `BUILD_COMMAND`: The command to execute `NullAway` for the project at the path given in `PROJECT_PATH`. The script will use the command, `cd PROJECT_PATH && BUILD_COMMAND` to execute `NullAway`.
3. `INITIALIZER`: Fully qualified name of the `Initializer` annotation to inject on detected initializer methods.
4. `NONNULL`: Fully qualified name of the `Nonnull` annotation.
5. `NULLABLE`: Fully qualified name of the `Nullable` annotation.
6. `FORMAT`: If set to `true` the formate of the code will be preserved at the end of execution.
7. `OUTPUT_DIR`: Directory where the serialized output of NullAway should be written.
8. `NULLAWAY_CONFIG_PATH`: `path_to_config.xml` given to project in time of preparing the process.
9. `CSS_CONFIG_PATH`: `path_to_css.xml` given to project in time of preparing the process. 
10. `DEPTH`: The depth of deep analysis.

### Running the script

Before running, please make sure that all the changes in the `Requirements for the Target Project` section has been applied to the target project.

The script is written in `python3` in the file `run.py`. It needs the `core.jar` file to execute at the relative path: `./jars/core.jar` just like the structure in this repo. To recreate/update the `core.jar` please run `python3 updatejar.py`

To run the script a `command` must be passed to the script. A `command` must be one of the followings:
1. `explore`: It will make `diagnose_report.json` file which is the result of analyizing all fixes comming from `NullAway`
2. `preprocess`: It will perform a preprocessing phase which adds `@Initialize` annotation to all initializer methods detected by `NullAway`.
3. `apply`: It will apply all the effective fixes reported in `diagnose_report.json` which reduces the number or errors.
4. `clean`: It will clean all genereted files.
5. `run`: It will run `explore`/`apply` in iterations, until no further new fix is suggested.

```cmd
python3 run.py preprocess
python3 run.py explore
python3 run.py apply
python3 run.py clean
python3 run.py run
```

After the instructions above are followed, to annotate the target project, simply run `python3 run.py run`

### Output

All outputs will be stored at `OUTPUT_DIR` in `config.json` directory. To delete all outputs, Please run `python run.py clean`.


### Annotator Depth Level

Regarding `Annotator Depth level`, the number of remaining warnings will reduce as the depth increases. However, in our experiments,
level 4 is the sweet spot for having the best performance. Please look at the chart below, running the core from level 0 to 10 over 20 open source projects. As you can see, on level 4 we reach the optimal solution.

![image info](./../pics/depth.png)
