package edu.ucr.cs.riple.core.metadata.index;

public interface Factory<T extends Hashable> {

  T build(String[] infos);
}
