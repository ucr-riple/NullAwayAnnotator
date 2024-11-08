/*
 * MIT License
 *
 * Copyright (c) 2022 Nima Karimipour
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package edu.ucr.cs.riple.injector;

import edu.ucr.cs.riple.injector.changes.AddMarkerAnnotation;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.injector.location.OnMethod;
import java.util.Collections;
import org.junit.Test;

public class ImportDeclarationAdditionTest extends BaseInjectorTest {

  @Test
  public void customNullableExists() {
    // Custom annot with Nullable simple name exists, the import declaration should be skipped.
    injectorTestHelper
        .addInput(
            "Main.java",
            "package ucr.edu;",
            "import custom.Nullable;",
            "public class Main {",
            "   public enum Test{",
            "     CLASSIC;",
            "     public Object run(){",
            "       return null;",
            "     }",
            "   }",
            "}")
        .expectOutput(
            "package ucr.edu;",
            "import custom.Nullable;",
            "public class Main {",
            "   public enum Test{",
            "     CLASSIC;",
            "     @Nullable public Object run(){",
            "       return null;",
            "     }",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod("Main.java", "ucr.edu.Main$Test", "run()"),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void customEndingWithNullableExists() {
    // Custom annot ending with Nullable exists, the import declaration should be added since we
    // only skip if the simple name is exactly is Nullable.
    injectorTestHelper
        .addInput(
            "Main.java",
            "package ucr.edu;",
            "import custom.aNullable;",
            "public class Main {",
            "   public enum Test{",
            "     CLASSIC;",
            "     public Object run(){",
            "       return null;",
            "     }",
            "   }",
            "}")
        .expectOutput(
            "package ucr.edu;",
            "import custom.aNullable;",
            "import javax.annotation.Nullable;",
            "public class Main {",
            "   public enum Test{",
            "     CLASSIC;",
            "     @Nullable public Object run(){",
            "       return null;",
            "     }",
            "   }",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnMethod("Main.java", "ucr.edu.Main$Test", "run()"),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void noImportNoJavadocNoCopyrightNoPackageTest() {
    injectorTestHelper
        .addInput("Test.java", "public class Test {", "   Object h = new Object();", "}")
        .expectOutput(
            "import javax.annotation.Nullable;",
            "public class Test {",
            "   @Nullable Object h = new Object();",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Test.java", "Test", Collections.singleton("h")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void noImportNoJavadocNoCopyrightWithPackageTest() {
    injectorTestHelper
        .addInput(
            "Test.java",
            "package edu.ucr;",
            "public class Test {",
            "   Object h = new Object();",
            "}")
        .expectOutput(
            "package edu.ucr;",
            "import javax.annotation.Nullable;",
            "public class Test {",
            "   @Nullable Object h = new Object();",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Test.java", "edu.ucr.Test", Collections.singleton("h")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void noImportNoJavadocWithCopyrightWithPackageTest() {
    injectorTestHelper
        .addInput(
            "Test.java",
            "/*",
            " * Copyright content",
            " */",
            "package edu.ucr;",
            "public class Test {",
            "   Object h = new Object();",
            "}")
        .expectOutput(
            "/*",
            " * Copyright content",
            " */",
            "package edu.ucr;",
            "import javax.annotation.Nullable;",
            "public class Test {",
            "   @Nullable Object h = new Object();",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Test.java", "edu.ucr.Test", Collections.singleton("h")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void noImportWithJavadocWithCopyrightWithPackageTest() {
    injectorTestHelper
        .addInput(
            "Test.java",
            "/*",
            " * Copyright content",
            " */",
            "package edu.ucr;",
            "/**",
            " * javadoc",
            " */",
            "public class Test {",
            "   Object h = new Object();",
            "}")
        .expectOutput(
            "/*",
            " * Copyright content",
            " */",
            "package edu.ucr;",
            "import javax.annotation.Nullable;",
            "/**",
            " * javadoc",
            " */",
            "public class Test {",
            "   @Nullable Object h = new Object();",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Test.java", "edu.ucr.Test", Collections.singleton("h")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void noImportWithJavadocWithCopyrightNoPackageTest() {
    injectorTestHelper
        .addInput(
            "Test.java",
            "/*",
            " * Copyright content",
            " */",
            "/**",
            " * javadoc",
            " */",
            "public class Test {",
            "   Object h = new Object();",
            "}")
        .expectOutput(
            "/*",
            " * Copyright content",
            " */",
            "import javax.annotation.Nullable;",
            "/**",
            " * javadoc",
            " */",
            "public class Test {",
            "   @Nullable Object h = new Object();",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Test.java", "Test", Collections.singleton("h")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void noImportNoJavadocWithCopyrightNoPackageTest() {
    injectorTestHelper
        .addInput(
            "Test.java",
            "/*",
            " * Copyright content",
            " */",
            "public class Test {",
            "   Object h = new Object();",
            "}")
        .expectOutput(
            "/*",
            " * Copyright content",
            " */",
            "import javax.annotation.Nullable;",
            "public class Test {",
            "   @Nullable Object h = new Object();",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Test.java", "Test", Collections.singleton("h")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void noImportNoJavadocWithCopyrightNoPackageOtherFormatTest() {
    injectorTestHelper
        .addInput(
            "Test.java",
            "//",
            "// Copyright content",
            "// ",
            "public class Test {",
            "   Object h = new Object();",
            "}")
        .expectOutput(
            "//",
            "// Copyright content",
            "// ",
            "import javax.annotation.Nullable;",
            "public class Test {",
            "   @Nullable Object h = new Object();",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Test.java", "Test", Collections.singleton("h")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void withImportWithJavadocWithCopyrightNoPackageTest() {
    injectorTestHelper
        .addInput(
            "Test.java",
            "/*",
            " * Copyright content",
            " */",
            "import custom.annots.A;",
            "/**",
            " * javadoc",
            " */",
            "public class Test {",
            "   Object h = new Object();",
            "}")
        .expectOutput(
            "/*",
            " * Copyright content",
            " */",
            "import custom.annots.A;",
            "import javax.annotation.Nullable;",
            "/**",
            " * javadoc",
            " */",
            "public class Test {",
            "   @Nullable Object h = new Object();",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Test.java", "Test", Collections.singleton("h")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void withImportWithJavadocWithCopyrightWithPackageTest() {
    injectorTestHelper
        .addInput(
            "Test.java",
            "/*",
            " * Copyright content",
            " */",
            "package edu.ucr;",
            "import custom.annots.A;",
            "/**",
            " * javadoc",
            " */",
            "public class Test {",
            "   Object h = new Object();",
            "}")
        .expectOutput(
            "/*",
            " * Copyright content",
            " */",
            "package edu.ucr;",
            "import custom.annots.A;",
            "import javax.annotation.Nullable;",
            "/**",
            " * javadoc",
            " */",
            "public class Test {",
            "   @Nullable Object h = new Object();",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Test.java", "edu.ucr.Test", Collections.singleton("h")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void importEndStartOnNewLineTest() {
    injectorTestHelper
        .addInput(
            "Test.java",
            "import",
            "          custom.annots.A;",
            "public class Test {",
            "   Object h = new Object();",
            "}")
        .expectOutput(
            "import",
            "          custom.annots.A;",
            "import javax.annotation.Nullable;",
            "public class Test {",
            "   @Nullable Object h = new Object();",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Test.java", "Test", Collections.singleton("h")),
                "javax.annotation.Nullable"))
        .start();
  }

  @Test
  public void packageEndStartOnNewLineTest() {
    injectorTestHelper
        .addInput(
            "Test.java",
            "package",
            "          edu.ucr;",
            "public class Test {",
            "   Object h = new Object();",
            "}")
        .expectOutput(
            "package",
            "          edu.ucr;",
            "import javax.annotation.Nullable;",
            "public class Test {",
            "   @Nullable Object h = new Object();",
            "}")
        .addChanges(
            new AddMarkerAnnotation(
                new OnField("Test.java", "edu.ucr.Test", Collections.singleton("h")),
                "javax.annotation.Nullable"))
        .start();
  }
}
