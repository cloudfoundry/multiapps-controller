package com.sap.cloud.lm.sl.cf.process.metadata;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.cf.web.api.model.OperationMetadata;

public class BlueGreenDeployMetadataTest extends MetadataBaseTest {

    @Override
    protected OperationMetadata getMetadata() {
        return BlueGreenDeployMetadata.getMetadata();
    }

    @Override
    protected String getDiagramId() {
        return Constants.BLUE_GREEN_DEPLOY_SERVICE_ID;
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
                Variables.USE_NAMESPACES.getName(),
                Variables.USE_NAMESPACES_FOR_SERVICES.getName(),
                Variables.VERSION_RULE.getName(),
                Variables.DELETE_SERVICES.getName(),
                Variables.DELETE_SERVICE_KEYS.getName(),
                Variables.DELETE_SERVICE_BROKERS.getName(),
                Variables.FAIL_ON_CRASHED.getName(),
                Variables.MTA_ID.getName(),
                Variables.KEEP_FILES.getName(),
                Variables.NO_RESTART_SUBSCRIBED_APPS.getName(),
                Variables.GIT_URI.getName(),
                Variables.GIT_REF.getName(),
                Variables.GIT_REPO_PATH.getName(),
                Variables.GIT_SKIP_SSL.getName(),
                Variables.NO_FAIL_ON_MISSING_PERMISSIONS.getName(),
                Variables.ABORT_ON_ERROR.getName(),
                Variables.MODULES_FOR_DEPLOYMENT.getName(),
                Variables.RESOURCES_FOR_DEPLOYMENT.getName(),
                Variables.VERIFY_ARCHIVE_SIGNATURE.getName(),
                Variables.NO_CONFIRM.getName(),
                Variables.KEEP_ORIGINAL_APP_NAMES_AFTER_DEPLOY.getName(),
            // @formatter:on
        };
    }
}
