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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import org.json.simple.JSONObject;

/** Container class to hold paths to nullaway and scanner config files. */
public class ModuleInfo {
  /** Path to nullaway config. */
  public final Path nullawayConfig;
  /** Path to scanner config. */
  public final Path scannerConfig;
  /** Directory where all serialized data from checkers are located. */
  public final Path dir;
  /** Global counter for assigning unique id for each instance. */
  public static int GLOBAL_ID = 0;

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
    this.dir = globalDir.resolve(String.valueOf(GLOBAL_ID++));
    try {
      Files.deleteIfExists(this.dir);
      if (!this.dir.toFile().mkdirs()) {
        throw new RuntimeException(
            "Could not create output directory for project: " + this.dir.toFile());
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ModuleInfo that = (ModuleInfo) o;
    return nullawayConfig.equals(that.nullawayConfig) && scannerConfig.equals(that.scannerConfig);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nullawayConfig, scannerConfig);
  }
}
