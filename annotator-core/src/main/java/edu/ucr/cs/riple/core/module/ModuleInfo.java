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
import edu.ucr.cs.riple.core.registries.field.FieldRegistry;
import edu.ucr.cs.riple.core.registries.index.Fix;
import edu.ucr.cs.riple.core.registries.index.NonnullStore;
import edu.ucr.cs.riple.core.registries.method.MethodRegistry;
import edu.ucr.cs.riple.core.registries.region.CompoundRegionRegistry;
import edu.ucr.cs.riple.core.registries.region.RegionRegistry;
import edu.ucr.cs.riple.core.registries.region.generatedcode.AnnotationProcessorHandler;
import edu.ucr.cs.riple.core.registries.region.generatedcode.LombokHandler;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnClass;
import edu.ucr.cs.riple.scanner.Serializer;
import edu.ucr.cs.riple.scanner.generatedcode.SourceType;
import java.nio.file.Path;

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

  /**
   * Region registry that contains information about the regions that can potentially be impacted by
   * a fix.
   */
  private final CompoundRegionRegistry regionRegistry;

  /**
   * The set of annotation processor handlers that are used to process the generated code in this
   * module.
   */
  private final ImmutableSet<AnnotationProcessorHandler> annotationProcessorHandlers;

  private final Context context;

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
    this.context = context;
    this.configurations = configurations;
    // Build with scanner checker activated to generate required files to create the moduleInfo.
    context.checker.prepareConfigFilesForBuild(configurations);
    Utility.runScannerChecker(context, configurations, buildCommand);
    checkScannerConfiguration();
    this.nonnullStore = new NonnullStore(configurations, context);
    this.fieldRegistry = new FieldRegistry(configurations, context);
    this.methodRegistry = new MethodRegistry(context);
    this.regionRegistry = new CompoundRegionRegistry(this, context);
    ImmutableSet.Builder<AnnotationProcessorHandler> builder = new ImmutableSet.Builder<>();
    if (context.config.generatedCodeDetectors.contains(SourceType.LOMBOK)) {
      builder.add(new LombokHandler(this));
    }
    this.annotationProcessorHandlers = builder.build();
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
   * Getter for the module configuration this moduleInfo is created for.
   *
   * @return The module configuration this moduleInfo is created for.
   */
  public ImmutableSet<ModuleConfiguration> getModuleConfiguration() {
    return configurations;
  }

  /**
   * Getter for the Annotator context.
   *
   * @return The Annotator context.
   */
  public Context getContext() {
    return context;
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
   * Checks if the passed location is declared in containing modules.
   *
   * @param location The location to check.
   * @return True if the passed location is declared in containing modules., false otherwise.
   */
  public boolean declaredInModule(Location location) {
    if (location.isOnParameter()) {
      location = location.toMethod();
    }
    if (location.isOnMethod()) {
      return methodRegistry.declaredInModule(location);
    }
    if (location.isOnField()) {
      return fieldRegistry.declaredInModule(location);
    }
    return methodRegistry.declaredInModule(location);
  }

  /**
   * Checks if the passed fix contains only annotation changes on elements declared in the
   * containing modules.
   *
   * @param fix The Fix to check.
   * @return True if the passed fix contains annotation changes only on containing modules.
   */
  public boolean declaredInModule(Fix fix) {
    return fix.changes.stream().allMatch(annot -> declaredInModule(annot.getLocation()));
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

  /**
   * Getter for the created {@link RegionRegistry} instance.
   *
   * @return The created {@link RegionRegistry} instance.
   */
  public CompoundRegionRegistry getRegionRegistry() {
    return regionRegistry;
  }

  /**
   * Getter for the set of annotation processor handlers that are used to process the generated
   * code.
   *
   * @return Immutable set of annotation processor handlers that are used to process the generated
   *     in this module.
   */
  public ImmutableSet<AnnotationProcessorHandler> getAnnotationProcessorHandlers() {
    return annotationProcessorHandlers;
  }

  /** Checks if AnnotatorScanner is executed correctly for the modules. */
  private void checkScannerConfiguration() {
    for (ModuleConfiguration config : configurations) {
      if (config.scannerConfig == null) {
        throw new IllegalArgumentException(
            "AnnotatorScanner configuration is not set for module: " + config);
      }
      // check for existence of one of the serialized files from Scanner. In this case we chose
      // NON_NULL_ELEMENTS_FILE_NAME but any other file would work.
      Path pathToNonnull = config.dir.resolve(Serializer.NON_NULL_ELEMENTS_FILE_NAME);
      if (!pathToNonnull.toFile().exists()) {
        String moduleName = config.id == 0 ? "target" : "dependency " + config.id;
        throw new IllegalArgumentException(
            "AnnotatorScanner is not correctly configured for the module: "
                + moduleName
                + ".\n"
                + "Please verify that the path specified for AnnotatorScanner on line "
                + config.id
                + " in the configuration file matches the path provided in file with -cp/--config-paths, "
                + "and that it is identical to the path specified with -XepOpt:AnnotatorScanner:ConfigPath."
                + "\n"
                + "If the path is set correctly, rerun annotator with -rboserr/--redirect-build-output-stderr flag "
                + "and check compilation output and ensure NullAway and AnnotatorScanner is executing properly.");
      }
    }
  }
}
