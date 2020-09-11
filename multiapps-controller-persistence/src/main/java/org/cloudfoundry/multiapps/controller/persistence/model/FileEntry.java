package org.cloudfoundry.multiapps.controller.persistence.model;

import java.math.BigInteger;
import java.util.Date;

import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableFileEntry.class)
@JsonDeserialize(as = ImmutableFileEntry.class)
public interface FileEntry {

    @Nullable
    String getId();

    @Nullable
    String getName();

    @Nullable
    String getNamespace();

    @Nullable
    String getSpace();

    @Nullable
    BigInteger getSize();

    @Nullable
    String getDigest();

    @Nullable
    String getDigestAlgorithm();

    @Nullable
    Date getModified();

}
