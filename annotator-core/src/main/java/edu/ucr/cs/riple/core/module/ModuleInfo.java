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

package edu.ucr.cs.riple.core.module;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.Context;
import edu.ucr.cs.riple.core.metadata.field.FieldRegistry;
import edu.ucr.cs.riple.core.metadata.index.NonnullStore;
import edu.ucr.cs.riple.core.metadata.method.MethodRegistry;
import edu.ucr.cs.riple.core.metadata.region.CompoundRegionRegistry;
import edu.ucr.cs.riple.core.metadata.region.RegionRegistry;
import edu.ucr.cs.riple.core.util.FixSerializationConfig;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnClass;

/** This class is used to store the code structural information about the module. */
public class ModuleInfo {

  /** This field is used to store the information about the fields in the module. */
  private final FieldRegistry fieldRegistry;
  /** This field is used to store the information about the methods in the module. */
  private final MethodRegistry methodRegistry;
  /** This field is used to store the information about the nonnull annotations in the module. */
  private final NonnullStore nonnullStore;
  /** The set of modules this moduleInfo is created for. */
  private final ImmutableSet<ModuleConfiguration> configurations;

  private final RegionRegistry regionRegistry;

  /**
   * This constructor is used to create a moduleInfo for a single module.
   *
   * @param context Annotator context.
   * @param moduleConfiguration The module info.
   * @param buildCommand The command to build the passed module.
   */
  public ModuleInfo(Context context, ModuleConfiguration moduleConfiguration, String buildCommand) {
    this(context, ImmutableSet.of(moduleConfiguration), buildCommand);
  }

  /**
   * This constructor is used to create a moduleInfo for a set of modules.
   *
   * @param context Annotator context.
   * @param configurations The set of modules.
   * @param buildCommand The command to build the passed modules.
   */
  public ModuleInfo(
      Context context, ImmutableSet<ModuleConfiguration> configurations, String buildCommand) {
    // Build with scanner checker activated to generate required files to create the moduleInfo.
    Utility.enableNullAwaySerialization(configurations);
    Utility.runScannerChecker(context, configurations, buildCommand);
    configurations.forEach(
        module -> {
          FixSerializationConfig.Builder nullAwayConfig =
              new FixSerializationConfig.Builder()
                  .setSuggest(true, true)
                  .setOutputDirectory(module.dir.toString())
                  .setFieldInitInfo(true);
          nullAwayConfig.writeAsXML(module.nullawayConfig.toString());
        });
    this.configurations = configurations;
    this.nonnullStore = new NonnullStore(configurations);
    this.fieldRegistry = new FieldRegistry(configurations);
    this.methodRegistry = new MethodRegistry(context);
    this.regionRegistry = new CompoundRegionRegistry(context.config, this);
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
   * Getter for the set of module configurations this moduleInfo is created for.
   *
   * @return The set of module configurations this moduleInfo is created for.
   */
  public ImmutableSet<ModuleConfiguration> getModuleConfigurations() {
    return configurations;
  }

  /**
   * Checks if the passed location is declared in this moduleInfo's modules.
   *
   * @param location The location to check.
   * @return True if the passed location is declared in this moduleInfo's modules, false otherwise.
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

  public RegionRegistry getRegionRegistry() {
    return regionRegistry;
  }
}
