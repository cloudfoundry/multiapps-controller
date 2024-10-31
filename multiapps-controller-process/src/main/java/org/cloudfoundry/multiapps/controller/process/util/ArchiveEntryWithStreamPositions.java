package org.cloudfoundry.multiapps.controller.process.util;

import java.util.Arrays;

import org.cloudfoundry.multiapps.common.SLException;
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

    CompressionMethod getCompressionMethod();

    boolean isDirectory();

    enum CompressionMethod {
        STORED(0), DEFLATED(8);

        private final int value;

        CompressionMethod(int value) {
            this.value = value;
        }

        public static CompressionMethod parseValue(int value) {
            return Arrays.stream(values())
                         .filter(entry -> entry.value == value)
                         .findFirst()
                         .orElseThrow(() -> new SLException("Compression method with value: {0} not found", value));
        }
    }
}
