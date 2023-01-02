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
import java.util.Map;
import java.util.Objects;

/** Container class to hold paths to nullaway and scanner config files. */
public class ModuleInfo {
  /** Path to nullaway config. */
  public final Path nullawayConfig;
  /** Path to scanner config. */
  public final Path scannerConfig;
  /** Directory where all serialized data from checkers are located. */
  public final Path dir;

  /**
   * Creates an instance of {@link ModuleInfo} from the given map object. This method is used in
   * constructing {@link ModuleInfo} instances from a JSON object. Please note that array of
   * dictionaries in json are parsed to {@link org.json.JSONArray} of maps. This method is called on
   * each dictionary stored in the parsed collection to create the corresponding instance.
   *
   * @param id Global unique id for this module.
   * @param globalDir Global path for all Annotator/Scanner/NullAway outputs.
   * @param mapFromJSON Map Object to retrieve require values.
   * @return An instance of {@link ModuleInfo}.
   */
  public static ModuleInfo buildFromMap(int id, Path globalDir, Map<?, ?> mapFromJSON) {
    String nullawayConfigPath = (String) mapFromJSON.get("NULLAWAY");
    String scannerConfigPath = (String) mapFromJSON.get("SCANNER");
    if (nullawayConfigPath == null || scannerConfigPath == null) {
      throw new IllegalArgumentException(
          "Both paths to NullAway and Scanner config files must be set with NULLAWAY and SCANNER keys!");
    }
    return new ModuleInfo(
        id, globalDir, Paths.get(nullawayConfigPath), Paths.get(scannerConfigPath));
  }

  public ModuleInfo(int id, Path globalDir, Path nullawayConfig, Path scannerConfig) {
    this.nullawayConfig = nullawayConfig;
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
    if (!(o instanceof ModuleInfo)) {
      return false;
    }
    ModuleInfo that = (ModuleInfo) o;
    return nullawayConfig.equals(that.nullawayConfig) && scannerConfig.equals(that.scannerConfig);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nullawayConfig, scannerConfig);
  }
}
