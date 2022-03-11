/*
 * Copyright (c) 2022 University of California, Riverside.
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

package edu.ucr.cs.css;

import com.google.errorprone.ErrorProneFlags;

public class Config {

  public final String outputDirectory;
  public final boolean methodTrackerIsActive;
  public final boolean fieldTrackerIsActive;
  public final boolean callTrackerIsActive;
  public final Serializer serializer;

  static final String EP_FL_NAMESPACE = "NullAway";
  static final String FL_FIELD = EP_FL_NAMESPACE + ":ActivateFieldTracker";
  static final String FL_METHOD = EP_FL_NAMESPACE + ":ActivateMethodTracker";
  static final String FL_CALL = EP_FL_NAMESPACE + ":ActivateCallTracker";
  static final String FL_OUTPUT_DIR = EP_FL_NAMESPACE + ":OutputDirectory";

  static final String DEFAULT_PATH = "/tmp/NullAwayFix";

  public Config() {
    methodTrackerIsActive = true;
    fieldTrackerIsActive = true;
    callTrackerIsActive = true;
    outputDirectory = DEFAULT_PATH;
    serializer = new Serializer(this);
  }

  public Config(ErrorProneFlags flags) {
    fieldTrackerIsActive = flags.getBoolean(FL_FIELD).orElse(true);
    methodTrackerIsActive = flags.getBoolean(FL_METHOD).orElse(true);
    callTrackerIsActive = flags.getBoolean(FL_CALL).orElse(true);
    outputDirectory = flags.get(FL_OUTPUT_DIR).orElse(DEFAULT_PATH);
    serializer = new Serializer(this);
  }
}
