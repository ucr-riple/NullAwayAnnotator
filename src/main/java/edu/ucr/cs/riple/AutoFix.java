package edu.ucr.cs.riple;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public class AutoFix extends DefaultTask {
    @TaskAction
    public void greet() {
        NullAwayAutoFixExtension extension = getProject().getExtensions().findByType(NullAwayAutoFixExtension.class);
        if (extension == null) {
            extension = new NullAwayAutoFixExtension();
        }

        String message = extension.getMessage();
        HelloWorld helloWorld = new HelloWorld(message);
        System.out.println(helloWorld.greet());
    }

    static class HelloWorld{
        String message;

        public HelloWorld(String message) {
            this.message = message;
        }

        String greet(){
            return message;
        }
    }
}