/*
 * MIT License
 *
 * Copyright (c) 2022 Nima Karimipour
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

package edu.ucr.cs.riple.scanner;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import edu.ucr.cs.riple.scanner.out.MethodInfo;
import java.util.stream.Stream;

/**
 * Container class to store the state of the checker.
 *
 * <p>Note: We could not rely on the given {@link com.google.errorprone.VisitorState#context} since
 * we receive different instances of {@link com.google.errorprone.SubContext} and writes on these
 * instances are not globally reflected.
 */
public class ScannerContext {

  /**
   * MultiMap of visited methods. It stores the set of visited methods paired with their computed
   * hash value.
   */
  private final Multimap<Integer, MethodInfo> visitedMethods;
  /**
   * Last given id to the most recent newly visited method. Used to assign unique ids for each
   * method.
   */
  private int methodId;
  /** Type Annotator Scanner config. */
  private final Config config;

  public ScannerContext(Config config) {
    this.methodId = 0;
    this.visitedMethods = MultimapBuilder.hashKeys().arrayListValues().build();
    this.config = config;
  }

  /**
   * Returns a unique id and increments to prepare it for the next call.
   *
   * @return unique id.
   */
  public int getNextMethodId() {
    return ++methodId;
  }

  /**
   * Adds the method to the set of discovered methods.
   *
   * @param methodInfo method info instance.
   */
  public void visitMethod(MethodInfo methodInfo) {
    this.visitedMethods.put(methodInfo.hashCode(), methodInfo);
  }

  /**
   * Getter for config.
   *
   * @return returns config instance.
   */
  public Config getConfig() {
    return config;
  }

  /**
   * Retrieves stream of {@link MethodInfo} that has the given hash value.
   *
   * @param hashHint Given hash value to filter elements based on.
   * @return Stream of {@link MethodInfo}.
   */
  public Stream<MethodInfo> getVisitedMethodsWithHashHint(int hashHint) {
    return this.visitedMethods.get(hashHint).stream();
  }
}
