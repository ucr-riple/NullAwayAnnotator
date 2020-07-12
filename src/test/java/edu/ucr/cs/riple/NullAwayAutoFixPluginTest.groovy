package edu.ucr.cs.riple

import org.junit.Test
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Project
import static org.junit.Assert.*

class NullAwayAutoFixPluginTest {
    @Test
    void demo_plugin_should_add_task_to_project() {
        Project project = ProjectBuilder.builder().build()
        project.getPlugins().apply 'edu.ucr.cs.riple.demo.plugin'

        assertTrue(project.tasks.demo instanceof AutoFix)
    }
}