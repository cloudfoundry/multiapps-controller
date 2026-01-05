package org.cloudfoundry.multiapps.controller.persistence.query;

import java.util.Date;

import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdown;

public interface ApplicationShutdownQuery extends Query<ApplicationShutdown, ApplicationShutdownQuery> {

    ApplicationShutdownQuery id(String instanceId);

    ApplicationShutdownQuery applicationId(String applicationId);

    ApplicationShutdownQuery applicationInstanceIndex(int applicationInstanceIndex);

    ApplicationShutdownQuery shutdownStatus(String shutdownStatus);

    ApplicationShutdownQuery startedAt(Date startedAt);
}
