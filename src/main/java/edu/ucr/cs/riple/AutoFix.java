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
  private String executable;
  private String reformat;
  boolean hideNullAwayOutput;
  private int maximumRound;

  @TaskAction
  public void autoFix() {
    NullAwayAutoFixExtension extension =
            getProject().getExtensions().findByType(NullAwayAutoFixExtension.class);
    if (extension == null) {
      extension = new NullAwayAutoFixExtension();
    }

    hideNullAwayOutput = extension.shouldHideNullAwayOutput();
    maximumRound = extension.getMaximumRound();
    executable = extension.getExecutable();
    reformat = extension.getFormatTask();

    String fixPath =
            extension.getFixPath() == null
                    ? getProject().getProjectDir().getAbsolutePath()
                    : extension.getFixPath();
    fixPath = (fixPath.endsWith("/") ? fixPath : fixPath + "/") + "fixes.json";

    String reformatSummary =
            (reformat != null && !reformat.equals("")) ? "Reformat Task: " + reformat + "\n" : "";
    System.out.println(
            "\n"
                    + "=========="
                    + "\n"
                    + "Process info:"
                    + "\n"
                    + "Executable: "
                    + executable
                    + "\n"
                    + reformatSummary
                    + "Fix path: "
                    + fixPath);

    System.out.println("Building Injector....");
    injector = Injector.builder(Injector.MODE.OVERWRITE).setFixesJsonFilePath(fixPath).build();
    System.out.println("Built.");
    run(fixPath);
  }

  private void run(String fixPath) {
    boolean finished = false;
    int round = 0;

    while (!finished && round < maximumRound) {
      System.out.println("Cleared fix path for new run: " + new File(fixPath).delete());
      System.out.println("Round " + (++round) + "...");
      execute(getProject(), "build", true, false);
      if (newFixRequested(fixPath)) {
        System.out.println("NullAway found some fixable error(s), going for next round...");
        injector.start();
      } else {
        System.out.println("NullAway found no fixable errors, shutting down.");
        finished = true;
      }
    }
    if (round >= maximumRound) System.out.println("Exceeded maximum round");
    if (reformat != null && !reformat.equals("")) {
      System.out.println("Reformatting project...");
      execute(getProject(), reformat, false, true);
    }
    System.out.println("Finished");
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

  private void execute(Project project, String taskName, boolean rerun, boolean topLevel){
    String task = "";
    String executablePath = project.getProjectDir().getAbsolutePath();
    String subProjectPath = project.getPath().replace(":", "");
    if (!topLevel) {
      if (executablePath.endsWith(subProjectPath)) executablePath = executablePath.replace(subProjectPath, "");
      task = (project.getPath().equals(":") ? "" : project.getPath() + ":") + taskName;
    }else{
      task = taskName;
      executablePath = executablePath.replace(project.getPath().substring(1), "");
    }
    String hideOutput = "> /dev/null 2>&1";
    String command = ""
            + "cd "
            + executablePath
            + " && ./"
            + executable
            + " "
            + task
            + " ";
    if(rerun) command += "--rerun-tasks";
    if(hideNullAwayOutput) command += hideOutput;
    Process proc;
    try {
      proc = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", command});
      if (proc != null) {
        proc.waitFor();
      }
    } catch (Exception e) {
      throw new RuntimeException("Could not run command: " + task + " from gradle");
    }
  }
}
