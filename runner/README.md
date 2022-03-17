## Runner
Script to run [Annotator](../README.md).

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

Please find a sample project setup below:
```groovy
dependencies {
    //Here we pass the customized verion of NullAway
    annotationProcessor "edu.ucr.cs.riple:nullaway:0.7.12-SNAPSHOT"
    compileOnly "com.google.code.findbugs:jsr305:3.0.2"
    errorprone "com.google.errorprone:error_prone_core:2.3.2"
    errorproneJavac "com.google.errorprone:javac:9+181-r4173-1"
}

tasks.withType(JavaCompile) {
    if (!name.toLowerCase().contains("test")) {
        options.errorprone.errorproneArgs += ["-XepDisableAllChecks",
                                              "-Xep:NullAway:ERROR",
                                              "-XepOpt:NullAway:AnnotatedPackages=",
                                              //Autofix flag must be set to true
                                              "-XepOpt:NullAway:AutoFix=true"]
    }
}
```

## Config
---
Configurations are written inside the `config.json` file. Please find a sample below:
```json
{
    "PROJECT_PATH": "/Projects/MPAndroidChart",
    "BUILD_COMMAND": "./gradlew build -x test",
    "ANNOTATION":{
        "INITIALIZE": "com.github.Initializer",
        "NONNULL": "javax.annotation.NonNull",
        "NULLABLE": "javax.annotation.Nullable"
    },
    "FORMAT": "",
    "DEPTH": 4
}
```
Below is the description of each setting:
1. `PROJECT_PATH`: The path to the project directory (if a subproject needs to be analyzied, this path needs to point to the subproject not the root project)
2. `BUILD_COMMAND`: The command to execute `NullAway` for the project at the path given in `PROJECT_PATH`. The script will use the command, `cd PROJECT_PATH && BUILD_COMMAND` to execute `NullAway`.
3. `INITIALIZE`: Fully qualified name of the `Initializer` annotation to inject on detected initializer methods.
4. `NONNULL`: Fully qualified name of the `Nonnull` annotation.
5. `NULLABLE`: Fully qualified name of the `Nullable` annotation.
6. `FORMAT`: Task which automatically reformats the code. This command should be executed at the directory where `BUILD_COMMAND` is executed. Since some projects have checkstyle tasks, reformat task will be executed to pass checkstyles in consecutive builds.
6. `DEPTH`: The depth of deep analysis.

## Run
---
Before running, please make sure that all the changes in the `Requirements for Target Project` section has been applied to the target project.

The script is written in `python3` in the file `run.py`. It needs the `NullAwayAutoFixer.jar` file to execute at the relative path: `./jars/NulAwayAutoFixer.jar` just like the structure in this repo.
To run the script a `command` must be passed to the script. A `command` must be one of the followings:
1. `diagnose`: It will make `diagnose_report.json` file which is the result of analyizing all fixes comming from `NullAway`
2. `pre`: It will perform a preprocessing phase which adds `@Initialize` annotation to all initializer methods detected by `NullAway`.
3. `apply`: It will apply all the effective fixes reported in `diagnose_report.json` which reduces the number or errors.
4. `clean`: It will clean all genereted files.
5. `loop`: It will run `diagnose`/`apply` in iterations, until no further new fix is suggested.

```cmd
python3 run.py pre
python3 run.py diagnose
python3 run.py apply
python3 run.py clean
python3 run.py loop
```

## Output

All outputs will be stored at `/tmp/NullAwayFix/` directory. To delete all outputs, Please run `python run.py clean`.

If `AutoFix` flag is set to `true`, anytime `NullAway` (can be via the build command) is executed on a project, a `fixes.json` file will be generetaed which includes all the suggested fixes.

Please find a sample `fixes.json` below:
```json
{
    "fixes": [
        {
            "annotation": "javax.annotation.Nullable",
            "reason": "FIELD_NO_INIT",
            "method": "",
            "param": "mVelocityTracker",
            "location": "CLASS_FIELD",
            "class": "com.github.CustomGestureDetector",
            "pkg": "com.github",
            "uri": "file:AbsolutePathTo/CustomGestureDetector.java",
            "inject": "true"
        }
    ]
}
```

After the diagnose task is finished, `diagnose_report.json` will be created which holds the information regarding the effectiveness of each fix written in `fixes.json`. Please find a sample below:

Here `effect` refers to the difference in number of errors.
```json
{
    "reports": [
        {
            "annotation": "javax.annotation.Nullable",
            "reason": "FIELD_NO_INIT",
            "method": "",
            "param": "mVelocityTracker",
            "location": "CLASS_FIELD",
            "class": "com.github.CustomGestureDetector",
            "pkg": "com.github",
            "inject": "true",
            "uri": "file:AbsolutePathTo/CustomGestureDetector.java",
            "errors": [],
            "effect": -3
        }
    ]
}
```


## Command Line Arguments

`Annotator` needs 5 arguments:
```txt
1. command to execute NullAway: (example: ./grawdlew build)
2. output directory: (default: /tmp/NullAwayFix/)
3. Annotator Depth level: (default: 0, depth of search in search space)
4. Nullable Annotation: (fully qualified name of the annotation)
5. optimized: (flag to run optimized version)
```
To run `Annotator` please use the repo [Diagnoser](https://github.com/nimakarimipour/Diagnoser) which has python scripts which automates setup and running `Annotator` on target module. ```Diagnoser``` uses a jar file where all required dependencies are included and does not need any installation.

Regarding `Annotator Depth level`, the number of remaining warnings will reduce as the depth increases. However, in our experiments,
level 4 is the sweet spot for having the best performance. Please look at the chart below, running the core from level 0 to 10 over 20 open source projects. As you can see, on level 4 we reach the optimal solution.

![image info](./pics/depth.png)


## Artifact Evaluation

Due to complexity of making test cases (inputs are project modules :D ). I provided a [docker](https://github.com/nimakarimipour/DiagnoserDocker) script and the [repo](https://github.com/nimakarimipour/Docker_AE_NA) for artifact evaluation of this project where docker works with
