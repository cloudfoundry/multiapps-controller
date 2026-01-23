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
                Variables.MTA_NAMESPACE.getName(),
                Variables.APPLY_NAMESPACE_APP_NAMES.getName(),
                Variables.APPLY_NAMESPACE_SERVICE_NAMES.getName(),
                Variables.APPLY_NAMESPACE_APP_ROUTES.getName(),
                Variables.APPLY_NAMESPACE_AS_SUFFIX.getName(),
                Variables.VERSION_RULE.getName(),
                Variables.DELETE_SERVICES.getName(),
                Variables.DELETE_SERVICE_KEYS.getName(),
                Variables.DELETE_SERVICE_BROKERS.getName(),
                Variables.MTA_ID.getName(),
                Variables.KEEP_FILES.getName(),
                Variables.NO_RESTART_SUBSCRIBED_APPS.getName(),
                Variables.NO_FAIL_ON_MISSING_PERMISSIONS.getName(),
                Variables.ABORT_ON_ERROR.getName(),
                Variables.MODULES_FOR_DEPLOYMENT.getName(),
                Variables.RESOURCES_FOR_DEPLOYMENT.getName(),
                Variables.APPS_START_TIMEOUT_PROCESS_VARIABLE.getName(),
                Variables.APPS_STAGE_TIMEOUT_PROCESS_VARIABLE.getName(),
                Variables.APPS_UPLOAD_TIMEOUT_PROCESS_VARIABLE.getName(),
                Variables.APPS_TASK_EXECUTION_TIMEOUT_PROCESS_VARIABLE.getName(),
                Variables.SKIP_APP_DIGEST_CALCULATION.getName(),
                Variables.IS_SECURITY_ENABLED.getName()
            // @formatter:on
        };
    }
}
