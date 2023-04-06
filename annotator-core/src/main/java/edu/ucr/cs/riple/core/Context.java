package edu.ucr.cs.riple.core;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.metadata.field.FieldRegistry;
import edu.ucr.cs.riple.core.metadata.index.NonnullStore;
import edu.ucr.cs.riple.core.metadata.method.MethodRegistry;
import edu.ucr.cs.riple.core.util.FixSerializationConfig;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.location.Location;

public class Context {

  private final FieldRegistry fieldRegistry;
  private final MethodRegistry methodRegistry;
  private final NonnullStore nonnullStore;
  private final ImmutableSet<ModuleInfo> modules;

  public Context(Config config, ModuleInfo moduleInfo, String buildCommand) {
    this(config, ImmutableSet.of(moduleInfo), buildCommand);
  }

  public Context(Config config, ImmutableSet<ModuleInfo> modules, String buildCommand) {
    Utility.setScannerCheckerActivation(config, modules, true);
    modules.forEach(
        module -> {
          FixSerializationConfig.Builder nullAwayConfig =
              new FixSerializationConfig.Builder()
                  .setSuggest(true, true)
                  .setOutputDirectory(module.dir.toString())
                  .setFieldInitInfo(true);
          nullAwayConfig.writeAsXML(module.nullawayConfig.toString());
        });
    Utility.build(config, buildCommand);
    Utility.setScannerCheckerActivation(config, modules, false);
    this.modules = modules;
    this.nonnullStore = new NonnullStore(config, modules);
    this.fieldRegistry = new FieldRegistry(config, modules);
    this.methodRegistry = new MethodRegistry(config);
  }

  public FieldRegistry getFieldRegistry() {
    return fieldRegistry;
  }

  public MethodRegistry getMethodRegistry() {
    return methodRegistry;
  }

  public NonnullStore getNonnullStore() {
    return nonnullStore;
  }

  public ImmutableSet<ModuleInfo> getModules() {
    return modules;
  }

  public boolean declaredInModule(Location location) {
    return methodRegistry.declaredInModule(location);
  }
}
