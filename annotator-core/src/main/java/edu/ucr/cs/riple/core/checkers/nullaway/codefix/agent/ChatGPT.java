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
import edu.ucr.cs.riple.annotator.util.parsers.JsonParser;
import edu.ucr.cs.riple.core.Context;
import edu.ucr.cs.riple.core.checkers.nullaway.NullAwayError;
import edu.ucr.cs.riple.core.registries.method.MethodRecord;
import edu.ucr.cs.riple.core.registries.method.MethodRegistry;
import edu.ucr.cs.riple.core.registries.region.MethodRegionRegistry;
import edu.ucr.cs.riple.core.registries.region.Region;
import edu.ucr.cs.riple.core.util.ASTParser;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.changes.MethodRewriteChange;
import edu.ucr.cs.riple.injector.location.OnMethod;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Wrapper class to interact with ChatGPT to generate code fixes for {@link NullAwayError}s. */
public class ChatGPT {

  /** The URL to send the request to ChatGPT. */
  private static final String URL = "https://api.openai.com/v1/chat/completions";

  /** The model to use for the request from ChatGPT. */
  private static final String MODEL = "gpt-4o";

  /** The API key to use for the request from ChatGPT. */
  private static final String API_KEY = retrieveApiKey();

  /** The prompt to ask ChatGPT to rewrite the {@link Object#equals(Object)} method. */
  private final String dereferenceEqualsMethodRewritePrompt;

  /** The prompt to ask ChatGPT to rewrite the {@link Object#toString()}} method. */
  private final String dereferenceToStringMethodRewritePrompt;

  /** The prompt to ask ChatGPT to rewrite the {@link Object#hashCode()} method. */
  private final String dereferenceHashCodeMethodRewritePrompt;

  /**
   * The prompt to ask ChatGPT to fix the dereference error by generating a code fix using safe
   * regions.
   */
  private final String dereferenceFixBySafeRegionsPrompt;

  /** The prompt to ask ChatGPT to check if the error is a false positive at the error point. */
  private final String checkIfFalsePositiveAtErrorPointPrompt;

  /** The prompt to ask ChatGPT to check if the method is an initializer. */
  private final String checkIfMethodIsAnInitializerPrompt;

  /** The {@link Context} instance. */
  private final Context context;

  /**
   * The {@link ASTParser} instance used to parse the source code of the file containing the error.
   */
  private final ASTParser parser;

  public ChatGPT(Context context, ASTParser parser) {
    this.dereferenceEqualsMethodRewritePrompt =
        Utility.readResourceContent("prompts/dereference/equals-rewrite.txt");
    this.dereferenceToStringMethodRewritePrompt =
        Utility.readResourceContent("prompts/dereference/tostring-rewrite.txt");
    this.dereferenceHashCodeMethodRewritePrompt =
        Utility.readResourceContent("prompts/dereference/hashcode-rewrite.txt");
    this.dereferenceFixBySafeRegionsPrompt =
        Utility.readResourceContent("prompts/dereference/fix-by-safe-region.txt");
    this.checkIfFalsePositiveAtErrorPointPrompt =
        Utility.readResourceContent("prompts/inquiry/is-false-positive.txt");
    this.checkIfMethodIsAnInitializerPrompt =
        Utility.readResourceContent("prompts/inquiry/is-initializer.txt");
    this.context = context;
    this.parser = parser;
  }

  /**
   * Ask ChatGPT a question and get a response.
   *
   * @param prompt the question to ask.
   * @return the response from ChatGPT.
   */
  public static Response ask(String prompt) {
    try {
      // Making a POST request
      HttpURLConnection connection = (HttpURLConnection) new URL(URL).openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Authorization", "Bearer " + API_KEY);
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
      return new Response(extractMessageFromJSONResponse(response.toString()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Fix a dereference error by generating a code fix. The fix is a rewrite of the {@link
   * Object#equals(Object)} method. Instead of comparing on the field directly that might cause of a
   * dereference error, it should simply call {@code Objects.equals} on the field.
   *
   * @param error the error to fix.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code empty set} if the
   *     error cannot be fixed.
   */
  public Set<MethodRewriteChange> fixDereferenceErrorInEqualsMethod(NullAwayError error) {
    MethodRewriteChange change = fixErrorInPlace(error, dereferenceEqualsMethodRewritePrompt);
    if (change == null) {
      return Set.of();
    }
    change.addImport("java.util.Objects");
    return Set.of(change);
  }

  /**
   * Fix a dereference error by generating a code fix using safe regions.
   *
   * @param error the error to fix.
   * @param safeRegions the safe regions to use for the fix.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {{@code empty set} if
   *     the
   */
  public Set<MethodRewriteChange> fixDereferenceErrorBySafeRegions(
      NullAwayError error, Set<Region> safeRegions) {
    String safeRegionExamples =
        safeRegions.stream()
            .filter(Region::isOnCallable)
            .map(region -> parser.getRegionSourceCode(region).content)
            .collect(Collectors.joining("\n"));
    String[] info = NullAwayError.extractPlaceHolderValue(error);
    String expression = info[0];
    String method = parser.getRegionSourceCode(error.getRegion()).content;
    String prompt =
        String.format(
            dereferenceFixBySafeRegionsPrompt,
            error.position.diagnosticLine,
            expression,
            method,
            safeRegionExamples);
    Response response = ask(prompt);
    return Set.of();
  }

  /**
   * Fix a dereference error by generating a code fix. The fix is a rewrite of the {@link
   * Object#toString()} method. The fix is to check if the field use value "null" and if not, call
   * the toString method on the field.
   *
   * @param error the error to fix.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code empty set} if the
   *     error cannot be fixed.
   */
  public Set<MethodRewriteChange> fixDereferenceErrorInToStringMethod(NullAwayError error) {
    MethodRewriteChange change = fixErrorInPlace(error, dereferenceToStringMethodRewritePrompt);
    return change == null ? Set.of() : Set.of(change);
  }

  /**
   * Fix a dereference error by generating a code fix. The fix is a rewrite of the {@link
   * Object#hashCode()} method. The fix is to check if the field use value 1 and if not, call the
   * hashCode method on the field.
   *
   * @param error the error to fix.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code empty set} if the
   *     error cannot be fixed.
   */
  public Set<MethodRewriteChange> fixDereferenceErrorInHashCodeMethod(NullAwayError error) {
    MethodRewriteChange change = fixErrorInPlace(error, dereferenceHashCodeMethodRewritePrompt);
    if (change == null) {
      return Set.of();
    }
    return Set.of(change);
  }

  /**
   * Check if the error is a false positive at the error point. If it is a false positive, then the
   * solution is to cast the variable to nonnull.
   *
   * @param error the error to check.
   * @return {@code true} if the error is a false positive, {@code false} otherwise.
   */
  public boolean checkIfFalsePositiveAtErrorPoint(NullAwayError error) {
    String nullableExpression = NullAwayError.extractPlaceHolderValue(error)[0];
    // Build a context for prompt generation
    // Retrieve the callers of the method up to a depth of 3.
    List<Set<MethodRecord>> depthRecord =
        computeBFSOnCallGraph(error.getRegion().clazz, error.getRegion().member);
    // Construct the prompt
    String enclosingMethod = parser.getRegionSourceCode(error.getRegion()).content;
    String prompt =
        String.format(
            checkIfFalsePositiveAtErrorPointPrompt,
            nullableExpression,
            error.position.diagnosticLine.trim(),
            enclosingMethod);
    Response response = ask(prompt);
    return response.isAgreement();
  }

  /**
   * Checks if the method is an initializer and a good candidate to receive an {@code @Initializer}
   * annotation.
   *
   * @param onMethod the method to check.
   * @return {@code true} if the method is an initializer, {@code false} otherwise.
   */
  public boolean checkIfMethodIsAnInitializer(OnMethod onMethod) {
    // Construct the prompt
    String enclosingMethod =
        parser.getRegionSourceCode(new Region(onMethod.clazz, onMethod.method)).content;
    String prompt = String.format(checkIfMethodIsAnInitializerPrompt, enclosingMethod);
    Response response = ask(prompt);
    return response.isAgreement();
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
  private static String retrieveApiKey() {
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
    String enclosingMethod = parser.getRegionSourceCode(error.getRegion()).content;
    Response response = ask(String.format(prompt, enclosingMethod, error.message));
    if (!response.isSuccessFull()) {
      return null;
    }
    String code = response.getCode();
    if (code.isEmpty()) {
      return null;
    }
    return new MethodRewriteChange(
        new OnMethod(error.path, error.getRegion().clazz, error.getRegion().member), code);
  }
}
