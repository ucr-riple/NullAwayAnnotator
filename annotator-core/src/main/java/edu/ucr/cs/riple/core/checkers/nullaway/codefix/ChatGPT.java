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

package edu.ucr.cs.riple.core.checkers.nullaway.codefix;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.ucr.cs.riple.annotator.util.parsers.JsonParser;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.Context;
import edu.ucr.cs.riple.core.checkers.nullaway.NullAway;
import edu.ucr.cs.riple.core.checkers.nullaway.NullAwayError;
import edu.ucr.cs.riple.core.registries.method.invocation.InvocationRecord;
import edu.ucr.cs.riple.core.registries.region.Region;
import edu.ucr.cs.riple.core.util.ASTParser;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.SourceCode;
import edu.ucr.cs.riple.injector.changes.MethodRewriteChange;
import edu.ucr.cs.riple.injector.changes.RegionRewrite;
import edu.ucr.cs.riple.injector.location.OnMethod;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  /**
   * The prompt to ask ChatGPT to fix the dereference error by generating a code fix using all
   * regions.
   */
  private final String dereferenceFixByAllRegionsPrompt;

  /** Prompt to return null if the expression is null within a nullable method. */
  private final String rewriteReturnNullForNullableMethodPrompt;

  /** Prompt to rewrite the expression to be non-nullable using cast to nonnull. */
  private final String rewriteCastToNonnullPrompt;

  /** The prompt to ask ChatGPT to check if the expression can be null at the error point. */
  private final String checkIfExpressionCanBeNullAtErrorPointPrompt;

  /** The prompt to ask ChatGPT to check if the method is an initializer. */
  private final String checkIfMethodIsAnInitializerPrompt;

  /** The prompt to ask ChatGPT to check if the parameter is nullable. */
  private final String checkIfParamIsNullablePrompt;

  /** The prompt to ask ChatGPT to check if the method is returning nullable. */
  private final String checkIfMethodIsReturningNullablePrompt;

  /** The prompt to ask ChatGPT to check if the method is returning nullable on the call site. */
  private final String checkIfMethodReturnsNullableAtCallSitePrompt;

  /** Simple prompt to ask ChatGPT to fix the error in place. */
  private final String basicFixRequestPrompt;

  /** The logger instance. */
  private final Logger logger;

  /** Counter to keep track of the number of requests sent to ChatGPT per error. */
  public static final AtomicInteger count = new AtomicInteger(0);

  /** Cache for the responses from ChatGPT. */
  private final ResponseCache responseCache;

  /** Limit of retries to get a valid response from ChatGPT. */
  private static final int RETRY_LIMIT = 5;

  /**
   * The {@link ASTParser} instance used to parse the source code of the file containing the error.
   */
  private final ASTParser parser;

  public ChatGPT(Config config, ASTParser parser) {
    this.dereferenceEqualsMethodRewritePrompt =
        Utility.readResourceContent("prompts/dereference/equals-rewrite.txt");
    this.dereferenceToStringMethodRewritePrompt =
        Utility.readResourceContent("prompts/dereference/tostring-rewrite.txt");
    this.dereferenceHashCodeMethodRewritePrompt =
        Utility.readResourceContent("prompts/dereference/hashcode-rewrite.txt");
    this.dereferenceFixBySafeRegionsPrompt =
        Utility.readResourceContent("prompts/dereference/fix-by-safe-regions.txt");
    this.dereferenceFixByAllRegionsPrompt =
        Utility.readResourceContent("prompts/dereference/fix-by-all-regions.txt");
    this.rewriteReturnNullForNullableMethodPrompt =
        Utility.readResourceContent("prompts/dereference/return-null-rewrite.txt");
    this.rewriteCastToNonnullPrompt =
        Utility.readResourceContent("prompts/dereference/cast-to-nonnull-rewrite.txt");
    this.checkIfExpressionCanBeNullAtErrorPointPrompt =
        Utility.readResourceContent("prompts/inquiry/is-false-positive.txt");
    this.checkIfMethodIsAnInitializerPrompt =
        Utility.readResourceContent("prompts/inquiry/is-initializer.txt");
    this.checkIfParamIsNullablePrompt =
        Utility.readResourceContent("prompts/inquiry/is-param-nullable.txt");
    this.checkIfMethodIsReturningNullablePrompt =
        Utility.readResourceContent("prompts/inquiry/is-returning-nullable.txt");
    this.checkIfMethodReturnsNullableAtCallSitePrompt =
        Utility.readResourceContent("prompts/inquiry/is-nullable-at-call-site.txt");
    this.basicFixRequestPrompt = Utility.readResourceContent("prompts/basic-fix-request.txt");
    this.parser = parser;
    this.responseCache = new ResponseCache(config);
    this.responseCache.createTable();
    this.logger = LoggerFactory.getLogger(ChatGPT.class);
  }

  /**
   * Ask ChatGPT a question and get a response. This method caches the response for the same prompt
   * to avoid making the same request to ChatGPT multiple times.
   *
   * @param prompt the question to ask.
   * @return the response from ChatGPT.
   */
  public Response ask(String prompt) {
    logger.trace("Asking ChatGPT:\n{}", prompt);
    prompt = preprocessPrompt(prompt);
    ResponseCache.CachedData cachedResponse = responseCache.getCachedResponse(prompt);
    if (cachedResponse != null) {
      System.out.println("Retrieving response from cache");
      logger.trace("Retrieving response from cache");
      String cachedPrompt = cachedResponse.prompt;
      if (cachedPrompt.equals(prompt)) {
        logger.trace("Cache hit for prompt: {}", prompt);
        Optional<Response> response = Response.tryCreate(cachedResponse.response);
        if (response.isPresent()) {
          return response.get();
        }
        logger.trace(
            "Cached response could not be parsed or validated, moving to send request to OpenAI");
      } else {
        logger.trace("Cache collision detected: Cached:\n{}\nPrompt:\n{}", cachedPrompt, prompt);
        throw new RuntimeException("Cache collision detected");
      }
    }
    logger.trace("Sending request to OpenAI...");
    String response = sendRequestToOpenAI(prompt);
    String current = response;
    for (int i = 0; i < RETRY_LIMIT; i++) {
      Optional<Response> maybe;
      try {
        maybe = Response.tryCreate(current);
        if (maybe.isPresent()) {
          logger.trace("Cached response");
          System.out.println("Cached response");
          responseCache.cacheResponse(prompt, response);
          return maybe.get();
        }
      } catch (Exception e) {
        logger.warn("Failed to create Response from OpenAI response: {}", e.getMessage());
        String promptWithFailure =
            "Your previous response could not be parsed or validated.\n\n"
                + "Original prompt:\n"
                + prompt
                + "\n\n"
                + "Your last response was:\n"
                + current
                + "\n\n"
                + "The error encountered was:\n"
                + e.getMessage()
                + "\n\n"
                + "Please try again, ensuring that the response follows the format described in the original prompt.";
        current = sendRequestToOpenAI(promptWithFailure); // ask ChatGPT again
      }
    }
    logger.error("Failed to create valid Response after retries.");
    throw new IllegalStateException("Failed to create valid Response after retries.");
  }

  /**
   * Sends an actual request to ChatGPT to get a response for the prompt.
   *
   * @param prompt the prompt to ask ChatGPT.
   * @return the response from ChatGPT.
   */
  private static String sendRequestToOpenAI(String prompt) {
    if (count.incrementAndGet() > 50) {
      throw new RuntimeException("Exceeded the limit of 50 requests to OpenAI");
    }
    System.out.println("Sending request to OpenAI...");
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
      StringBuilder rawResponse = new StringBuilder();
      while ((line = br.readLine()) != null) {
        rawResponse.append(line);
      }
      br.close();
      String response = extractMessageFromJSONResponse(rawResponse.toString());
      System.out.println("Response received from OpenAI.");
      return response;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Preprocess the prompt to remove any unnecessary information. For now, it removes all
   * occurrences {@code @Nullable}.
   *
   * @param prompt the prompt to preprocess.
   * @return the preprocessed prompt.
   */
  private static String preprocessPrompt(String prompt) {
    // remove all occurrences of @Nullable
    prompt = prompt.replaceAll("@Nullable", "");
    return prompt;
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
   * Fix a dereference error by generating a code fix. The fix is a rewrite of the {@link
   * Object#equals(Object)} method. Instead of comparing on the field directly that might cause of a
   * dereference error, it should simply call {@code Objects.equals} on the field.
   *
   * @param error the error to fix.
   * @param context Annotator context.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code empty set} if the
   *     error cannot be fixed.
   */
  public Set<RegionRewrite> fixDereferenceErrorInEqualsMethod(
      NullAwayError error, Context context) {
    MethodRewriteChange change =
        fixErrorInPlace(error, dereferenceEqualsMethodRewritePrompt, context);
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
   * @param context Annotator context.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {{@code empty set}.
   */
  public Set<RegionRewrite> fixDereferenceErrorBySafeRegions(
      NullAwayError error, Set<Region> safeRegions, Context context) {
    logger.trace("Attempting to fix dereference error by using safe regions");
    String expression = error.getNullableExpression();
    String region = parser.getRegionSourceCode(error.getRegion()).content;
    String prompt =
        String.format(
            dereferenceFixBySafeRegionsPrompt,
            error.position.diagnosticLine,
            expression,
            region,
            constructPromptForRegions(safeRegions));
    logger.trace("Asking if the error can be fixed by using safe regions");
    Response response = ask(prompt);
    if (!response.isSuccessFull()) {
      logger.trace("Response is not successful");
      return Set.of();
    }
    String code = response.getCode();
    logger.trace("Fixing the error by using safe regions with code:\n{}", code);
    return Set.of(
        RegionRewrite.of(error.path, error.getRegion().clazz, error.getRegion().member, code));
  }

  /**
   * Fix a dereference error by generating a code fix using all regions. This method should be used
   * if either the safe regions are not available or the error cannot be fixed by using safe
   * regions.
   *
   * @param error the error to fix.
   * @param safeRegions regions where no error is present.
   * @param errorRegions regions where an error is present.
   * @param context Annotator context.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code empty set} if the
   *     error cannot be fixed.
   */
  public Set<RegionRewrite> fixDereferenceErrorByAllRegions(
      NullAwayError error, Set<Region> safeRegions, Set<Region> errorRegions, Context context) {
    logger.trace("Attempting to fix dereference error by using all regions");
    String expression = error.getNullableExpression();
    String method = parser.getRegionSourceCode(error.getRegion()).content;
    String regionData =
        safeRegions.isEmpty()
            ? "I could not find any use case where the expression does not potentially produce Null Pointer Exception"
            : "I found these code snippets where the expression does not produce Null Pointer Exception but they did not show any specific pattern to rewrite the method: \n"
                + constructPromptForRegions(safeRegions);
    String prompt =
        String.format(
            dereferenceFixByAllRegionsPrompt,
            error.position.diagnosticLine.trim(),
            expression,
            method,
            regionData,
            constructPromptForRegions(errorRegions),
            expression,
            expression);
    logger.trace("Asking if the error can be fixed by using all regions");
    Response response = ask(prompt);
    logger.trace("response: " + response);
    if (!response.isSuccessFull()) {
      logger.trace("Response is not successful");
      return Set.of();
    }
    String code = response.getCode();
    logger.trace("Fixing the error by using all regions with code:\n{}", code);
    return Set.of(
        RegionRewrite.of(error.path, error.getRegion().clazz, error.getRegion().member, code));
  }

  /**
   * Fix a dereference error by generating a code fix. The fix is a rewrite of the {@link
   * Object#toString()} method. The fix is to check if the field use value "null" and if not, call
   * the toString method on the field.
   *
   * @param error the error to fix.
   * @param context Annotator context.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code empty set} if the
   *     error cannot be fixed.
   */
  public Set<RegionRewrite> fixDereferenceErrorInToStringMethod(
      NullAwayError error, Context context) {
    MethodRewriteChange change =
        fixErrorInPlace(error, dereferenceToStringMethodRewritePrompt, context);
    return change == null ? Set.of() : Set.of(change);
  }

  /**
   * Fixes the dereference error by returning null in a nullable method.
   *
   * @param error the error to fix.
   * @param context Annotator context.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code empty set} if
   *     model fails.
   */
  public Set<RegionRewrite> fixDereferenceByReturningNullInNullableMethod(
      NullAwayError error, Context context) {
    String enclosingMethod = parser.getRegionSourceCode(error.getRegion()).content;
    Response response =
        ask(
            String.format(
                rewriteReturnNullForNullableMethodPrompt,
                enclosingMethod,
                error.position.diagnosticLine,
                error.getNullableExpression()));
    if (!response.isSuccessFull()) {
      logger.trace("Response is not successful");
      return Set.of();
    }
    String code = response.getCode();
    logger.trace("Fixing the error by returning null earlier");
    return Set.of(
        new MethodRewriteChange(
            new OnMethod(error.path, error.getRegion().clazz, error.getRegion().member), code));
  }

  /**
   * Fixes the dereference error by returning null in a nullable method.
   *
   * @param error the error to fix.
   * @param context Annotator context.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code empty set} if
   *     model fails.
   */
  public Set<RegionRewrite> fixDereferenceByAddingCastToNonnull(
      NullAwayError error, String reason, Context context) {
    String enclosingMethod = parser.getRegionSourceCode(error.getRegion()).content;
    String prompt =
        String.format(
            rewriteCastToNonnullPrompt,
            reason,
            error.getNullableExpression(),
            enclosingMethod,
            error.position.diagnosticLine,
            error.getNullableExpression(),
            error.getNullableExpression());
    Response response = ask(prompt);
    if (!response.isSuccessFull()) {
      logger.trace("Response is not successful");
      return Set.of();
    }
    String code = response.getCode();
    logger.trace("Fixing the error by adding castToNonnull");
    return Set.of(
        RegionRewrite.of(
            error.path,
            error.getRegion().clazz,
            error.getRegion().member,
            code,
            Set.of(NullAway.CAST_TO_NONNULL)));
  }

  /**
   * Fix a dereference error by generating a code fix. The fix is a rewrite of the {@link
   * Object#hashCode()} method. The fix is to check if the field use value 1 and if not, call the
   * hashCode method on the field.
   *
   * @param error the error to fix.
   * @param context Annotator context.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code empty set} if the
   *     error cannot be fixed.
   */
  public Set<RegionRewrite> fixDereferenceErrorInHashCodeMethod(
      NullAwayError error, Context context) {
    MethodRewriteChange change =
        fixErrorInPlace(error, dereferenceHashCodeMethodRewritePrompt, context);
    if (change == null) {
      return Set.of();
    }
    return Set.of(change);
  }

  /**
   * Fixes the error using the basic prompt. The prompt includes the surrounding code of the error
   * and the error info.
   *
   * @param error the error to fix.
   * @param context Annotator context.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code empty set} if the
   *     model fails.
   */
  public Set<RegionRewrite> fixUsingBasicPrompt(NullAwayError error, Context context) {
    SourceCode src = parser.getRegionSourceCode(error.getRegion());
    int firstLine = error.position.lineNumber - 20;
    if (firstLine < 0) {
      firstLine = 0;
    }
    int lastLine = error.position.lineNumber + 20;
    String errorContext;
    // check if src.range is including the first and last line
    if (src.range.begin.line - 1 < firstLine && src.range.end.line - 1 > lastLine) {
      // the method already includes the first and last line
      errorContext = src.content;
    } else {
      if (src.range.begin.line - 1 < firstLine) {
        firstLine = src.range.begin.line - 1;
      }
      if (src.range.end.line - 1 > lastLine) {
        lastLine = src.range.end.line - 1;
      }
      errorContext = Utility.getLinesFromFile(error.path, firstLine, lastLine);
    }
    String region =
        "the "
            + (error.getRegion().isOnCallable() ? "method: " : "declaration of field: ")
            + error.getRegion().member;
    String prompt =
        String.format(
            basicFixRequestPrompt,
            error.messageType,
            error.message,
            error.position.diagnosticLine,
            region,
            errorContext,
            error.getRegion().member);
    Response response = ask(prompt);
    if (!response.isSuccessFull()) {
      return Set.of();
    }
    String code = response.getCode();
    if (code.isEmpty()) {
      return Set.of();
    }
    return Set.of(
        RegionRewrite.of(error.path, error.getRegion().clazz, error.getRegion().member, code));
  }

  /**
   * Check if the expression can be null at the error point. This method only considers the method
   * body and does not consider the call site.
   *
   * @param error the error to check.
   * @param context Annotator context.
   * @return {@code true} if the error is a false positive, {@code false} otherwise.
   */
  public Response checkNullabilityPossibilityAtErrorPoint(NullAwayError error, Context context) {
    if (!error.getRegion().isOnCallable()) {
      // If the error is not on a callable, we cannot check if the expression and assume it can be
      // null.
      return Response.agree();
    }
    String regionMember = parser.getRegionSourceCode(error.getRegion()).content;
    String prompt = null;
    if (error.messageType.equals("DEREFERENCE_NULLABLE")) {
      // Construct the prompt
      prompt =
          String.format(
              checkIfExpressionCanBeNullAtErrorPointPrompt,
              // nullable expression
              error.getNullableExpression(),
              error.position.diagnosticLine.trim(),
              regionMember);
    }
    if (error.messageType.equals("RETURN_NULLABLE")) {
      prompt =
          String.format(
              checkIfExpressionCanBeNullAtErrorPointPrompt,
              error.position.diagnosticLine.trim(),
              error.position.diagnosticLine.trim(),
              regionMember);
    }
    if (prompt == null) {
      return null;
    }
    logger.trace("Asking if the expression can be null at error point point");
    return ask(prompt);
  }

  /**
   * Checks if the method is an initializer and a good candidate to receive an {@code @Initializer}
   * annotation.
   *
   * @param onMethod the method to check.
   * @param context Annotator context.
   * @return {@code true} if the method is an initializer, {@code false} otherwise.
   */
  public boolean checkIfMethodIsAnInitializer(OnMethod onMethod, Context context) {
    // Construct the prompt
    String enclosingMethod =
        parser.getRegionSourceCode(new Region(onMethod.clazz, onMethod.method)).content;
    String prompt = String.format(checkIfMethodIsAnInitializerPrompt, enclosingMethod);
    logger.trace("Asking if the method is an initializer: {}", onMethod.method);
    Response response = ask(prompt);
    return response.isAgreement();
  }

  /**
   * Check if the parameter is nullable given the call context of the method.
   *
   * @param encClass the enclosing class of the method.
   * @param method the method to check.
   * @param param the parameter to check. * @return {@code true} if the parameter is nullable,
   *     {@code false} otherwise.
   * @param callContext the call context of the method.
   * @param context Annotator context. * @return {@code true} if the parameter is nullable, {@code
   *     false} otherwise.
   */
  public Response checkIfParamIsNullable(
      String encClass, String method, String param, String callContext, Context context) {
    logger.trace("Asking if the parameter is nullable: {}", param);
    String prompt =
        String.format(
            checkIfParamIsNullablePrompt,
            param,
            callContext,
            parser.getRegionSourceCode(new Region(encClass, method)).content);
    return ask(prompt);
  }

  /**
   * Check if the method is returning nullable given the call context of the method.
   *
   * @param encClass the enclosing class of the method.
   * @param method the method to check.
   * @param callContext the context of the method.
   * @param context Annotator context.
   * @return {@code true} if the method is returning nullable, {@code false} otherwise.
   */
  public Response checkIfMethodIsReturningNullable(
      String encClass, String method, String callContext, Context context) {
    logger.trace("Asking if the method is returning nullable: {}", method);
    String methodSource = parser.getRegionSourceCode(new Region(encClass, method)).content;
    String prompt =
        String.format(checkIfMethodIsReturningNullablePrompt, methodSource, callContext);
    return ask(prompt);
  }

  /**
   * Checks if the method is returning nullable on the call site.
   *
   * @param invocation the invocation of the method.
   * @param callContext the context of the method.
   * @param context Annotator context.
   * @return {@code true} if the method is returning nullable, {@code false} otherwise.
   */
  public Response checkIfMethodIsReturningNullableOnCallSite(
      String invocation, InvocationRecord callContext, Context context) {
    logger.trace("Asking if the method is returning nullable on the call site: {}", invocation);
    String prompt =
        String.format(
            checkIfMethodReturnsNullableAtCallSitePrompt,
            callContext.getRoot(),
            invocation,
            callContext.constructCallGraphContext(),
            invocation,
            invocation);
    return ask(prompt);
  }

  /**
   * Extracts the source code of the regions and constructs a single text that contains the source
   * of regions.
   *
   * @param regions the regions to extract the source code from.
   * @return the source code of the regions.
   */
  private String constructPromptForRegions(Set<Region> regions) {
    return regions.stream()
        .filter(Region::isOnCallable)
        .map(region -> parser.getRegionSourceCode(region).content)
        .collect(Collectors.joining("\n"));
  }

  /**
   * Fix the error in place (by rewriting the method) by asking ChatGPT to generate a code fix.
   *
   * @param error the error to fix.
   * @param prompt the prompt to ask ChatGPT.
   * @param context Annotator context.
   * @return a {@link MethodRewriteChange} that represents the code fix, or {@code null} if the
   *     error cannot be fixed.
   */
  private MethodRewriteChange fixErrorInPlace(NullAwayError error, String prompt, Context context) {
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
