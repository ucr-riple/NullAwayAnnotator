package edu.ucr.cs.riple;

import edu.ucr.cs.riple.diagnose.Diagnose;
import edu.ucr.cs.riple.diagnose.DiagnoseExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class NullAwayAutoFixPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getExtensions().create("autoFixMode", NullAwayAutoFixExtension.class);
        project.getTasks().create("autofix", AutoFix.class);

        project.getExtensions().create("annotationEditor", AnnotationEditorExtension.class);
        project.getTasks().create("annotedit", AnnotationEditor.class);

        project.getExtensions().create("diagnoseMode", DiagnoseExtension.class);
        project.getTasks().create("diagnose", Diagnose.class);
    }
}
