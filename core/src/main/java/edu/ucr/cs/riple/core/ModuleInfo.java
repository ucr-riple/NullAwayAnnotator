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

package edu.ucr.cs.riple.core;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.json.simple.JSONObject;

/** Container class to hold paths to nullaway and scanner config files. */
public class ModuleInfo {
  /** Path to nullaway config. */
  public final Path nullawayConfig;
  /** Path to scanner config. */
  public final Path scannerConfig;
  /** Directory where all serialized data from checkers are located. */
  public final Path dir;
  /** Unique id for this build info. */
  private final int id;
  /** Global counter for assigning unique id for each instance. */
  private static int GLOBAL_ID = 0;

  /**
   * Creates an instance of {@link ModuleInfo} from the given json object.
   *
   * @param globalDir Global path for all Annotator/Scanner/NullAway outputs.
   * @param jsonObject Json Object to retrieve values.
   * @return An instance of {@link ModuleInfo}.
   */
  public static ModuleInfo buildFromJson(Path globalDir, JSONObject jsonObject) {
    String nullawayConfigPath = (String) jsonObject.get("NULLAWAY");
    String scannerConfigPath = (String) jsonObject.get("SCANNER");
    if (nullawayConfigPath == null || scannerConfigPath == null) {
      throw new IllegalArgumentException(
          "Both paths to NullAway and Scanner config files must be set with NULLAWAY and SCANNER keys!");
    }
    return new ModuleInfo(globalDir, Paths.get(nullawayConfigPath), Paths.get(scannerConfigPath));
  }

  public ModuleInfo(Path globalDir, Path nullawayConfig, Path scannerConfig) {
    this.nullawayConfig = nullawayConfig;
    this.scannerConfig = scannerConfig;
    this.id = GLOBAL_ID++;
    this.dir = globalDir.resolve(String.valueOf(this.id));
    if (!this.dir.toFile().mkdirs()) {
      throw new RuntimeException(
          "Could not create output directory for project: " + this.dir.toFile());
    }
  }
}
