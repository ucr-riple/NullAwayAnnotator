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

package edu.ucr.cs.riple.core.module;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import org.json.simple.JSONObject;

/** Container class to hold paths to checker and scanner config files. */
public class ModuleConfiguration {

  /** Path to checker config. */
  public final Path checkerConfig;
  /** Path to scanner config. */
  public final Path scannerConfig;
  /** Directory where all serialized data from checkers are located. */
  public final Path dir;

  /**
   * Creates an instance of {@link ModuleConfiguration} from the given json object.
   *
   * @param id Global unique id for this module.
   * @param globalDir Global path for all Annotator/Scanner/NullAway outputs.
   * @param jsonObject Json Object to retrieve values.
   * @return An instance of {@link ModuleConfiguration}.
   */
  public static ModuleConfiguration buildFromJson(int id, Path globalDir, JSONObject jsonObject) {
    String checkerConfigPath = (String) jsonObject.get("CHECKER");
    String scannerConfigPath = (String) jsonObject.get("SCANNER");
    if (checkerConfigPath == null || scannerConfigPath == null) {
      throw new IllegalArgumentException(
          "Both paths to NullAway and Scanner config files must be set with CHECKER and SCANNER keys!");
    }
    return new ModuleConfiguration(
        id, globalDir, Paths.get(checkerConfigPath), Paths.get(scannerConfigPath));
  }

  public ModuleConfiguration(int id, Path globalDir, Path checkerConfig, Path scannerConfig) {
    this.checkerConfig = checkerConfig;
    this.scannerConfig = scannerConfig;
    this.dir = globalDir.resolve(String.valueOf(id));
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
    if (!(o instanceof ModuleConfiguration)) {
      return false;
    }
    ModuleConfiguration that = (ModuleConfiguration) o;
    return checkerConfig.equals(that.checkerConfig) && scannerConfig.equals(that.scannerConfig);
  }

  @Override
  public int hashCode() {
    return Objects.hash(checkerConfig, scannerConfig);
  }
}
