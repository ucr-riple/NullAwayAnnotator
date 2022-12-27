package edu.ucr.cs.riple.core.adapters;

import com.google.common.base.Preconditions;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import edu.ucr.cs.riple.injector.location.Location;
import java.nio.file.Paths;
import java.util.Arrays;

public class NullAwayV2Adapter extends NullAwayV1Adapter {
  public NullAwayV2Adapter(Config config) {
    super(config);
  }

  @Override
  public Error deserializeError(String[] values) {
    Preconditions.checkArgument(
        values.length == 12,
        "Expected 12 values to create Error instance in NullAway serialization version 2 but found: "
            + values.length);
    int offset = Integer.parseInt(values[4]);
    String path = values[5].startsWith("file:/") ? values[5].substring(6) : values[5];
    return new Error(
        values[0],
        values[1],
        new Region(values[2], values[3]),
        config.offsetHandler.getOriginalOffset(Paths.get(path), offset),
        Location.createLocationFromArrayInfo(Arrays.copyOfRange(values, 6, 12)));
  }
}
