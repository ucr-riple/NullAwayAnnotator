/*
 * MIT License
 *
 * Copyright (c) 2023 Nima Karimipour
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

package edu.ucr.cs.riple.core.metadata;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.ModuleInfo;
import edu.ucr.cs.riple.core.metadata.field.FieldRegistry;
import edu.ucr.cs.riple.core.metadata.index.NonnullStore;
import edu.ucr.cs.riple.core.metadata.method.MethodRegistry;
import edu.ucr.cs.riple.core.util.FixSerializationConfig;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnClass;

/** This class is used to store the code structural information about the module. */
public class Context {

  /** This field is used to store the information about the fields in the module. */
  private final FieldRegistry fieldRegistry;
  /** This field is used to store the information about the methods in the module. */
  private final MethodRegistry methodRegistry;
  /** This field is used to store the information about the nonnull annotations in the module. */
  private final NonnullStore nonnullStore;
  /** The set of modules this context is created for. */
  private final ImmutableSet<ModuleInfo> modules;

  /**
   * This constructor is used to create a context for a single module.
   *
   * @param config Annotator config.
   * @param moduleInfo The module info.
   * @param buildCommand The command to build the passed module.
   */
  public Context(Config config, ModuleInfo moduleInfo, String buildCommand) {
    this(config, ImmutableSet.of(moduleInfo), buildCommand);
  }

  /**
   * This constructor is used to create a context for a set of modules.
   *
   * @param config Annotator config.
   * @param modules The set of modules.
   * @param buildCommand The command to build the passed modules.
   */
  public Context(Config config, ImmutableSet<ModuleInfo> modules, String buildCommand) {
    // Build with scanner checker activated to generate required files to create the context.
    Utility.setScannerCheckerActivation(config, modules, true);
    modules.forEach(
        module -> {
          FixSerializationConfig.Builder nullAwayConfig =
              new FixSerializationConfig.Builder()
                  .setSuggest(true, true)
                  .setOutputDirectory(module.dir.toString())
                  .setFieldInitInfo(true);
          nullAwayConfig.writeAsXML(module.checkerConfig.toString());
        });
    Utility.build(config, buildCommand);
    Utility.setScannerCheckerActivation(config, modules, false);
    this.modules = modules;
    this.nonnullStore = new NonnullStore(config, modules);
    this.fieldRegistry = new FieldRegistry(config, modules);
    this.methodRegistry = new MethodRegistry(config);
  }

  /**
   * Getter for the created {@link FieldRegistry} instance.
   *
   * @return The created {@link FieldRegistry} instance.
   */
  public FieldRegistry getFieldRegistry() {
    return fieldRegistry;
  }

  /**
   * Getter for the created {@link MethodRegistry} instance.
   *
   * @return The created {@link MethodRegistry} instance.
   */
  public MethodRegistry getMethodRegistry() {
    return methodRegistry;
  }

  /**
   * Getter for the created {@link NonnullStore} instance.
   *
   * @return The created {@link NonnullStore} instance.
   */
  public NonnullStore getNonnullStore() {
    return nonnullStore;
  }

  /**
   * Getter for the set of modules this context is created for.
   *
   * @return The set of modules this context is created for.
   */
  public ImmutableSet<ModuleInfo> getModules() {
    return modules;
  }

  /**
   * Checks if the passed location is declared in this context's modules.
   *
   * @param location The location to check.
   * @return True if the passed location is declared in this context's modules, false otherwise.
   */
  public boolean declaredInModule(Location location) {
    return methodRegistry.declaredInModule(location);
  }

  /**
   * Creates a {@link edu.ucr.cs.riple.injector.location.OnClass} instance targeting the passed
   * classes flat name.
   *
   * @param clazz Enclosing class of the field.
   * @return {@link edu.ucr.cs.riple.injector.location.OnClass} instance targeting the passed
   *     classes flat name.
   */
  public OnClass getLocationOnClass(String clazz) {
    return fieldRegistry.getLocationOnClass(clazz);
  }
}
