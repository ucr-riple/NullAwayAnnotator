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

package edu.ucr.cs.riple.core.checkers.nullaway.codefix.agent;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.ucr.cs.riple.core.Context;
import edu.ucr.cs.riple.core.checkers.nullaway.NullAwayError;
import edu.ucr.cs.riple.core.registries.method.MethodRecord;
import edu.ucr.cs.riple.core.registries.method.MethodRegistry;
import edu.ucr.cs.riple.core.registries.region.MethodRegionRegistry;
import edu.ucr.cs.riple.core.util.ASTUtil;
import edu.ucr.cs.riple.core.util.JsonParser;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.changes.MethodRewriteChange;
import edu.ucr.cs.riple.injector.location.OnMethod;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Wrapper class to interact with ChatGPT to generate code fixes for {@link NullAwayError}s. */
public class ChatGPT {

  /** The URL to send the request to ChatGPT. */
  private static final String URL = "https://api.openai.com/v1/chat/completions";

  /** The model to use for the request from ChatGPT. */
  private static final String MODEL = "gpt-4o";

  /** The API key to use for the request from ChatGPT. */
  private final String apiKey;

  /** The prompt to ask ChatGPT to rewrite the {@link Object#equals(Object)} method. */
  private final String dereferenceEqualsMethodRewritePrompt;

  /** The prompt to ask ChatGPT to rewrite the {@link Object#toString()} ()} method. */
  private final String dereferenceToStringMethodRewritePrompt;

  /** The prompt to ask ChatGPT to rewrite the {@link Object#hashCode()} method. */
  private final String dereferenceHashCodeMethodRewritePrompt;

  /** The {@link Context} instance. */
  private final Context context;

  /** The URL to send the request to ChatGPT. */
  private final URL url;

  /**
   * The pattern to extract the code from the response from ChatGPT. The code is in the format:
   * {@code ```java\ncode\n```}.
   */
  private final Pattern codeResponsePattern;

  public ChatGPT(Context context) {
    // read openai-api-key.txt from resources
    this.apiKey = retrieveApiKey();
    this.dereferenceEqualsMethodRewritePrompt =
        Utility.readResourceContent("prompts/dereference/equals-rewrite.txt");
    this.dereferenceToStringMethodRewritePrompt =
        Utility.readResourceContent("prompts/dereference/tostring-rewrite.txt");
    this.dereferenceHashCodeMethodRewritePrompt =
        Utility.readResourceContent("prompts/dereference/hashcode-rewrite.txt");
    this.context = context;
    this.codeResponsePattern = Pattern.compile("```java\\n(.*?)\\n```", Pattern.DOTALL);
    try {
      this.url = new URL(URL);
    } catch (MalformedURLException e) {
      throw new RuntimeException("Error Happened creating URL to: " + URL, e);
    }
  }

  /**
   * Fix a dereference error by generating a code fix. The fix is a rewrite of the {@link
   * Object#equals(Object)} method. Instead of comparing on the field directly that might cause of a
   * dereference error, it should simply call {@code Objects.equals} on the field.
   *
   * @param error the error to fix.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code null} if the
   *     error cannot be fixed.
   */
  public Set<MethodRewriteChange> fixDereferenceErrorInEqualsMethod(NullAwayError error) {
    MethodRewriteChange change = fixErrorInPlace(error, dereferenceEqualsMethodRewritePrompt);
    return change == null ? Set.of() : Set.of(change);
  }

  /**
   * Fix a dereference error by generating a code fix. The fix is a rewrite of the {@link
   * Object#toString()} method. The fix is to check if the field use value "null" and if not, call
   * the toString method on the field.
   *
   * @param error the error to fix.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code null} if the
   *     error cannot be fixed.
   */
  public Set<MethodRewriteChange> fixDereferenceErrorInToStringMethod(NullAwayError error) {
    MethodRewriteChange change = fixErrorInPlace(error, dereferenceToStringMethodRewritePrompt);
    return change == null ? Set.of() : Set.of(change);
  }

  /**
   * Check if the error is a false positive at the error point. If it is a false positive, then the
   * solution is to cast the variable to nonnull.
   *
   * @param error the error to check.
   * @return {@code true} if the error is a false positive, {@code false} otherwise.
   */
  public boolean checkIfFalsePositiveAtErrorPoint(NullAwayError error) {
    // Build a context for prompt generation
    // Retrieve the callers of the method up to a depth of 3.
    List<Set<MethodRecord>> depthRecord =
        computeBFSOnCallGraph(error.getRegion().clazz, error.getRegion().member);
    Pattern pattern = Pattern.compile("dereferenced expression (\\S+) is @Nullable");
    Matcher matcher = pattern.matcher(error.message);
    // Construct the prompt
    StringBuilder prompt = new StringBuilder();
    String expression = matcher.group(1);
    String enclosingMethod =
        ASTUtil.getRegionSourceCode(context.config, error.path, error.getRegion());
    prompt
        .append("In the method below is there a possibility that the expression: ")
        .append(expression)
        .append(" at line: ")
        .append(error.diagnosticLine)
        .append(" be null")
        .append("\n")
        .append(enclosingMethod)
        .append("\n")
        .append(
            "I only need one single word as answer from you. "
                + "Just in case it is not possible to be null JUST SAY NO and if it is possible to be null JUST SAY YES");

    return false;
  }

  /**
   * Fix a dereference error by generating a code fix. The fix is a rewrite of the {@link
   * Object#hashCode()} method. The fix is to check if the field use value 1 and if not, call the
   * hashCode method on the field.
   *
   * @param error the error to fix.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code null} if the
   *     error cannot be fixed.
   */
  public Set<MethodRewriteChange> fixDereferenceErrorInHashCodeMethod(NullAwayError error) {
    MethodRewriteChange change = fixErrorInPlace(error, dereferenceHashCodeMethodRewritePrompt);
    return change == null ? Set.of() : Set.of(change);
  }

  /**
   * Extract the message from the JSON response from ChatGPT. The response is in the format:
   * {"choices":[{"role":"user","content":"..."}]}
   *
   * @param response the JSON response.
   * @return the message from the response.
   */
  private static String extractMessageFromJSONResponse(String response) {
    JsonParser parser = new JsonParser(response);
    List<JsonObject> choices = parser.getArrayValueFromKey("choices").orElse(List.of());
    if (choices.isEmpty()) {
      return "";
    }
    JsonObject choice = choices.get(0);
    return new JsonParser(choice).getValueFromKey("message:content").orElse("").getAsString();
  }

  /**
   * This method retrieves the API key from the local machine environment variable. This mechanism
   * should be changed in future and ask the user to provide a key, or use a different mechanism to
   * store the key. For now, it is fine to use this method so we don't have to expose the API key in
   * the code.
   *
   * @return the API key.
   */
  private String retrieveApiKey() {
    return System.getenv("OPENAI_KEY").trim();
  }

  /**
   * Compute a BFS on the call graph to find the callers of the given method up to a depth of 3.
   *
   * @param clazz the class name of the target method.
   * @param member the member name of the target method.
   * @return A map from depth to the set of methods at that depth.
   */
  private List<Set<MethodRecord>> computeBFSOnCallGraph(String clazz, String member) {
    final MethodRegistry mr = context.targetModuleInfo.getMethodRegistry();
    MethodRecord current = mr.findMethodByName(clazz, member);
    Preconditions.checkArgument(
        current != null, String.format("Method not found: %s#%s", clazz, member));
    Deque<MethodRecord> deque = new ArrayDeque<>();
    final MethodRegionRegistry mrr =
        context.targetModuleInfo.getRegionRegistry().getMethodRegionRegistry();
    int depth = 0;
    deque.add(current);
    List<Set<MethodRecord>> depthRecord = new ArrayList<>();
    while (!deque.isEmpty() && depth++ < 4) {
      Set<MethodRecord> currentDepth = new HashSet<>();
      int size = deque.size();
      for (int i = 0; i < size; i++) {
        MethodRecord method = deque.poll();
        if (method == null) {
          continue;
        }
        currentDepth.add(method);
        for (MethodRecord caller : mrr.getCallers(method)) {
          if (caller == null) {
            continue;
          }
          deque.add(caller);
        }
      }
      depthRecord.add(currentDepth);
    }
    return depthRecord;
  }

  /**
   * Fix the error in place (by rewriting the method) by asking ChatGPT to generate a code fix.
   *
   * @param error the error to fix.
   * @param prompt the prompt to ask ChatGPT.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code null} if the
   *     error cannot be fixed.
   */
  private MethodRewriteChange fixErrorInPlace(NullAwayError error, String prompt) {
    String enclosingMethod =
        ASTUtil.getRegionSourceCode(context.config, error.path, error.getRegion());
    String response = ask(String.format(prompt, enclosingMethod, error.message));
    if (response.isEmpty()) {
      return null;
    }
    String code = parseCode(response);
    if (code.isEmpty()) {
      return null;
    }
    return new MethodRewriteChange(
        new OnMethod(error.path, error.getRegion().clazz, error.getRegion().member), code);
  }

  /**
   * Parse the code from the response from ChatGPT.
   *
   * @param code the code from the response.
   * @return the parsed code.
   */
  private String parseCode(String code) {
    Matcher matcher = codeResponsePattern.matcher(code);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return "";
  }

  /**
   * Ask ChatGPT a question and get a response.
   *
   * @param prompt the question to ask.
   * @return the response from ChatGPT.
   */
  private String ask(String prompt) {
    try {
      // Making a POST request
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Authorization", "Bearer " + apiKey);
      connection.setRequestProperty("Content-Type", "application/json");

      // Request content
      JsonObject message = new JsonObject();
      message.addProperty("role", "user");
      message.addProperty("content", prompt); // No need to escape
      JsonArray messages = new JsonArray();
      messages.add(message);
      JsonObject requestBody = new JsonObject();
      requestBody.addProperty("model", MODEL);
      requestBody.add("messages", messages);
      String body = requestBody.toString();

      // Send request
      connection.setDoOutput(true);
      OutputStreamWriter writer =
          new OutputStreamWriter(connection.getOutputStream(), Charset.defaultCharset());
      writer.write(body);
      writer.flush();
      writer.close();

      // Response from ChatGPT
      BufferedReader br =
          new BufferedReader(
              new InputStreamReader(connection.getInputStream(), Charset.defaultCharset()));
      String line;
      StringBuilder response = new StringBuilder();
      while ((line = br.readLine()) != null) {
        response.append(line);
      }
      br.close();
      return extractMessageFromJSONResponse(response.toString());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
