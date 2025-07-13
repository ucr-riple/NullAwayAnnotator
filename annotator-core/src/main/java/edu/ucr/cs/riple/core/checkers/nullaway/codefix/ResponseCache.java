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

import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.util.Utility;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResponseCache {

  private final Map<String, String> cache;
  private int lastID = 0;
  public final Path dir;

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

  public ResponseCache(Config config) {
    this.cache = new HashMap<>();
    this.dir =
        Paths.get(
            "/home/nima/Desktop/logs/db_cache/"
                + (config.isTestMode ? "Test" : config.benchmarkName));
    File dir = this.dir.toFile();
    Pattern pattern = Pattern.compile("^(\\d+)\\.txt$");
    File[] files = dir.listFiles();
    if (files == null) {
      files = new File[0];
    }
    System.out.println("Loading cache from " + dir.getAbsolutePath());
    for (File file : files) {
      if (file.isFile()) {
        Matcher matcher = pattern.matcher(file.getName());
        if (matcher.matches()) {
          int id = Integer.parseInt(matcher.group(1));
          if (id > lastID) {
            lastID = id;
          }
          String prompt = normalize(Utility.readFile(dir.toPath().resolve(file.getName())));
          String response = Utility.readFile(dir.toPath().resolve(id + "_response.txt"));
          cache.put(prompt, response);
        }
      }
    }
    System.out.println("Loaded " + cache.size() + " entries from cache.");
  }

  /**
   * Retrieves the cached response for a given prompt.
   *
   * @param prompt The prompt to search for in the cache.
   * @return A CachedData object containing the prompt and response, or null if not found.
   */
  public CachedData getCachedResponse(String prompt) {
    prompt = normalize(prompt);
    if (cache.containsKey(prompt)) {
      return new CachedData(prompt, cache.get(prompt));
    }
    return null;
  }

  /**
   * Caches the response for a given prompt. If the prompt with the same hash already exists, this
   * will fail.
   *
   * @param prompt The prompt to cache.
   * @param response The response to cache.
   */
  public void cacheResponse(String prompt, String response) {
    prompt = normalize(prompt);
    cache.put(prompt, response);
    int id = ++lastID;
    // write to file, I don't have in utility
    try {
      Files.write(dir.resolve(id + ".txt"), prompt.getBytes(StandardCharsets.UTF_8));
      Files.write(dir.resolve(id + "_response.txt"), response.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new RuntimeException("Could not write to cache file.", e);
    }
  }

  public static String normalize(String prompt) {
    return prompt.replace("\r\n", "\n").replaceAll("\\s+", " ").trim();
  }
}
