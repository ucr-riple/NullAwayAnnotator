package edu.ucr.cs.riple.components;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BaseErrorProneJavaCompiler;
import com.google.errorprone.BugPattern;
import com.google.errorprone.DiagnosticTestHelper;
import com.google.errorprone.ErrorProneOptions;
import com.google.errorprone.InvalidCommandLineOptionException;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.main.Main;

import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.truth.Truth.assertWithMessage;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.fail;

public class NullAwayRunner {

    private static final ImmutableList<String> DEFAULT_ARGS =
            ImmutableList.of("-encoding", "UTF-8", "-XDdev", "-parameters", "-XDcompilePolicy=simple");

    private final DiagnosticTestHelper diagnosticHelper;
    private final BaseErrorProneJavaCompiler compiler;
    private final ByteArrayOutputStream outputStream;
    private final NullAwayInMemoryFileManager fileManager;
    private final List<JavaFileObject> sources = new ArrayList<>();
    private ImmutableList<String> extraArgs = ImmutableList.of();
    private boolean run = false;

    private NullAwayRunner(ScannerSupplier scannerSupplier, String checkName, Class<?> clazz) {
        this.fileManager = new NullAwayInMemoryFileManager(clazz);
        try {
            fileManager.setLocation(StandardLocation.SOURCE_PATH, Collections.emptyList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        this.diagnosticHelper = new DiagnosticTestHelper(checkName);
        this.outputStream = new ByteArrayOutputStream();
        this.compiler = new BaseErrorProneJavaCompiler(scannerSupplier);
    }

    public static NullAwayRunner newInstance(
            Class<? extends BugChecker> checker, Class<?> clazz) {
        ScannerSupplier scannerSupplier = ScannerSupplier.fromBugCheckerClasses(checker);
        String checkName = checker.getAnnotation(BugPattern.class).name();
        return new NullAwayRunner(scannerSupplier, checkName, clazz);
    }


    private static List<String> buildArguments(List<String> extraArgs) {
        ImmutableList.Builder<String> result = ImmutableList.<String>builder().addAll(DEFAULT_ARGS);
        return result.addAll(extraArgs).build();
    }

    public NullAwayRunner addSourceLines(String path, String... lines) {
        this.sources.add(fileManager.forSourceLines(path, lines));
        return this;
    }

    public NullAwayRunner setArgs(List<String> args) {
        this.extraArgs = ImmutableList.copyOf(args);
        return this;
    }

    public void doTest() {
        checkState(!sources.isEmpty(), "No source files to compile");
        checkState(!run, "doTest should only be called once");
        this.run = true;
        Main.Result result = compile();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnosticHelper.getDiagnostics()) {
            if (diagnostic.getCode().contains("error.prone.crash")) {
                fail(diagnostic.getMessage(Locale.ENGLISH));
            }
        }
    }

    private Main.Result compile() {
        List<String> processedArgs = buildArguments(extraArgs);
        checkWellFormed(sources, processedArgs);
        fileManager.createAndInstallTempFolderForOutput();
        return compiler
                .getTask(
                        new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, UTF_8)), true),
                        fileManager,
                        diagnosticHelper.collector,
                        ImmutableList.copyOf(processedArgs),
                        ImmutableList.of(),
                        sources)
                .call()
                ? Main.Result.OK
                : Main.Result.ERROR;
    }

    private void checkWellFormed(Iterable<JavaFileObject> sources, List<String> args) {
        fileManager.createAndInstallTempFolderForOutput();
        JavaCompiler compiler = JavacTool.create();
        OutputStream outputStream = new ByteArrayOutputStream();
        List<String> remainingArgs = null;
        try {
            remainingArgs = Arrays.asList(ErrorProneOptions.processArgs(args).getRemainingArgs());
        } catch (InvalidCommandLineOptionException e) {
            fail("Exception during argument processing: " + e);
        }
        JavaCompiler.CompilationTask task =
                compiler.getTask(
                        new PrintWriter(
                                new BufferedWriter(new OutputStreamWriter(outputStream, UTF_8)), true),
                        fileManager,
                        null,
                        remainingArgs,
                        null,
                        sources);
        boolean result = task.call();
        assertWithMessage(
                String.format(
                        "Test program failed to compile with non Error Prone error: %s", outputStream))
                .that(result)
                .isTrue();
    }
}
