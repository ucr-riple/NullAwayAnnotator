/*
 * MIT License
 *
 * Copyright (c) 2020 Nima Karimipour
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

import com.google.common.collect.ImmutableSet;
import com.uber.nullaway.fixserialization.FixSerializationConfig;
import edu.ucr.cs.css.Serializer;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.FixEntity;
import edu.ucr.cs.riple.core.metadata.method.MethodInheritanceTree;
import edu.ucr.cs.riple.core.metadata.trackers.CompoundTracker;
import edu.ucr.cs.riple.core.util.Utility;
import edu.ucr.cs.riple.injector.Fix;
import edu.ucr.cs.riple.injector.Injector;
import edu.ucr.cs.riple.injector.WorkListBuilder;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class Annotator {
  private Injector injector;
  private Explorer explorer;
  public static final Log log = new Log();
  public Config config;

  public static class Log {
    public int total;
    int requested;
    long time;
    long deep;
    long buildTime = 0;

    @Override
    public String toString() {
      return "total="
          + total
          + ", requested="
          + requested
          + ", time="
          + time
          + ", deep="
          + deep
          + ", buildTime="
          + buildTime;
    }
  }

  private void init(Config config) {
    System.out.println("Making the first build.");
    FixSerializationConfig.Builder builder =
        new FixSerializationConfig.Builder()
            .setSuggest(true, true)
            .setAnnotations(config.nullableAnnot, "UNKNOWN")
            .setOutputDirectory(config.dir.toString());
    buildProject(builder, false);
    Path fixesPath = config.dir.resolve("fixes.path");
    ImmutableSet<Fix> allFixes = Utility.readAllFixes(fixesPath);
    if (config.useCache) {
      Utility.removeCachedFixes(allFixes, config);
    }
    log.total = allFixes.size();
    this.injector =
        Injector.builder()
            .setMode(Injector.MODE.BATCH)
            .keepStyle(config.lexicalPreservationEnabled)
            .build();
    MethodInheritanceTree methodInheritanceTree =
        new MethodInheritanceTree(config.dir.resolve(Serializer.METHOD_INFO_NAME));
    Bank<Error> errorBank = new Bank<>(config.dir.resolve("errors.tsv"), Error::new);
    Bank<FixEntity> fixBank = new Bank<>(fixesPath, FixEntity::new);
    CompoundTracker tracker = new CompoundTracker(config.dir);
    this.explorer =
        new Explorer(this, allFixes, errorBank, fixBank, tracker, methodInheritanceTree);
  }

  public void start(Config config) {
    log.time = System.currentTimeMillis();
    System.out.println("Annotator Started.");
    init(config);
    ImmutableSet<Report> reports = explorer.explore();
    log.deep = System.currentTimeMillis() - log.deep;
    log.time = System.currentTimeMillis() - log.time;
    Utility.writeReports(config, reports);
    Utility.writeLog(config);
  }

  public void remove(List<Fix> fixes) {
    if (fixes == null || fixes.size() == 0) {
      return;
    }
    List<Fix> toRemove =
        fixes
            .stream()
            .map(
                fix ->
                    new Fix(
                        fix.annotation,
                        fix.method,
                        fix.param,
                        fix.location,
                        fix.className,
                        fix.uri,
                        "false"))
            .collect(Collectors.toList());
    apply(toRemove);
  }

  public void apply(List<Fix> fixes) {
    if (fixes == null || fixes.size() == 0) {
      return;
    }
    injector.start(new WorkListBuilder(fixes).getWorkLists(), false);
  }

  public void buildProject(FixSerializationConfig.Builder writer, boolean count) {
    if (count) {
      log.requested++;
    }
    writer.writeAsXML(config.nullAwayConfigPath.toString());
    try {
      long start = System.currentTimeMillis();
      Utility.executeCommand(config.buildCommand);
      log.buildTime += System.currentTimeMillis() - start;
    } catch (Exception e) {
      throw new RuntimeException("Could not run command: " + config.buildCommand);
    }
  }

  public void buildProject(FixSerializationConfig.Builder writer) {
    buildProject(writer, true);
  }
}
