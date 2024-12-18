package org.cloudfoundry.multiapps.controller.process.metadata;

import org.cloudfoundry.multiapps.controller.api.model.OperationMetadata;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

class RevertDeployMetadataTest extends MetadataBaseTest {

    @Override
    protected OperationMetadata getMetadata() {
        return RevertDeployMetadata.getMetadata();
    }

    @Override
    protected String getDiagramId() {
        return Constants.REVERT_DEPLOY_SERVICE_ID;
    }

    @Override
    protected String[] getVersions() {
        return new String[] { Constants.SERVICE_VERSION_1_1, Constants.SERVICE_VERSION_1_2 };
    }

    @Override
    protected String[] getParametersIds() {
        return new String[] { Variables.MTA_NAMESPACE.getName(), Variables.VERSION_RULE.getName(), Variables.MTA_ID.getName(),
            Variables.DELETE_SERVICES.getName(), Variables.NO_FAIL_ON_MISSING_PERMISSIONS.getName(), Variables.ABORT_ON_ERROR.getName(),
            Variables.APPS_START_TIMEOUT_PROCESS_VARIABLE.getName(), Variables.APPS_STAGE_TIMEOUT_PROCESS_VARIABLE.getName(),
            Variables.APPS_UPLOAD_TIMEOUT_PROCESS_VARIABLE.getName(), Variables.APPS_TASK_EXECUTION_TIMEOUT_PROCESS_VARIABLE.getName(),
            Variables.PROCESS_USER_PROVIDED_SERVICES.getName() };
    }

}
