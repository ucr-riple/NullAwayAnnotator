package edu.ucr.cs.riple;

import edu.riple.annotationinjector.Injector;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class AutoFix extends DefaultTask {

  Injector injector = null;
  String executable = "gradlew";

  void buildProject(Project project) {
    String executablePath = project.getProjectDir().getAbsolutePath();
    String subProjectPath = project.getPath().replace(":", "");
    if (executablePath.endsWith(subProjectPath)) {
      executablePath = executablePath.replace(subProjectPath, "");
    }
    String task = "build";
    String hideOutput = "> /dev/null 2>&1";
    String command = ""
//            + "export JAVA_HOME=`/usr/libexec/java_home -v 1.8`"
//            + " && cd "
            + "cd "
            + executablePath
            + " && ./"
            + executable
            + " "
            + task
            + " "
            + "--rerun-tasks "
            + hideOutput;

    System.out.println("Running: " + command);

    Process proc;
    try {
      proc = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", command});
      if (proc != null) {
        proc.waitFor();
      }
      command = "export JAVA_HOME=`/usr/libexec/java_home -v 11`";
      proc = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", command});
      if (proc != null) {
        proc.waitFor();
      }
    } catch (Exception e) {
      throw new RuntimeException("Could not run command: " + task + " from gradle");
    }
  }

  @TaskAction
  public void autoFix() {
    NullAwayAutoFixExtension extension =
        getProject().getExtensions().findByType(NullAwayAutoFixExtension.class);
    if (extension == null) {
      extension = new NullAwayAutoFixExtension();
    }

    // todo: use mode later
    String mode = extension.getMode();

    executable = extension.getExecutable();
    String fixPath =
        extension.getFixPath() == null
            ? getProject().getProjectDir().getAbsolutePath()
            : extension.getFixPath();
    fixPath = (fixPath.endsWith("/") ? fixPath : fixPath + "/") + "fixes.json";

    System.out.println(
        "\n"
            + "=========="
            + "\n"
            + "Process info:"
            + "\n"
            + "Mode: "
            + mode
            + "\n"
            + "Executable: "
            + executable
            + "\n"
            + "Fix path: "
            + fixPath);

    System.out.println("Building Injector");
    injector = Injector.builder(Injector.MODE.OVERWRITE).setFixesJsonFilePath(fixPath).build();
    System.out.println("Built.");
    run(fixPath);
  }

  private void run(String fixPath) {
    boolean finished = false;
    int round = 0;

    while (!finished) {
      System.out.println("Cleared fix path for new run: " + new File(fixPath).delete());
      System.out.println("Round " + (++round) + "...");
      buildProject(getProject());
      if (newFixRequested(fixPath)) {
        System.out.println("NullAway found some fixable error(s), going for next round...");
        injector.start();
      } else {
        System.out.println("NullAway found no fixable errors, shutting down.");
        finished = true;
      }
    }
  }

  private boolean newFixRequested(String path) {
    try {
      BufferedReader bufferedReader =
          Files.newBufferedReader(Paths.get(path), Charset.defaultCharset());
      JSONObject obj = (JSONObject) new JSONParser().parse(bufferedReader);
      JSONArray fixesJson = (JSONArray) obj.get("fixes");
      bufferedReader.close();
      return fixesJson.size() > 0;
    } catch (IOException ex) {
      return false;
    } catch (ParseException e) {
      throw new RuntimeException("Error in parsing object: " + e);
    }
  }
}
