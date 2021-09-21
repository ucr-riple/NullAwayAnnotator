package edu.ucr.cs.riple.autofixer.metadata.index;

public interface Factory<T extends Hashable> {

  T build(String[] infos);
}
