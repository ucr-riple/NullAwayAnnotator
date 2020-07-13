package edu.ucr.cs.riple;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public class AutoFix extends DefaultTask {
    @TaskAction
    public void getMode() {
        NullAwayAutoFixExtension extension = getProject().getExtensions().findByType(NullAwayAutoFixExtension.class);
        if (extension == null) {
            extension = new NullAwayAutoFixExtension();
        }

        String mode = extension.getMode();
        System.out.println("Current MODE is: " + mode);
    }
}