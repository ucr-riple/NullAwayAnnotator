package edu.ucr.cs.riple.core.explorers.impactanalyzers;

import edu.ucr.cs.riple.core.CompilerRunner;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.explorers.suppliers.Supplier;
import edu.ucr.cs.riple.core.global.GlobalAnalyzer;
import edu.ucr.cs.riple.core.injectors.AnnotationInjector;
import edu.ucr.cs.riple.core.metadata.graph.ConflictGraph;
import edu.ucr.cs.riple.core.metadata.graph.Node;
import edu.ucr.cs.riple.core.metadata.index.Bank;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.Fix;
import edu.ucr.cs.riple.core.metadata.method.MethodDeclarationTree;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import edu.ucr.cs.riple.injector.changes.AddMarkerAnnotation;
import edu.ucr.cs.riple.injector.location.Location;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class ImpactAnalyzer {

  protected final MethodDeclarationTree methodDeclarationTree;
  protected final AnnotationInjector injector;
  protected final Bank<Fix> fixBank;
  protected final Bank<Error> errorBank;
  protected final GlobalAnalyzer analyzer;
  protected final Config config;
  protected final CompilerRunner compilerRunner;

  public ImpactAnalyzer(Config config, Supplier supplier) {
    this.config = config;
    this.methodDeclarationTree = supplier.getMethodDeclarationTree();
    this.injector = supplier.getInjector();
    this.fixBank = supplier.getFixBank();
    this.errorBank = supplier.getErrorBank();
    this.analyzer = supplier.getGlobalAnalyzer();
    this.compilerRunner = supplier.getCompilerRunner();
  }

  public abstract void analyzeImpacts(ConflictGraph graph);

  /**
   * Updates list of triggered fixes with fixes triggered from downstream dependencies.
   *
   * @param node Node in process.
   * @param localTriggeredFixes Collection of triggered fixes locally.
   */
  public void addTriggeredFixesFromDownstream(Node node, Collection<Fix> localTriggeredFixes) {
    Set<Location> currentLocationTargetedByTree =
        node.tree.stream().map(Fix::toLocation).collect(Collectors.toSet());
    localTriggeredFixes.addAll(
        analyzer.getImpactedParameters(node.tree).stream()
            .filter(input -> !currentLocationTargetedByTree.contains(input))
            .map(
                onParameter ->
                    new Fix(
                        new AddMarkerAnnotation(onParameter, config.nullableAnnot),
                        "PASSING_NULLABLE",
                        new Region(onParameter.clazz, onParameter.method),
                        false))
            .collect(Collectors.toList()));
  }
}
