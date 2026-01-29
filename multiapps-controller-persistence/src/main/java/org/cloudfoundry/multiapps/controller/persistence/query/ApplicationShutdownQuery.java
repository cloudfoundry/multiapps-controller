package org.cloudfoundry.multiapps.controller.persistence.query;

import java.time.LocalDateTime;

import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdown;

public interface ApplicationShutdownQuery extends Query<ApplicationShutdown, ApplicationShutdownQuery> {

    ApplicationShutdownQuery id(String instanceId);

    ApplicationShutdownQuery applicationId(String applicationId);

    ApplicationShutdownQuery applicationInstanceIndex(int applicationInstanceIndex);

    ApplicationShutdownQuery shutdownStatus(ApplicationShutdown.Status shutdownStatus);

    ApplicationShutdownQuery startedAt(LocalDateTime startedAt);

    ApplicationShutdownQuery startedAtBefore(LocalDateTime startedAt);
}
