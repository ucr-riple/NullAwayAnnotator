package edu.ucr.cs.riple;

import edu.riple.annotationinjector.Fix;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;


public class AutoFix extends DefaultTask {

    Injector injector = null;

    static void runTask(Project project, String task){
        String command = "cd " + project.getProjectDir().getAbsolutePath() + " && ./gradlew" + " " + task;
        Process proc = null;
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

        String fixPath = extension.getFixPath() == null ? getProject().getProjectDir().getAbsolutePath() : extension.getFixPath();
        fixPath = (fixPath.endsWith("/") ? fixPath : fixPath + "/") + "fixes.json";


        System.out.println("Fix path: " + fixPath);
        injector = Injector.builder(Injector.MODE.OVERWRITE).setFixesJsonFilePath(fixPath).build();
        run(fixPath);
    }

    private void run(String fixPath) {
        boolean finished = false;

        while (!finished) {
            runTask(getProject(), "build");
            if(newFixRequested(fixPath)) {
                System.out.println("NullAway found some error(s), going for next round...");
                injector.start();
                new File(fixPath).delete();
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
