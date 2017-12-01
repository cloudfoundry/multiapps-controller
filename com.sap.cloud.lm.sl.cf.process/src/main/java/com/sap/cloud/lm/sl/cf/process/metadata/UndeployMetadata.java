package com.sap.cloud.lm.sl.cf.process.metadata;

import java.util.HashSet;
import java.util.Set;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.web.api.model.OperationMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.ParameterMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.ParameterMetadata.ParameterType;

public class UndeployMetadata {

    private final static Set<ParameterMetadata> PARAMS = new HashSet<ParameterMetadata>();

    static {
        PARAMS.add(ParameterMetadata.builder().id(Constants.PARAM_DELETE_SERVICES).type(ParameterType.BOOLEAN).defaultValue(false).build());
        PARAMS.add(
            ParameterMetadata.builder().id(Constants.PARAM_DELETE_SERVICE_BROKERS).type(ParameterType.BOOLEAN).defaultValue(false).build());
        PARAMS.add(ParameterMetadata.builder().id(Constants.PARAM_MTA_ID).required(true).type(ParameterType.STRING).build());
        PARAMS.add(ParameterMetadata.builder().id(Constants.PARAM_NO_RESTART_SUBSCRIBED_APPS).defaultValue(false).type(
            ParameterType.BOOLEAN).build());
        PARAMS.add(ParameterMetadata.builder().id(Constants.PARAM_NO_FAIL_ON_MISSING_PERMISSIONS).defaultValue(false).type(
            ParameterType.BOOLEAN).build());
        PARAMS.add(ParameterMetadata.builder().id(Constants.PARAM_ABORT_ON_ERROR).type(ParameterType.BOOLEAN).defaultValue(false).build());
    }

    public static OperationMetadata getMetadata() {
        return OperationMetadata.builder().parameters(PARAMS).processId(Constants.UNDEPLOY_SERVICE_ID).versions(
            Constants.SERVICE_VERSION_1_0).build();
    }
}
