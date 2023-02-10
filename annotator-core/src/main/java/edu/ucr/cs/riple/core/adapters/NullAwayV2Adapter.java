package edu.ucr.cs.riple.core.adapters;

import com.google.common.base.Preconditions;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.metadata.field.FieldDeclarationStore;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.trackers.Region;
import edu.ucr.cs.riple.core.metadata.trackers.TrackerNode;
import edu.ucr.cs.riple.injector.Helper;
import edu.ucr.cs.riple.injector.location.Location;
import edu.ucr.cs.riple.injector.location.OnField;
import edu.ucr.cs.riple.scanner.generatedcode.SourceType;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class NullAwayV2Adapter extends NullAwayAdapterBaseClass {

  public NullAwayV2Adapter(Config config, FieldDeclarationStore fieldDeclarationStore) {
    super(config, fieldDeclarationStore);
  }

  @Override
  public TrackerNode deserializeTrackerNode(String[] values) {
    Preconditions.checkArgument(
        values.length == 5,
        "Expected 5 values to create TrackerNode instance in NullAway serialization version 1 but found: "
            + values.length);
    return new TrackerNode(
        new Region(values[0], values[1], SourceType.valueOf(values[4])), values[2], values[3]);
  }

  @Override
  public Set<Region> getFieldRegionScope(OnField onField) {
    return onField.variables.stream()
        .map(fieldName -> new Region(onField.clazz, fieldName))
        .collect(Collectors.toSet());
  }

  @Override
  public Error deserializeError(String[] values, FieldDeclarationStore store) {
    Preconditions.checkArgument(
        values.length == 12,
        "Expected 12 values to create Error instance in NullAway serialization version 2 but found: "
            + values.length);
    int offset = Integer.parseInt(values[4]);
    Path path = Helper.deserializePath(values[5]);
    String errorMessage = values[1];
    String errorType = values[0];
    Region region = new Region(values[2], values[3]);
    return createError(
        errorType,
        errorMessage,
        region,
        config.offsetHandler.getOriginalOffset(path, offset),
        Location.createLocationFromArrayInfo(Arrays.copyOfRange(values, 6, 12)),
        fieldDeclarationStore);
  }

  @Override
  public int getVersionNumber() {
    return 2;
  }
}
