package org.cloudfoundry.multiapps.controller.persistence.query;

import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;

public interface LoggingConfigurationQuery extends Query<LoggingConfiguration, LoggingConfigurationQuery> {

    LoggingConfigurationQuery id(String id);

    LoggingConfigurationQuery mtaSpace(String mtaSpace);

    LoggingConfigurationQuery mtaId(String mtaId);

    LoggingConfigurationQuery mtaSpaceId(String mtaSpaceId);

    LoggingConfigurationQuery namespace(String namespace);
}
