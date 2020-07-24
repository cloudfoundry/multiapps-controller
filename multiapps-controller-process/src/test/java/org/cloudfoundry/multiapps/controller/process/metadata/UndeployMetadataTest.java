package org.cloudfoundry.multiapps.controller.process.metadata;

import org.cloudfoundry.multiapps.controller.api.model.OperationMetadata;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

public class UndeployMetadataTest extends MetadataBaseTest {

    @Override
    protected OperationMetadata getMetadata() {
        return UndeployMetadata.getMetadata();
    }

    @Override
    protected String getDiagramId() {
        return Constants.UNDEPLOY_SERVICE_ID;
    }

    @Override
    protected String[] getVersions() {
        return new String[] { Constants.SERVICE_VERSION_1_0 };
    }

    @Override
    protected String[] getParametersIds() {
        return new String[] {
            // @formatter:off
                Variables.DELETE_SERVICES.getName(),
                Variables.DELETE_SERVICE_KEYS.getName(),
                Variables.DELETE_SERVICE_BROKERS.getName(),
                Variables.MTA_ID.getName(),
                Variables.MTA_NAMESPACE.getName(),
                Variables.NO_RESTART_SUBSCRIBED_APPS.getName(),
                Variables.NO_FAIL_ON_MISSING_PERMISSIONS.getName(),
                Variables.ABORT_ON_ERROR.getName(),
                Variables.ENABLE_ENV_DETECTION.getName(),
            // @formatter:on
        };
    }
}