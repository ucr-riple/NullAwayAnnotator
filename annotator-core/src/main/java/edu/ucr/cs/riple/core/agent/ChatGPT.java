package edu.ucr.cs.riple.core.agent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;

public class ChatGPT {

  private static final String MODEL = "gpt-4o";
  private final String apiKey;

  public ChatGPT() {
    // read openai-api-key.txt from resources
    try {
      // check if the file is in the resources
      if (getClass().getResource("/openai-api-key.txt") == null) {
        throw new RuntimeException("openai-api-key.txt not found in resources");
      }
      BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(
                  Objects.requireNonNull(getClass().getResourceAsStream("/openai-api-key.txt"))));
      apiKey = reader.readLine();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public String ask(String prompt) {
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

  public static String extractMessageFromJSONResponse(String response) {
    int start = response.indexOf("content") + 11;
    int end = response.indexOf("\"", start);
    return response.substring(start, end);
  }
}
