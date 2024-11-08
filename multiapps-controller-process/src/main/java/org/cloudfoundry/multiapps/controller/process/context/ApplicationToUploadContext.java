package org.cloudfoundry.multiapps.controller.process.context;

import java.util.List;

import org.cloudfoundry.Nullable;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.util.ArchiveEntryWithStreamPositions;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableApplicationToUploadContext.class)
@JsonDeserialize(as = ImmutableApplicationToUploadContext.class)
public interface ApplicationToUploadContext {

    StepLogger getStepLogger();

    CloudApplicationExtended getApplication();

    String getModuleFileName();

    String getSpaceGuid();

    String getCorrelationId();

    String getTaskId();

    String getAppArchiveId();

    @Nullable // TODO: backwards compatibility for one tact
    List<ArchiveEntryWithStreamPositions> getArchiveEntries();
}
