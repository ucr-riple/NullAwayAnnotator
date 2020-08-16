package edu.ucr.cs.riple;

import edu.riple.annotationinjector.Injector;
import edu.riple.annotationinjector.Report;
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
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoFix extends DefaultTask {

  Injector injector = null;
  private String executable;
  private String reformat;
  private int maximumRound;

  @TaskAction
  public void autoFix() {
    NullAwayAutoFixExtension extension =
        getProject().getExtensions().findByType(NullAwayAutoFixExtension.class);
    if (extension == null) {
      extension = new NullAwayAutoFixExtension();
    }

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
    injector =
        Injector.builder()
            .setMode(Injector.MODE.OVERWRITE)
            .setFixesJsonFilePath(fixPath)
            .setCleanImports(false)
            .setNumberOfWorkers(1)
            .build();
    System.out.println("Built.");
    run(fixPath);
  }

  private void run(String fixPath) {
    boolean finished = false;
    int round = 0;
    ArrayList<IterationReport> reports = new ArrayList<>();
    while (!finished && round < maximumRound) {
      IterationReport r = new IterationReport();
      System.out.println("Cleared fix path for new run: " + new File(fixPath).delete());
      System.out.println("Round " + (++round) + "...");
      r.round = round;
      int totalNumberOfErrors = buildProject(getProject());
      r.totalErrors = totalNumberOfErrors;
      System.out.println("Total number of errors found by NullAway: " + totalNumberOfErrors);
      if (newFixRequested(fixPath)) {
        System.out.println("NullAway found some fixable error(s), going for next round...");
        Report report = injector.start();
        r.fixableErrors = report.totalNumberOfDistinctFixes;
        r.processed = report.processed;
        System.out.println("Report: " + report);
        if (report.processed == 0) {
          System.out.println(
              "No new fixable error has been discovered compared to the last iteration, shutting down.");
          finished = true;
        }
      } else {
        System.out.println("NullAway found no fixable errors, shutting down.");
        finished = true;
      }
      System.gc();
      reports.add(r);
    }
    writeReports(reports, fixPath);
    if (round >= maximumRound && !finished) System.out.println("Exceeded maximum round");
    if (reformat != null && !reformat.equals("")) {
      System.out.println("Reformatting project...");
      reformat(getProject());
    }
    System.out.println("Finished");
  }

  @SuppressWarnings("unchecked")
  private void writeReports(ArrayList<IterationReport> reports, String fixPath) {
    JSONObject object = new JSONObject();
    object.put("reports", reports);
    fixPath = fixPath.substring(0, fixPath.lastIndexOf("/")).concat("/reports.json");
    try (Writer writer = Files.newBufferedWriter(Paths.get(fixPath), Charset.defaultCharset())) {
      writer.write(object.toJSONString().replace("\\", ""));
      writer.flush();
    } catch (IOException e) {
      throw new RuntimeException("Could not create the fix json file");
    }
  }

  private boolean newFixRequested(String path) {
    try {
      BufferedReader bufferedReader =
          Files.newBufferedReader(Paths.get(path), Charset.defaultCharset());
      JSONObject obj = (JSONObject) new JSONParser().parse(bufferedReader);
      JSONArray fixesJson = (JSONArray) obj.get("fixes");
      bufferedReader.close();
      System.out.println("Number of fixable errors found by NullAway: " + fixesJson.size());
      return fixesJson.size() > 0;
    } catch (IOException ex) {
      return false;
    } catch (ParseException e) {
      throw new RuntimeException("Error in parsing object: " + e);
    }
  }

  private int buildProject(Project project) {
    int totalNumberOfErrors = 0;
    String executablePath = project.getProjectDir().getAbsolutePath();
    String task = "";
    if (!project.getPath().equals(":")) {
      String subProjectPath = project.getPath().replace(":", "");
      executablePath =
          executablePath.substring(0, executablePath.length() - subProjectPath.length());
      task = project.getPath() + ":";
    }
    task += "build";
    String command = "" + "cd " + executablePath + " && ./" + executable + " " + task;
    Process proc;
    try {
      System.out.println("NullAway is Running...");
      proc = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", command});
      String line;
      if (proc != null) {
        BufferedReader input = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        final String innerClassInstantiationByReferenceRegex = "^\\d+\\s+errors";
        while ((line = input.readLine()) != null) {
          Matcher matcher = Pattern.compile(innerClassInstantiationByReferenceRegex).matcher(line);
          if (matcher.find()) totalNumberOfErrors = Integer.parseInt(matcher.group().split(" ")[0]);
        }
        input.close();
      }
    } catch (Exception e) {
      System.out.println("Error happened: " + e.getMessage());
      throw new RuntimeException("Could not run command: " + command + " from gradle");
    }
    System.out.println("NullAway is finished.");
    return totalNumberOfErrors;
  }

  private void reformat(Project project) {
    String task;
    String executablePath = project.getProjectDir().getAbsolutePath();
    task = reformat;
    executablePath = executablePath.replace(project.getPath().substring(1), "");
    String hideOutput = "> /dev/null 2>&1";
    String command = "" + "cd " + executablePath + " && ./" + executable + " " + task + " ";
    command += hideOutput;
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
