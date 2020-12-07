package edu.ucr.cs.riple;

import edu.ucr.cs.riple.diagnose.Diagnose;
import edu.ucr.cs.riple.diagnose.DiagnoseExtension;
import edu.ucr.cs.riple.editors.AnnotationEditor;
import edu.ucr.cs.riple.editors.AnnotationEditorExtension;
import edu.ucr.cs.riple.editors.ApplyBatch;
import edu.ucr.cs.riple.editors.ApplyBatchExtension;
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

        project.getExtensions().create("Batch", ApplyBatchExtension.class);
        project.getTasks().create("applyBatch", ApplyBatch.class);
    }
}
