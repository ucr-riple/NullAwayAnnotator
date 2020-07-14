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

    void buildProject(Project project){
        String task = "build";
        String setToJava11 = "export JAVA_HOME=`/usr/libexec/java_home -v 11` && ";
        String command = setToJava11
                + "cd "
                + project.getProjectDir().getAbsolutePath()
                + " && ./"
                + executable + " "
                + task + " "
                + "--rerun-tasks";

        Process proc;
        try {
            proc = Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", command });
            if (proc != null) {
                proc.waitFor();
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not run command: " + task + " from gradle");
        }
    }

    @TaskAction
    public void autoFix() {
        NullAwayAutoFixExtension extension = getProject().getExtensions().findByType(NullAwayAutoFixExtension.class);
        if (extension == null) {
            extension = new NullAwayAutoFixExtension();
        }

        //todo: use later
        String mode = extension.getMode();

        executable = extension.getExecutable();
        String fixPath = extension.getFixPath() == null ? getProject().getProjectDir().getAbsolutePath() : extension.getFixPath();
        fixPath = (fixPath.endsWith("/") ? fixPath : fixPath + "/") + "fixes.json";

        System.out.println("\n" +
                "==========" + "\n" +
                "Process info:" + "\n" +
                "Mode: " + mode + "\n" +
                "Executable: " + executable + "\n" +
                "Fix path: " + fixPath
        );
        injector = Injector.builder(Injector.MODE.OVERWRITE).setFixesJsonFilePath(fixPath).build();
        run(fixPath);
    }

    private void run(String fixPath) {
        boolean finished = false;

        while (!finished) {
            buildProject(getProject());
            if(newFixRequested(fixPath)) {
                System.out.println("NullAway found some error(s), going for next round...");
                injector.start();
                System.out.println("Cleared fix path for new run: " + new File(fixPath).delete());
            }
            else {
                System.out.println("NullAway found no errors, shutting down.");
                finished = true;
            }
        }
    }

    private boolean newFixRequested(String path){
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
