package org.cloudfoundry.multiapps.controller.process.util;

import org.immutables.value.Value;

@Value.Immutable
public interface FileEntryProperties {

    String getGuid();

    String getName();

    String getSpaceGuid();

    long getMaxFileSizeInBytes();
}
