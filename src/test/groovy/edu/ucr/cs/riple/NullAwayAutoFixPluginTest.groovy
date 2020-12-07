package edu.ucr.cs.riple

import edu.ucr.cs.riple.diagnose.Diagnose
import edu.ucr.cs.riple.editors.AnnotationEditor
import org.junit.Test
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Project
import static org.junit.Assert.assertTrue

//This is a test to check if classes are correctly wired.
class NullAwayAutoFixPluginTest {

    @Test
    void demo_plugin_should_add_task_to_project() {
        Project project = ProjectBuilder.builder().build()
        project.getPlugins().apply 'edu.ucr.cs.riple.plugin'

        assertTrue(project.tasks.autofix instanceof AutoFix)
        assertTrue(project.tasks.annotedit instanceof AnnotationEditor)
        assertTrue(project.tasks.diagnose instanceof Diagnose)
    }

    @Test
    void should_be_able_to_add_task_to_project() {
        Project project = ProjectBuilder.builder().build()

        def task = project.task('autofix', type: AutoFix)
        assertTrue(task instanceof AutoFix)

        task = project.task('annotedit', type: AnnotationEditor)
        assertTrue(task instanceof AnnotationEditor)

        task = project.task('diagnose', type: Diagnose)
        assertTrue(task instanceof Diagnose)
    }
}
