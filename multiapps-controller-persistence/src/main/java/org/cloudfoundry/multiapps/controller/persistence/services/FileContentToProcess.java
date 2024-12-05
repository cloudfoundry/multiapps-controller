package org.cloudfoundry.multiapps.controller.persistence.services;

import org.immutables.value.Value;

@Value.Immutable
public interface FileContentToProcess {

    String getGuid();

    String getSpaceGuid();

    long getStartOffset();

    long getEndOffset();

}
