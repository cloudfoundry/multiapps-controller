package com.sap.cloud.lm.sl.cf.process.metadata;

import java.util.HashSet;
import java.util.Set;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.cf.web.api.model.ImmutableOperationMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.ImmutableParameterMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.OperationMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.ParameterMetadata.ParameterType;

public class UndeployMetadata {

    private static final Set<ImmutableParameterMetadata> PARAMS = new HashSet<>();

    static {
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Variables.DELETE_SERVICES.getName())
                                             .type(ParameterType.BOOLEAN)
                                             .defaultValue(Variables.DELETE_SERVICES.getDefaultValue())
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Variables.DELETE_SERVICE_KEYS.getName())
                                             .type(ParameterType.BOOLEAN)
                                             .defaultValue(Variables.DELETE_SERVICE_KEYS.getDefaultValue())
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Variables.DELETE_SERVICE_BROKERS.getName())
                                             .type(ParameterType.BOOLEAN)
                                             .defaultValue(Variables.DELETE_SERVICE_BROKERS.getDefaultValue())
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Variables.MTA_ID.getName())
                                             .required(true)
                                             .type(ParameterType.STRING)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Variables.NO_RESTART_SUBSCRIBED_APPS.getName())
                                             .defaultValue(Variables.NO_RESTART_SUBSCRIBED_APPS.getDefaultValue())
                                             .type(ParameterType.BOOLEAN)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Variables.NO_FAIL_ON_MISSING_PERMISSIONS.getName())
                                             .defaultValue(Variables.NO_FAIL_ON_MISSING_PERMISSIONS.getDefaultValue())
                                             .type(ParameterType.BOOLEAN)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Variables.ABORT_ON_ERROR.getName())
                                             .type(ParameterType.BOOLEAN)
                                             .defaultValue(Variables.ABORT_ON_ERROR.getDefaultValue())
                                             .build());
    }

    private UndeployMetadata() {
    }

    public static OperationMetadata getMetadata() {
        return ImmutableOperationMetadata.builder()
                                         .parameters(PARAMS)
                                         .diagramId(Constants.UNDEPLOY_SERVICE_ID)
                                         .addVersions(Constants.SERVICE_VERSION_1_0)
                                         .build();
    }
}
