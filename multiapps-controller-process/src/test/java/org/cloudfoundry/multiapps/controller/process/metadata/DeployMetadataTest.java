package org.cloudfoundry.multiapps.controller.process.metadata;

import org.cloudfoundry.multiapps.controller.api.model.OperationMetadata;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

public class DeployMetadataTest extends MetadataBaseTest {

    @Override
    protected OperationMetadata getMetadata() {
        return DeployMetadata.getMetadata();
    }

    @Override
    protected String getDiagramId() {
        return Constants.DEPLOY_SERVICE_ID;
    }

    @Override
    protected String[] getVersions() {
        return new String[] { Constants.SERVICE_VERSION_1_1, Constants.SERVICE_VERSION_1_2 };
    }

    @Override
    protected String[] getParametersIds() {
        return new String[] {
            // @formatter:off
                Variables.APP_ARCHIVE_ID.getName(),
                Variables.EXT_DESCRIPTOR_FILE_ID.getName(),
                Variables.NO_START.getName(),
                Variables.START_TIMEOUT.getName(),
                Variables.MTA_NAMESPACE.getName(),
                Variables.APPLY_NAMESPACE.getName(),
                Variables.VERSION_RULE.getName(),
                Variables.DELETE_SERVICES.getName(),
                Variables.DELETE_SERVICE_KEYS.getName(),
                Variables.DELETE_SERVICE_BROKERS.getName(),
                Variables.FAIL_ON_CRASHED.getName(),
                Variables.MTA_ID.getName(),
                Variables.KEEP_FILES.getName(),
                Variables.NO_RESTART_SUBSCRIBED_APPS.getName(),
                Variables.NO_FAIL_ON_MISSING_PERMISSIONS.getName(),
                Variables.ABORT_ON_ERROR.getName(),
                Variables.MODULES_FOR_DEPLOYMENT.getName(),
                Variables.RESOURCES_FOR_DEPLOYMENT.getName(),
            // @formatter:on
        };
    }
}
