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

package edu.ucr.cs.riple.annotator.util.io;

import com.google.common.collect.ImmutableMap;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility class for working with TSV (Tab-Separated Values) files.
 *
 * <p>This class provides methods to escape special characters in strings, initialize a TSV file
 * with a header, and add rows to the TSV file.
 */
public class TSVFiles {
  /** Special characters that need to be escaped in TSV files. */
  private static final ImmutableMap<Character, Character> escapes =
      ImmutableMap.of(
          '\n', 'n',
          '\t', 't',
          '\f', 'f',
          '\b', 'b',
          '\r', 'r');

  /**
   * Escapes special characters in string to conform with TSV file formats. The most common
   * convention for lossless conversion is to escape special characters with a backslash according
   * to <a
   * href="https://en.wikipedia.org/wiki/Tab-separated_values#Conventions_for_lossless_conversion_to_TSV">
   * Conventions for lossless conversion to TSV</a>
   *
   * @param str String to process.
   * @return returns a modified str which its special characters are escaped.
   */
  public static String escapeSpecialCharacters(String str) {
    // regex needs "\\" to match character '\', each must also be escaped in string to create "\\",
    // therefore, we need four "\".
    // escape existing backslashes
    str = str.replaceAll(Pattern.quote("\\"), Matcher.quoteReplacement("\\\\"));
    // escape special characters
    for (Map.Entry<Character, Character> entry : escapes.entrySet()) {
      str =
          str.replaceAll(
              String.valueOf(entry.getKey()), Matcher.quoteReplacement("\\" + entry.getValue()));
    }
    return str;
  }

  public static void initialize(Path path, String header) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      throw new RuntimeException("Could not clear file at: " + path, e);
    }
    try (OutputStream os = new FileOutputStream(path.toFile())) {
      header += "\n";
      os.write(header.getBytes(Charset.defaultCharset()), 0, header.length());
      os.flush();
    } catch (IOException e) {
      throw new RuntimeException("Could not finish resetting File at Path: " + path, e);
    }
  }

  /**
   * Appends the given string as a row in the file which the path is given.
   *
   * @param row Row to append.
   * @param path Path to the target file.
   */
  public static void addRow(String row, Path path) {
    // Since there is no method available in API of either javac or errorprone to inform NullAway
    // that the analysis is finished, we cannot open a single stream and flush it within a finalize
    // method. Must open and close a new stream everytime we are appending a new line to a file.
    if (row == null || row.isEmpty()) {
      return;
    }
    row = row + "\n";
    try (OutputStream os = new FileOutputStream(path.toFile(), true)) {
      os.write(row.getBytes(Charset.defaultCharset()), 0, row.length());
      os.flush();
    } catch (IOException e) {
      throw new RuntimeException("Error happened for writing at file: " + path, e);
    }
  }
}
