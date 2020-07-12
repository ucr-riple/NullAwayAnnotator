package edu.ucr.cs.riple;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class NullAwayAutoFixPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getExtensions().create("demoSetting", NullAwayAutoFixExtension.class);
        project.getTasks().create("demo", AutoFix.class);
    }
}