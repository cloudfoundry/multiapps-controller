package com.sap.cloud.lm.sl.cf.process.metadata;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.cf.web.api.model.OperationMetadata;

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
                Variables.NO_RESTART_SUBSCRIBED_APPS.getName(),
                Variables.NO_FAIL_ON_MISSING_PERMISSIONS.getName(),
                Variables.ABORT_ON_ERROR.getName(),
                Variables.ENABLE_ENV_DETECTION.getName(),
            // @formatter:on
        };
    }
}