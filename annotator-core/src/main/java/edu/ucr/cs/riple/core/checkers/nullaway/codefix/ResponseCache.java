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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ResponseCache {

  /** Database file path. */
  private static final String DB_URL = "jdbc:sqlite:chatgpt_cache.db";

  /** CachedData class to store the prompt and response. */
  public static class CachedData {
    /** Stored prompt. */
    public final String prompt;

    /** Stored response. */
    public final String response;

    public CachedData(String prompt, String response) {
      this.prompt = prompt;
      this.response = response;
    }
  }

  /** Creates the SQLite database and the cache table if it does not exist. */
  public static void createTable() {
    String createTableSQL =
        "CREATE TABLE IF NOT EXISTS cache ("
            + "hash TEXT PRIMARY KEY, "
            + "prompt TEXT, "
            + "response TEXT)";
    try (Connection conn = DriverManager.getConnection(DB_URL);
        Statement stmt = conn.createStatement()) {
      stmt.execute(createTableSQL);
    } catch (SQLException e) {
      throw new RuntimeException("Error creating cache table: " + e.getMessage(), e);
    }
  }

  /**
   * Hashes the prompt using SHA-256 algorithm.
   *
   * @param prompt The prompt to hash.
   * @return The hash of the prompt in hexadecimal format.
   */
  public static String hashPrompt(String prompt) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(prompt.getBytes());
      StringBuilder hexString = new StringBuilder();
      for (byte b : hashBytes) {
        hexString.append(String.format("%02x", b));
      }
      return hexString.toString(); // Return hash in hexadecimal format
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Error hashing prompt: " + e.getMessage(), e);
    }
  }

  /**
   * Retrieves the cached response for a given prompt.
   *
   * @param prompt The prompt to search for in the cache.
   * @return A CachedData object containing the prompt and response, or null if not found.
   */
  public static CachedData getCachedResponse(String prompt) {
    String selectSQL = "SELECT prompt, response FROM cache WHERE hash = ?";

    try (Connection conn = DriverManager.getConnection(DB_URL);
        PreparedStatement stmt = conn.prepareStatement(selectSQL)) {
      // Set the hash of the prompt as the search key
      stmt.setString(1, hashPrompt(prompt));
      // Execute the query
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          // Retrieve the stored prompt and response from the result set
          String storedPrompt = rs.getString("prompt");
          String cachedResponse = rs.getString("response");
          // Return both prompt and response as a CachedData object
          return new CachedData(storedPrompt, cachedResponse);
        } else {
          // No cache found for the given prompt
          return null;
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error retrieving cached response: " + e.getMessage(), e);
    }
  }

  /**
   * Caches the response for a given prompt. If the prompt with the same hash already exists, this
   * will fail.
   *
   * @param prompt The prompt to cache.
   * @param response The response to cache.
   */
  public static void cacheResponse(String prompt, String response) {
    // SQL statement for inserting a new prompt-response pair
    String insertSQL = "INSERT OR FAIL INTO cache (hash, prompt, response) " + "VALUES (?, ?, ?)";
    try (Connection conn = DriverManager.getConnection(DB_URL);
        PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
      stmt.setString(1, hashPrompt(prompt));
      stmt.setString(2, prompt);
      stmt.setString(3, response);
      stmt.executeUpdate();
    } catch (SQLException e) {
      // Check for SQLState related to unique constraint violation which occurs on hash collision
      if ("23000".equals(e.getSQLState())) {
        throw new RuntimeException(
            "Hash collision detected: A prompt with the same hash already exists.", e);
      } else {
        throw new RuntimeException("Error caching response: " + e.getMessage(), e);
      }
    }
  }
}
