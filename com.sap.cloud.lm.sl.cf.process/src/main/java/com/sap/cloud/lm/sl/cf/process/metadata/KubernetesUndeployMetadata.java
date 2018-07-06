package com.sap.cloud.lm.sl.cf.process.metadata;

import java.util.HashSet;
import java.util.Set;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.web.api.model.OperationMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.ParameterMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.ParameterMetadata.ParameterType;

public class KubernetesUndeployMetadata {

    private static final Set<ParameterMetadata> PARAMS = new HashSet<>();

    static {
        PARAMS.add(ParameterMetadata.builder()
            .id(Constants.PARAM_MTA_ID)
            .required(true)
            .type(ParameterType.STRING)
            .build());
    }

    public static OperationMetadata getMetadata() {
        return OperationMetadata.builder()
            .parameters(PARAMS)
            .activitiDiagramId(Constants.KUBERNETES_UNDEPLOY_SERVICE_ID)
            .versions(Constants.SERVICE_VERSION_1_0)
            .build();
    }
}
