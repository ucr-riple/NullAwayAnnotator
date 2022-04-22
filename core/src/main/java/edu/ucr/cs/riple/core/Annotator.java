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
import edu.ucr.cs.css.Serializer;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.method.MethodInheritanceTree;
import edu.ucr.cs.riple.core.metadata.trackers.CompoundTracker;
import edu.ucr.cs.riple.core.metadata.trackers.RegionTracker;
import edu.ucr.cs.riple.core.util.Utility;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Annotator {

  private final AnnotationInjector injector;
  private final Config config;
  private final Set<Report> reports;

  public Annotator(Config config) {
    this.config = config;
    this.reports = new HashSet<>();
    this.injector = new AnnotationInjector(config);
  }

  public void start() {
    System.out.println("Annotator Started.");
    preprocess();
    explore();
  }

  private void preprocess() {
    System.out.println("Preprocessing...");
    Utility.activateCSSChecker(config, true);
    this.reports.clear();
    System.out.println("Making the first build.");
    Utility.buildProject(config, true);
    Set<Fix> uninitializedFields =
        Utility.readFixesFromOutputDirectory(config)
            .stream()
            .filter(
                fix ->
                    fix.reason.equals("FIELD_NO_INIT")
                        && fix.location.kind.equals(FixType.FIELD.name))
            .collect(Collectors.toSet());
  }

  private void explore() {
    System.out.println("Making the first build.");
    Utility.buildProject(config);
    ImmutableSet<Fix> fixes = ImmutableSet.copyOf(Utility.readFixesFromOutputDirectory(config));
    Utility.activateCSSChecker(config, false);
    Bank<Error> errorBank = new Bank<>(config.dir.resolve("errors.tsv"), Error::new);
    Bank<Fix> fixBank = new Bank<>(config.dir.resolve("fixes.path"), Fix::new);
    RegionTracker tracker = new CompoundTracker(config.dir);
    MethodInheritanceTree tree =
        new MethodInheritanceTree(config.dir.resolve(Serializer.METHOD_INFO_NAME));
    Explorer explorer = new Explorer(injector, errorBank, fixBank, tracker, tree, fixes, config);
    Utility.activateCSSChecker(config, true);
    ImmutableSet<Report> reports = explorer.explore();
    Utility.writeReports(config, reports);
  }
}
