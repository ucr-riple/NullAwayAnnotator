package edu.ucr.cs.riple;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;


public class AutoFix extends DefaultTask {

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

        String mode = extension.getMode();
        System.out.println("Current MODE is: " + mode);

        runTask(getProject(), "build");
    }
}
