/*
 * MIT License
 *
 * Copyright (c) 2023 Nima Karimipour
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

import com.google.common.collect.ImmutableList;
import edu.ucr.cs.riple.injector.changes.AddTypeUseMarkerAnnotation;
import edu.ucr.cs.riple.injector.changes.RemoveTypeUseMarkerAnnotation;
import edu.ucr.cs.riple.injector.location.OnClassDeclaration;
import org.junit.Test;

public class OnClassDeclarationInjectionTest extends BaseInjectorTest {

  @Test
  public void additionTest() {
    injectorTestHelper
        .addInput("Foo.java", "package test;", "public class Foo extends Bar<String>{", "}")
        .expectOutput(
            "package test;",
            "import edu.custom.Untainted;",
            "public class Foo extends Bar<@Untainted String>{",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnClassDeclaration("Foo.java", "test.Foo", "Bar"),
                "edu.custom.Untainted",
                ImmutableList.of(ImmutableList.of(1, 0))))
        .start();
  }

  @Test
  public void deletionTest() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import edu.custom.Untainted;",
            "public class Foo extends Bar<@Untainted String>{",
            "}")
        .expectOutput(
            "package test;",
            "import edu.custom.Untainted;",
            "public class Foo extends Bar<String>{",
            "}")
        .addChanges(
            new RemoveTypeUseMarkerAnnotation(
                new OnClassDeclaration("Foo.java", "test.Foo", "Bar"),
                "edu.custom.Untainted",
                ImmutableList.of(ImmutableList.of(1, 0))))
        .start();
  }

  @Test
  public void additionOnAnonymousClassTest() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "public class Test {",
            "   void test() {",
            "   @RUntainted",
            "   File uriFile =",
            "       new File(",
            "           AccessController.doPrivileged(",
            "             new PrivilegedAction<String>() {",
            "               public String run() {",
            "                 return null;",
            "               }",
            "             }));",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import edu.custom.RUntainted;",
            "public class Test {",
            "   void test() {",
            "   @RUntainted",
            "   File uriFile =",
            "       new File(",
            "           AccessController.doPrivileged(",
            "             new PrivilegedAction<@RUntainted String>() {",
            "               public String run() {",
            "                 return null;",
            "               }",
            "             }));",
            "   }",
            "}")
        .addChanges(
            new AddTypeUseMarkerAnnotation(
                new OnClassDeclaration("Foo.java", "test.Test$1", "java.security.PrivilegedAction"),
                "edu.custom.RUntainted",
                ImmutableList.of(ImmutableList.of(1, 0))))
        .start();
  }

  @Test
  public void deletionOnAnonymousClassTest() {
    injectorTestHelper
        .addInput(
            "Foo.java",
            "package test;",
            "import edu.custom.RUntainted;",
            "public class Test {",
            "   void test() {",
            "   @RUntainted",
            "   File uriFile =",
            "       new File(",
            "           AccessController.doPrivileged(",
            "             new PrivilegedAction<@RUntainted String>() {",
            "               public String run() {",
            "                 return null;",
            "               }",
            "             }));",
            "   }",
            "}")
        .expectOutput(
            "package test;",
            "import edu.custom.RUntainted;",
            "public class Test {",
            "   void test() {",
            "   @RUntainted",
            "   File uriFile =",
            "       new File(",
            "           AccessController.doPrivileged(",
            "             new PrivilegedAction<String>() {",
            "               public String run() {",
            "                 return null;",
            "               }",
            "             }));",
            "   }",
            "}")
        .addChanges(
            new RemoveTypeUseMarkerAnnotation(
                new OnClassDeclaration("Foo.java", "test.Test$1", "java.security.PrivilegedAction"),
                "edu.custom.RUntainted",
                ImmutableList.of(ImmutableList.of(1, 0))))
        .start();
  }
}
