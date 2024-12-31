package edu.ucr.cs.riple.core.checkers.nullaway.codefix.agent;

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

public class ChatGPT {

  private static final String MODEL = "gpt-4o";
  private final String apiKey;
  private final String dereferenceEqualsMethodRewritePrompt;
  private final Config config;

  public ChatGPT(Config config) {
    // read openai-api-key.txt from resources
    this.apiKey = Utility.readResourceContent("openai-api-key.txt");
    this.dereferenceEqualsMethodRewritePrompt = Utility.readResourceContent("prompts/dereference-equals-rewrite.txt");
    this.config = config;
  }

  private String ask(String prompt) {
    String url = "https://api.openai.com/v1/chat/completions";

    try {
      URL obj = new URL(url);
      HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Authorization", "Bearer " + apiKey);
      connection.setRequestProperty("Content-Type", "application/json");

      // The request body
      String body =
          "{\"model\": \""
              + MODEL
              + "\", \"messages\": [{\"role\": \"user\", \"content\": \""
              + prompt
              + "\"}]}";
      connection.setDoOutput(true);
      OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
      writer.write(body);
      writer.flush();
      writer.close();

      // Response from ChatGPT
      BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
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
    String[] enclosingMethod = ASTUtil.getRegionSourceCode(config, error.path, error.getRegion());
    String prompt = String.format(dereferenceEqualsMethodRewritePrompt, enclosingMethod[0]);
    String response = ask(prompt);
    System.out.println(response);
  }
}
