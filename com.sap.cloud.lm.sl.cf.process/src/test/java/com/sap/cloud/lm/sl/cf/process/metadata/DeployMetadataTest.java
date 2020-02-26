package com.sap.cloud.lm.sl.cf.process.metadata;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.web.api.model.OperationMetadata;

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
                Constants.PARAM_APP_ARCHIVE_ID,
                Constants.PARAM_EXT_DESCRIPTOR_FILE_ID,
                Constants.PARAM_NO_START,
                Constants.PARAM_START_TIMEOUT,
                Constants.PARAM_USE_NAMESPACES,
                Constants.PARAM_USE_NAMESPACES_FOR_SERVICES,
                Constants.PARAM_VERSION_RULE,
                Constants.PARAM_DELETE_SERVICES,
                Constants.PARAM_DELETE_SERVICE_KEYS,
                Constants.PARAM_DELETE_SERVICE_BROKERS,
                Constants.PARAM_FAIL_ON_CRASHED,
                Constants.PARAM_MTA_ID,
                Constants.PARAM_KEEP_FILES,
                Constants.PARAM_NO_RESTART_SUBSCRIBED_APPS,
                Constants.PARAM_GIT_URI,
                Constants.PARAM_GIT_REF,
                Constants.PARAM_GIT_REPO_PATH,
                Constants.PARAM_GIT_SKIP_SSL,
                Constants.PARAM_NO_FAIL_ON_MISSING_PERMISSIONS,
                Constants.PARAM_ABORT_ON_ERROR,
                Constants.PARAM_MODULES_FOR_DEPLOYMENT,
                Constants.PARAM_RESOURCES_FOR_DEPLOYMENT,
                Constants.PARAM_VERIFY_ARCHIVE_SIGNATURE,
            // @formatter:on
        };
    }
}
