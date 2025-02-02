/*
 * MIT License
 *
 * Copyright (c) 2025 Nima Karimipour
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

package edu.ucr.cs.riple.core;

import static org.mockito.ArgumentMatchers.any;

import edu.ucr.cs.riple.core.checkers.nullaway.codefix.agent.ChatGPT;
import java.util.Arrays;
import org.junit.After;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;

public class CodeFixTest extends AnnotatorBaseCoreTest {

  MockedStatic<ChatGPT> responseMockedStatic;

  public CodeFixTest() {
    super("nullable-multi-modular");
  }

  private void mockChatGPTResponse(String[] responses) {
    if (responses == null) {
      throw new IllegalStateException("Mocked Responses are not set");
    }
    MockedStatic<ChatGPT> chatGPTMocked = Mockito.mockStatic(ChatGPT.class);
    OngoingStubbing<ChatGPT> stubbing = chatGPTMocked.when(() -> ChatGPT.ask(any()));
    Arrays.stream(responses).forEach(response -> stubbing.thenAnswer(invocation -> response));
    responseMockedStatic = chatGPTMocked;
  }

  @After
  public void close() {
    if (responseMockedStatic != null) {
      responseMockedStatic.close();
    }
  }

  @Test
  public void dereferenceEqualsRewriteTest() {
    mockChatGPTResponse(
        new String[] {
          "```java\n"
              + "public boolean equals(Object other) {\n"
              + "  if (other == null) {\n"
              + "    return false;\n"
              + "  }\n"
              + "  if (other instanceof Foo) {\n"
              + "    return Objects.equals(f, ((Foo) other).f);\n"
              + "  }\n"
              + "  return false;\n"
              + "}\n"
              + "```"
        });
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   @Nullable Object f;",
            "   public boolean equals(Object other) {",
            "      if(other == null) {",
            "         return false;",
            "      }",
            "      if(other instanceof Foo) {",
            "         return f.equals(((Foo) other).f);",
            "      }",
            "      return false;",
            "   }",
            "}")
        .expectNoReport()
        .resolveRemainingErrors()
        .start();
  }

  @Test
  public void dereferenceHashCodeRewriteTest() {
    mockChatGPTResponse(
        new String[] {
          "```java\n"
              + "public int hashCode() {\n"
              + "   return f == null ? 1 : f.hashCode();\n"
              + "}\n"
              + "```"
        });
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   @Nullable Object f;",
            "   public int hashCode() {",
            "      return f.hashCode();",
            "   }",
            "}")
        .expectNoReport()
        .resolveRemainingErrors()
        .start();
  }

  @Test
  public void dereferenceToStringRewriteTest() {
    mockChatGPTResponse(
        new String[] {
          "```java\n"
              + "public String toString() {\n"
              + "  return f == null ? \"null\" : f.toString();\n"
              + "}\n"
              + "```"
        });
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "   @Nullable Object f;",
            "   public String toString() {",
            "      return f.toString();",
            "   }",
            "}")
        .expectNoReport()
        .resolveRemainingErrors()
        .start();
  }

  @Test
  public void dereferenceAddPreconditionTest() {
    mockChatGPTResponse(new String[] {"NO"});
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "import java.util.Collection;",
            "public class Foo {",
            "   public String toString(@Nullable Collection<?> coll) {",
            "     boolean isEmpty = coll == null || coll.isEmpty();",
            "     if(isEmpty) {",
            "       return \"\";",
            "     }",
            "     return coll.iterator().next().toString();",
            "   }",
            "}")
        .expectNoReport()
        .resolveRemainingErrors()
        .start();
  }

  @Test
  public void dereferenceReturnNullForNullableExpressionInNullableMethodTest() {
    mockChatGPTResponse(new String[] {"YES"});
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "import java.util.Collection;",
            "public class Foo {",
            "   public @Nullable String foo(@Nullable Collection<?> coll) {",
            "     return coll.iterator().next().toString();",
            "   }",
            "}")
        .expectNoReport()
        .resolveRemainingErrors()
        .start();
  }

  @Test
  public void dereferenceFieldInitializedBeforeUseTest() {
    mockChatGPTResponse(new String[] {"NO"});
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Foo.java",
            "package test;",
            "public class Foo {",
            "   Object f;",
            "   public void init() {",
            "     this.f = new Object();",
            "   }",
            "   public String bar() {",
            "     return f.toString();",
            "   }",
            "}")
        .expectNoReport()
        .resolveRemainingErrors()
        .start();
  }

  @Test
  public void dereferenceFieldFixGenerationUsingSafeUsageTest() {
    mockChatGPTResponse(new String[] {"NO"});
    coreTestHelper
            .onTarget()
            .withSourceLines(
                    "Foo.java",
                    "package test;",
                    "import javax.annotation.Nullable;",
                    "public class Foo {",
                    "   Object f;",
                    "   public void init() {",
                    "     this.f = new Object();",
                    "   }",
                    "   public String bar() {",
                    "     return f.toString();",
                    "   }",
                    "}")
            .expectNoReport()
            .resolveRemainingErrors()
            .start();
  }
}
