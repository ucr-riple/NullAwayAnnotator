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

import static edu.ucr.cs.riple.core.checkers.nullaway.codefix.Response.agree;
import static edu.ucr.cs.riple.core.checkers.nullaway.codefix.Response.codeFix;
import static edu.ucr.cs.riple.core.checkers.nullaway.codefix.Response.disagree;
import static org.mockito.ArgumentMatchers.any;

import edu.ucr.cs.riple.core.checkers.nullaway.codefix.ChatGPT;
import edu.ucr.cs.riple.core.checkers.nullaway.codefix.Response;
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

  private void mockChatGPTResponse(Response... responses) {
    if (responses == null) {
      throw new IllegalStateException("Mocked Responses are not set");
    }
    MockedStatic<ChatGPT> chatGPTMocked = Mockito.mockStatic(ChatGPT.class);
    OngoingStubbing<ChatGPT> stubbing = chatGPTMocked.when(() -> ChatGPT.ask(any(), any()));
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
        codeFix(
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
        codeFix("public int hashCode() {", "   return f == null ? 1 : f.hashCode();", "}"));
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
        codeFix(
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
  public void dereferenceCastToNonnullTest() {
    mockChatGPTResponse(disagree());
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "import java.util.Collection;",
            "public class Foo {",
            "   @Nullable Collection<?> coll;",
            "   public String toString(Foo f) {",
            "     boolean isEmpty = f.coll == null || f.coll.isEmpty();",
            "     if(isEmpty) {",
            "       return \"\";",
            "     }",
            "     return f.coll.iterator().next().toString();",
            "   }",
            "}")
        .expectNoReport()
        .resolveRemainingErrors()
        .start();
  }

  @Test
  public void dereferenceReturnNullForNullableExpressionInNullableMethodTest() {
    mockChatGPTResponse(disagree());
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
    mockChatGPTResponse(agree());
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
            "   @Nullable",
            "   public Object getF() {",
            "     return f;",
            "   }",
            "}")
        .expectNoReport()
        .resolveRemainingErrors()
        .start();
  }

  @Test
  public void dereferenceFieldFixGenerationUsingSafeUsageTest() {
    mockChatGPTResponse(
        agree(),
        disagree(),
        codeFix(
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

  @Test
  public void dereferenceNullableParameterCastToNonnullTest() {
    //    coreTestHelper
    //        .onTarget()
    //        .withSourceLines(
    //            "Foo.java",
    //            "package test;",
    //            "import javax.annotation.Nullable;",
    //            "public class Foo {",
    //            "   public int exec(@Nullable Bar b){",
    //            "     aaa(b);",
    //            "     return useB(b);",
    //            "   }",
    //            "   public void aaa(Bar b){",
    //            "     if(b == null){",
    //            "       throw new IllegalArgumentException();",
    //            "     }",
    //            "   }",
    //            "   public int useB(@Nullable Bar b){",
    //            "     return b.exec();",
    //            "   }",
    //            "}")
    //        .withSourceLines(
    //            "Bar.java",
    //            "package test;",
    //            "public class Bar {",
    //            "   public int exec() {",
    //            "     return 0;",
    //            "   }",
    //            "}")
    //        .expectNoReport()
    //        .resolveRemainingErrors()
    //        .start();
  }

  @Test
  public void dereferenceMethodTargetMethodSuppressionTest() {
    mockChatGPTResponse(agree(), disagree());
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "import java.util.List;",
            "import java.util.Map;",
            "import java.util.HashMap;",
            "public class Foo {",
            "   @Nullable public String exec(String k, String defaultValue){",
            "     List<String> keys = List.of(\"A\", \"B\");",
            "     if(!keys.contains(k)){",
            "         return defaultValue;",
            "     }",
            "     Map<String, String> map = new HashMap<>();",
            "     for(String key : keys){",
            "          map.put(key, \"val:\" + key);",
            "     }",
            "     return map.get(k);",
            "   }",
            "   public String run(){",
            "     return exec(\"A\", \"def\").toString();",
            "   }",
            "}")
        .expectNoReport()
        .resolveRemainingErrors()
        .start();
  }

  @Test
  public void dereferenceMethodCallSiteSuppressionTest() {
    mockChatGPTResponse(agree(), agree(), disagree());
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "import java.util.List;",
            "import java.util.Map;",
            "import java.util.HashMap;",
            "public class Foo {",
            "     public Map<String, String> map = new HashMap<>();",
            "     @Nullable public String exec(String k, String defaultValue){",
            "     List<String> keys = List.of(\"A\", \"B\");",
            "     if(!keys.contains(k)){",
            "         return defaultValue;",
            "     }",
            "     return map.get(k);",
            "   }",
            "   public String run(){",
            "     return exec(\"C\", \"def\").toString();",
            "   }",
            "}")
        .expectNoReport()
        .resolveRemainingErrors()
        .start();
  }

  @Test
  public void assignFieldNullableTest() {
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "import java.util.List;",
            "import java.util.Map;",
            "import java.util.HashMap;",
            "import com.uber.nullaway.annotations.EnsuresNonNull;",
            "public class Foo {",
            "     Object foo = new Object();",
            "     void close(){",
            "       foo = null;",
            "     }",
            "     @EnsuresNonNull(\"foo\")",
            "     void reopen(){",
            "       foo = new Object();",
            "     }",
            "     public String exec1(){",
            "       return foo.toString();",
            "     }",
            "     public String exec2(){",
            "       return foo.toString();",
            "     }",
            "     public String exec3(){",
            "       return foo.toString();",
            "     }",
            "     public String exec4(){",
            "         if(foo == null){",
            "            reopen();",
            "         }",
            "         return foo.toString();",
            "     }",
            "}")
        .expectNoReport()
        .resolveRemainingErrors()
        .start();
  }

  @Test
  public void nullableReturnCastToNonnull() {
    mockChatGPTResponse(disagree());
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "     String display(@Nullable String s){",
            "       boolean isEmpty = s == null || s.isEmpty();",
            "       if(isEmpty){",
            "         return \"\";",
            "       }",
            "       return s;",
            "     }",
            "     String run1(@Nullable String s){",
            "       return display(s).toString();",
            "     }",
            "     String run2(@Nullable String s){",
            "       return display(s).toString();",
            "     }",
            "}")
        .expectNoReport()
        .resolveRemainingErrors()
        .start();
  }

  @Test
  public void uninitializedFieldTest() {
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "import java.util.List;",
            "import java.util.Map;",
            "import java.util.HashMap;",
            "import com.uber.nullaway.annotations.EnsuresNonNull;",
            "public class Foo {",
            "     Object foo;",
            "     @EnsuresNonNull(\"foo\")",
            "     void reopen(){",
            "       foo = new Object();",
            "     }",
            "     public String exec1(){",
            "       return foo.toString();",
            "     }",
            "     public String exec2(){",
            "       return foo.toString();",
            "     }",
            "     public String exec3(){",
            "       return foo.toString();",
            "     }",
            "     public String exec4(){",
            "         if(foo == null){",
            "            reopen();",
            "         }",
            "         return foo.toString();",
            "     }",
            "}")
        .expectNoReport()
        .resolveRemainingErrors()
        .start();
  }

  @Test
  public void getterForNullableFieldTest() {
    mockChatGPTResponse(agree(), agree());
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "     Object foo;",
            "     void init(){",
            "       this.foo = new Object();",
            "     }",
            "     public Object getFoo(){",
            "       return foo;",
            "     }",
            "}")
        .expectNoReport()
        .toDepth(1)
        .resolveRemainingErrors()
        .start();
  }

  @Test
  public void localVariableInitializedFieldTest() {
    mockChatGPTResponse(agree(), agree());
    coreTestHelper
        .onTarget()
        .withSourceLines(
            "Foo.java",
            "package test;",
            "import javax.annotation.Nullable;",
            "public class Foo {",
            "     @Nullable Object f1;",
            "     void init(){",
            "       this.f1 = new Object();",
            "     }",
            "     public void baz(){",
            "       Object local = f1;",
            "       local.toString();",
            "     }",
            "     public void safeBaz(){",
            "          if(f1 != null){",
            "               Object local = f1;",
            "               local.toString();",
            "          }",
            "     }",
            "}")
        .expectNoReport()
        .deactivateInference()
        .toDepth(1)
        .resolveRemainingErrors()
        .start();
  }
}
