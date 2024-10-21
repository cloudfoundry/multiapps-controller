package org.cloudfoundry.multiapps.controller.process.stream;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableArchiveEntryWithStreamPositions.class)
@JsonDeserialize(as = ImmutableArchiveEntryWithStreamPositions.class)
public interface ArchiveEntryWithStreamPositions {

    String getName();

    long getStartPosition();

    long getEndPosition();

    int getCompressionMethod();

    boolean isDirectory();

}
