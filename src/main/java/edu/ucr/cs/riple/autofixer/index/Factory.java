package edu.ucr.cs.riple.autofixer.index;

public interface Factory<T extends Hashable> {

  T build(String[] infos);
}
