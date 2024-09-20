package org.cloudfoundry.multiapps.controller.process.metadata;

import org.cloudfoundry.multiapps.controller.api.model.OperationMetadata;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

public class CtsDeployMetadataTest extends MetadataBaseTest {

    @Override
    protected OperationMetadata getMetadata() {
        return CtsDeployMetadata.getMetadata();
    }

    @Override
    protected String getDiagramId() {
        return Constants.CTS_DEPLOY_SERVICE_ID;
    }

    @Override
    protected String[] getVersions() {
        return new String[] { Constants.SERVICE_VERSION_1_0 };
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
                Variables.NO_CONFIRM.getName(),
                Variables.KEEP_ORIGINAL_APP_NAMES_AFTER_DEPLOY.getName(),
                Variables.CTS_PROCESS_ID.getName(),
                Variables.FILE_LIST.getName(),
                Variables.DEPLOY_URI.getName(),
                Variables.CTS_USERNAME.getName(),
                Variables.CTS_PASSWORD.getName(),
                Variables.APPLICATION_TYPE.getName(),
                Variables.TRANSFER_TYPE.getName(),
                Variables.DEPLOY_STRATEGY.getName(),
                Variables.SHOULD_APPLY_INCREMENTAL_INSTANCES_UPDATE.getName(),
                Variables.APPS_START_TIMEOUT_PROCESS_VARIABLE.getName(),
                Variables.APPS_STAGE_TIMEOUT_PROCESS_VARIABLE.getName(),
                Variables.APPS_UPLOAD_TIMEOUT_PROCESS_VARIABLE.getName(),
                Variables.APPS_TASK_EXECUTION_TIMEOUT_PROCESS_VARIABLE.getName()
            // @formatter:on
        };
    }
}
