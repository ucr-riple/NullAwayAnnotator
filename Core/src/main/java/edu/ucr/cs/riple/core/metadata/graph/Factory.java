package edu.ucr.cs.riple.core.metadata.graph;

import edu.ucr.cs.riple.injector.Fix;

public interface Factory<T extends AbstractNode> {

  T build(Fix fix);
}
