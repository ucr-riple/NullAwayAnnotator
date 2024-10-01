/*
 * Copyright (c) 2024 University of California, Riverside.
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

package edu.ucr.cs.riple.injector.exceptions;

import com.github.javaparser.ParseProblemException;
import java.nio.file.Path;

/**
 * Exception indicating that an error occurred while parsing a source file.
 *
 * <p>This serves as a wrapper for the {@link ParseProblemException} class, providing a more concise
 * representation of the underlying issue.
 */
public class ParseException extends RuntimeException {

  public ParseException(Path path, ParseProblemException exception) {
    super(retrieveExceptionMessage(path, exception));
  }

  private static String retrieveExceptionMessage(Path path, ParseProblemException e) {
    String message = e.getMessage();
    // If the message contains the stack trace, we should remove it. It does not contain any useful
    // information.
    int index = message.indexOf("Problem stacktrace :");
    message = index == -1 ? message : message.substring(0, index);
    return "javaparser was not able to parse file at: " + path + "\nParse problem:" + message;
  }
}
