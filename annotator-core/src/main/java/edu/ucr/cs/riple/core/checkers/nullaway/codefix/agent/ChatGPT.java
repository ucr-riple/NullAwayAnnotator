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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.checkers.nullaway.NullAwayError;
import edu.ucr.cs.riple.core.util.ASTUtil;
import edu.ucr.cs.riple.core.util.Utility;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

public class ChatGPT {

  private static final String MODEL = "gpt-4o";
  private final String apiKey;
  private final String dereferenceEqualsMethodRewritePrompt;
  private final Config config;

  public ChatGPT(Config config) {
    // read openai-api-key.txt from resources
    this.apiKey = Utility.readResourceContent("openai-api-key.txt").trim();
    this.dereferenceEqualsMethodRewritePrompt =
        Utility.readResourceContent("prompts/dereference-equals-rewrite.txt");
    this.config = config;
  }

  private String ask(String prompt) {
    String url = "https://api.openai.com/v1/chat/completions";

    try {
      // Making a POST request
      URL obj = new URL(url);
      HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
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
      OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), Charset.defaultCharset());
      writer.write(body);
      writer.flush();
      writer.close();

      // Response from ChatGPT
      BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), Charset.defaultCharset()));
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

  private static String extractMessageFromJSONResponse(String response) {
    int start = response.indexOf("content") + 11;
    int end = response.indexOf("\"", start);
    return response.substring(start, end);
  }

  public void fixDereferenceErrorInEqualsMethod(NullAwayError error) {
    String enclosingMethod = ASTUtil.getRegionSourceCode(config, error.path, error.getRegion());
    String prompt = String.format(dereferenceEqualsMethodRewritePrompt, enclosingMethod);
    String response = ask(prompt);
    System.out.println(response);
  }
}
