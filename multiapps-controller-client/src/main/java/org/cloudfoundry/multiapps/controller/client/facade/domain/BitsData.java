package org.cloudfoundry.multiapps.controller.client.facade.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.controller.client.facade.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@Value.Enclosing
@JsonSerialize(as = ImmutableBitsData.class)
@JsonDeserialize(as = ImmutableBitsData.class)
public interface BitsData extends CloudPackage.PackageData {

    @Nullable
    Checksum getChecksum();

    @Nullable
    String getError();

    @Value.Immutable
    @JsonSerialize(as = ImmutableBitsData.ImmutableChecksum.class)
    @JsonDeserialize(as = ImmutableBitsData.ImmutableChecksum.class)
    interface Checksum {

        @Nullable
        String getAlgorithm();

        @Nullable
        String getValue();

    }

}
