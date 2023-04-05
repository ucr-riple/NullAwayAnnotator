package edu.ucr.cs.riple.core;

import com.google.common.collect.ImmutableSet;
import edu.ucr.cs.riple.core.io.deserializers.CheckerDeserializer;
import edu.ucr.cs.riple.core.io.deserializers.nullaway.NullAwayV3Deserializer;
import edu.ucr.cs.riple.core.metadata.field.FieldRegistry;
import edu.ucr.cs.riple.core.metadata.index.NonnullStore;
import edu.ucr.cs.riple.core.metadata.method.MethodRegistry;
import edu.ucr.cs.riple.core.util.FixSerializationConfig;
import edu.ucr.cs.riple.core.util.Utility;

public class Context {

  private final FieldRegistry fieldRegistry;
  private final MethodRegistry methodRegistry;
  private final NonnullStore nonnullStore;
  private final ImmutableSet<ModuleInfo> modules;

  private final CheckerDeserializer deserializer;

  public Context(Config config, ModuleInfo moduleInfo, String buildCommand) {
    this.modules = ImmutableSet.of(moduleInfo);
    Utility.setScannerCheckerActivation(config, modules, true);
    modules.forEach(
        module -> {
          FixSerializationConfig.Builder nullAwayConfig =
              new FixSerializationConfig.Builder()
                  .setSuggest(true, true)
                  .setOutputDirectory(module.dir.toString())
                  .setFieldInitInfo(false);
          nullAwayConfig.writeAsXML(module.nullawayConfig.toString());
        });
    Utility.build(config, buildCommand);
    Utility.setScannerCheckerActivation(config, modules, false);
    this.nonnullStore = new NonnullStore(config, modules);
    this.fieldRegistry = new FieldRegistry(config, modules);
    this.methodRegistry = new MethodRegistry(config);
    deserializer = new NullAwayV3Deserializer(config, this);
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
}
