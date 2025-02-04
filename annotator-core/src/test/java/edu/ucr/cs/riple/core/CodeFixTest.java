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
import edu.ucr.cs.riple.core.checkers.nullaway.codefix.agent.Response;
import org.junit.After;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;

public class CodeFixTest extends AnnotatorBaseCoreTest {

  /** The XML formatted response for the agreement. */
  private static final Response AGREE = toXML("YES");

  /** The XML formatted response for the disagreement. */
  private static final Response DISAGREE = toXML("NO");

  MockedStatic<ChatGPT> responseMockedStatic;

  public CodeFixTest() {
    super("nullable-multi-modular");
  }

  private void mockChatGPTResponse(Response... responses) {
    if (responses == null) {
      throw new IllegalStateException("Mocked Responses are not set");
    }
    MockedStatic<ChatGPT> chatGPTMocked = Mockito.mockStatic(ChatGPT.class);
    OngoingStubbing<ChatGPT> stubbing = chatGPTMocked.when(() -> ChatGPT.ask(any()));
    for (Response response : responses) {
      stubbing = stubbing.thenAnswer(invocation -> response);
    }
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
        toXMLJava(
            "public boolean equals(Object other) {",
            "   if (other == null) {",
            "      return false;",
            "   }",
            "   if (other instanceof Foo) {",
            "      return Objects.equals(f, ((Foo) other).f);",
            "   }",
            "   return false;",
            "}"));
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
        toXMLJava("public int hashCode() {", "   return f == null ? 1 : f.hashCode();", "}"));
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
        toXMLJava(
            "public String toString() {", "   return f == null ? \"null\" : f.toString();", "}"));
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
    mockChatGPTResponse(AGREE);
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
    mockChatGPTResponse(AGREE);
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
    mockChatGPTResponse(AGREE);
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
    mockChatGPTResponse(
        DISAGREE,
        DISAGREE,
        toXMLJava(
            "public int run(){",
            "      if (b == null) {",
            "          return 0;",
            "      }",
            "      return b.exec();",
            "  }"));
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Foo.java",
            "package test;",
            "public class Foo {",
            "   Bar b;",
            "   public void setB(Bar b) {",
            "     this.b = b;",
            "   }",
            "   public int run(){",
            "     return b.exec();",
            "   }",
            "   public int calculate(){",
            "     if(b == null){",
            "       return 0;",
            "     }",
            "     return b.exec();",
            "   }",
            "   public void release(){",
            "     b = null;",
            "   }",
            "}")
        .withSourceLines(
            "Bar.java",
            "package test;",
            "public class Bar {",
            "   public int exec() {",
            "     return 0;",
            "   }",
            "}")
        .expectNoReport()
        .resolveRemainingErrors()
        .start();
  }

  /**
   * Convert the answer to XML format.
   *
   * @param answer the answer to be converted to XML format.
   * @return the XML formatted answer.
   */
  private static Response toXML(String answer) {
    return new Response(String.format("<ans>\n%s\n</ans>", answer));
  }

  /**
   * Convert the answer to XML format where the response is a successful generation of code fix.
   *
   * @param code the code to be converted to XML format.
   * @return the XML formatted answer.
   */
  private static Response toXMLJava(String... code) {
    String xml =
        "<success>true</success>\n" + "<code>\n" + "```java\n" + "%s\n" + "```\n" + "</code>\n";
    return toXML(String.format(xml, String.join("\n", code)));
  }
}
