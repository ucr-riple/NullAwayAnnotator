package com.example.tool.core.adapters;

import com.example.tool.core.Config;
import com.example.tool.core.metadata.field.FieldDeclarationStore;
import com.example.tool.core.metadata.index.Error;
import com.example.tool.core.metadata.trackers.Region;
import com.example.tool.core.metadata.trackers.TrackerNode;
import com.google.common.base.Preconditions;
import com.example.tool.core.metadata.index.NonnullStore;
import com.example.tool.injector.Helper;
import com.example.tool.injector.location.Location;
import com.example.tool.injector.location.OnField;
import com.example.too.scanner.generatedcode.SourceType;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class NullAwayV2Adapter extends NullAwayAdapterBaseClass {

  public NullAwayV2Adapter(
          Config config, FieldDeclarationStore fieldDeclarationStore, NonnullStore nonnullStore) {
    super(config, fieldDeclarationStore, nonnullStore);
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
        .collect(Collectors.toCollection(LinkedHashSet::new));
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
