package edu.ucr.cs.riple.core.deserializaition;

import edu.ucr.cs.riple.core.metadata.index.Fix;

public interface Deserializer {
  Error deserializeError(String[] info);

  Fix deserializeFix(String[] info);
}
