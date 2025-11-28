package org.cloudfoundry.multiapps.controller.persistence.query;

import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdown;

public interface ApplicationShutdownQuery extends Query<ApplicationShutdown, ApplicationShutdownQuery> {

    ApplicationShutdownQuery applicationInstanceId(String applicationInstanceId);

    ApplicationShutdownQuery applicationId(String applicationId);

    ApplicationShutdownQuery applicationInstanceIndex(int applicationInstanceIndex);

}
