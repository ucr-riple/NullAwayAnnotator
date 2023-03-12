/*
 * MIT License
 *
 * Copyright (c) 2023 Nima Karimipour
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

package edu.ucr.cs.riple.core.adapters;

import com.google.common.base.Preconditions;
import edu.ucr.cs.riple.core.Config;
import edu.ucr.cs.riple.core.metadata.field.FieldDeclarationStore;
import edu.ucr.cs.riple.core.metadata.index.Error;
import edu.ucr.cs.riple.core.metadata.index.NonnullStore;
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

/**
 * NullAway serialization adapter for version 3.
 *
 * <p>Updates to previous version (version 1):
 *
 * <ul>
 *   <li>Serialized errors contain an extra column indicating the offset of the program point where
 *       the error is reported.
 *   <li>Serialized errors contain an extra column indicating the path to the containing source file
 *       where the error is reported
 *   <li>Type arguments and Type use annotations are excluded from the serialized method signatures.
 * </ul>
 */
public class NullAwayV3Adapter extends NullAwayAdapterBaseClass {

  public NullAwayV3Adapter(
      Config config, FieldDeclarationStore fieldDeclarationStore, NonnullStore nonnullStore) {
    super(config, fieldDeclarationStore, nonnullStore);
  }

  @Override
  public TrackerNode deserializeTrackerNode(String[] values) {
    Preconditions.checkArgument(
        values.length == 5,
        "Expected 5 values to create TrackerNode instance in NullAway serialization version 3 but found: "
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
    return 3;
  }
}
