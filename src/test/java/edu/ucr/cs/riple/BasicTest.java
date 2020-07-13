package edu.ucr.cs.riple;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class BasicTest {
    @Test
    public void basicTest() {
        String greeting = "Hello World!";
        AutoFix.HelloWorld helloWorld = new AutoFix.HelloWorld(greeting);

        String actualGreeting = helloWorld.greet();

        System.out.println(actualGreeting + " " + greeting);
        assertThat(actualGreeting, is(greeting));
    }
}
