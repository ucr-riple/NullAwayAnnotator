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

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.annotator.util.parsers.XmlParser;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Response a response from the agent. */
public class Response {

  /**
   * Flag to indicate if the response is an agreement. Note if the response is not agreement, it
   * does not mean that the model disagreed.
   */
  private final boolean isAgreement;

  /**
   * Flag to indicate if the response is a disagreement. Note if the response is not disagreement,
   * it does not mean that the model agreed.
   */
  private final boolean isDisagreement;

  /**
   * Reason for the response. This is only available if the response is an agreement or a
   * disagreement.
   */
  private final String reason;

  /** Suggested code from the agent. */
  private final String code;

  /** Flag to indicate if the response is successful. */
  private final boolean success;

  /** Full content of response, used to retrieve values from custom tags. */
  private final String content;

  private static final Pattern RESPONSE_PATTERN =
      Pattern.compile("<response>.*?</response>", Pattern.DOTALL);

  /** The logger instance. */
  private static final Logger logger = LogManager.getLogger(Response.class);

  /**
   * The pattern to extract the code from the response from ChatGPT. The code is in the format:
   * {@code ```java\ncode\n```}.
   */
  private static final Pattern CODE_RESPONSE_PATTERN =
      Pattern.compile("```java\\s*([\\s\\S]*?)\\s*```");

  public Response(String response) {
    logger.debug("Creating Response:\n{}", response);
    Matcher matcher = RESPONSE_PATTERN.matcher(response);
    if (!matcher.find()) {
      throw new IllegalArgumentException("Invalid response format: " + response);
    }
    XmlParser parser = new XmlParser(matcher.group());
    this.content = matcher.group();
    this.isAgreement =
        parser
            .getArrayValueFromTag("/response/value", String.class)
            .orElse("")
            .equalsIgnoreCase("yes");
    this.isDisagreement =
        parser
            .getArrayValueFromTag("/response/value", String.class)
            .orElse("")
            .equalsIgnoreCase("no");
    this.success =
        isDisagreement
            || isAgreement
            || parser.getArrayValueFromTag("/response/success", Boolean.class).orElse(false);
    this.code = parseCode(parser.getValueFromTag("/response/code", String.class).orElse(""));
    this.reason = parser.getArrayValueFromTag("/response/reason", String.class).orElse("");
    logger.debug("Response created:\n{}", this);
  }

  /**
   * Checks if the response is an agreement.
   *
   * @return true if the response is an agreement.
   */
  public boolean isAgreement() {
    return isAgreement;
  }

  /**
   * Checks if the response is a disagreement.
   *
   * @return true if the response is a disagreement.
   */
  public boolean isDisagreement() {
    return isDisagreement;
  }

  /**
   * Checks if the response is successful.
   *
   * @return true if the response is successful.
   */
  public boolean isSuccessFull() {
    return success;
  }

  /**
   * Gets the code from the response.
   *
   * @return the code from the response.
   */
  public String getCode() {
    if (!success) {
      throw new IllegalStateException("Response is not successful");
    }
    return code;
  }

  /**
   * Returns the value of the tag.
   *
   * @param tag the tag to get the value from.
   * @return the value of the tag.
   */
  public String getValueFromTag(String tag) {
    XmlParser parser = new XmlParser(content);
    return parser.getValueFromTag(tag, String.class).orElse("");
  }

  /**
   * Returns the value of the attribute.
   *
   * @param parent the parent tag.
   * @param tag the tag to get the value from.
   * @return the value of the attribute.
   */
  public ImmutableSet<String> getValuesFromTag(String parent, String tag) {
    XmlParser parser = new XmlParser(content);
    return parser.getArrayValueFromTag(parent + "/" + tag, String.class).orElse(ImmutableSet.of());
  }

  /**
   * Parse the code from the response from ChatGPT.
   *
   * @param code the code from the response.
   * @return the parsed code.
   */
  private String parseCode(String code) {
    Matcher matcher = CODE_RESPONSE_PATTERN.matcher(code.trim());
    if (matcher.find()) {
      return matcher.group(1);
    }
    return "";
  }

  @Override
  public String toString() {
    if (isAgreement) {
      return "Agreement: " + reason;
    } else if (isDisagreement) {
      return "Disagreement: " + reason;
    }
    return isSuccessFull() ? getCode() : "Failed";
  }
}
