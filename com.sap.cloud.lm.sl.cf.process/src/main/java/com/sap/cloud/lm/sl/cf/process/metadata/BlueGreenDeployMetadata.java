package com.sap.cloud.lm.sl.cf.process.metadata;

import java.util.HashSet;
import java.util.Set;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.cf.web.api.model.ImmutableOperationMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.ImmutableParameterMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.OperationMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.ParameterMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.ParameterMetadata.ParameterType;

public class BlueGreenDeployMetadata {

    private static final Set<ParameterMetadata> PARAMS = new HashSet<>();

    static {
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Variables.APP_ARCHIVE_ID.getName())
                                             .type(ParameterType.STRING)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Variables.EXT_DESCRIPTOR_FILE_ID.getName())
                                             .type(ParameterType.STRING)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Variables.NO_START.getName())
                                             .defaultValue(Variables.NO_START.getDefaultValue())
                                             .type(ParameterType.BOOLEAN)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Variables.START_TIMEOUT.getName())
                                             .defaultValue(Variables.START_TIMEOUT.getDefaultValue())
                                             .type(ParameterType.INTEGER)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Variables.USE_NAMESPACES.getName())
                                             .defaultValue(Variables.USE_NAMESPACES.getDefaultValue())
                                             .type(ParameterType.BOOLEAN)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Variables.USE_NAMESPACES_FOR_SERVICES.getName())
                                             .defaultValue(Variables.USE_NAMESPACES_FOR_SERVICES.getDefaultValue())
                                             .type(ParameterType.BOOLEAN)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Variables.VERSION_RULE.getName())
                                             .defaultValue(Variables.VERSION_RULE.getDefaultValue()
                                                                                 .toString())
                                             .type(ParameterType.STRING)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Variables.DELETE_SERVICES.getName())
                                             .defaultValue(Variables.DELETE_SERVICES.getDefaultValue())
                                             .type(ParameterType.BOOLEAN)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Variables.DELETE_SERVICE_KEYS.getName())
                                             .defaultValue(Variables.DELETE_SERVICE_KEYS.getDefaultValue())
                                             .type(ParameterType.BOOLEAN)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Variables.DELETE_SERVICE_BROKERS.getName())
                                             .defaultValue(Variables.DELETE_SERVICE_BROKERS.getDefaultValue())
                                             .type(ParameterType.BOOLEAN)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Variables.FAIL_ON_CRASHED.getName())
                                             .defaultValue(Variables.FAIL_ON_CRASHED.getDefaultValue())
                                             .type(ParameterType.BOOLEAN)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Variables.MTA_ID.getName())
                                             .type(ParameterType.STRING)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Variables.KEEP_FILES.getName())
                                             .defaultValue(Variables.KEEP_FILES.getDefaultValue())
                                             .type(ParameterType.BOOLEAN)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Variables.NO_RESTART_SUBSCRIBED_APPS.getName())
                                             .defaultValue(Variables.NO_RESTART_SUBSCRIBED_APPS.getDefaultValue())
                                             .type(ParameterType.BOOLEAN)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Variables.GIT_URI.getName())
                                             .defaultValue(Variables.GIT_URI.getDefaultValue())
                                             .type(ParameterType.STRING)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Variables.GIT_REF.getName())
                                             .type(ParameterType.STRING)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Variables.GIT_REPO_PATH.getName())
                                             .type(ParameterType.STRING)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Variables.GIT_SKIP_SSL.getName())
                                             .defaultValue(Variables.GIT_SKIP_SSL.getDefaultValue())
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
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_MODULES_FOR_DEPLOYMENT)
                                             .type(ParameterType.STRING)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_RESOURCES_FOR_DEPLOYMENT)
                                             .type(ParameterType.STRING)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Variables.VERIFY_ARCHIVE_SIGNATURE.getName())
                                             .type(ParameterType.BOOLEAN)
                                             .defaultValue(Variables.VERIFY_ARCHIVE_SIGNATURE.getDefaultValue())
                                             .build());

        // Special blue green deploy parameters:
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Variables.NO_CONFIRM.getName())
                                             .type(ParameterType.BOOLEAN)
                                             .defaultValue(Variables.NO_CONFIRM.getDefaultValue())
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Variables.KEEP_ORIGINAL_APP_NAMES_AFTER_DEPLOY.getName())
                                             .type(ParameterType.BOOLEAN)
                                             .defaultValue(Variables.KEEP_ORIGINAL_APP_NAMES_AFTER_DEPLOY.getDefaultValue())
                                             .build());
    }

    private BlueGreenDeployMetadata() {
    }

    public static OperationMetadata getMetadata() {
        return ImmutableOperationMetadata.builder()
                                         .parameters(PARAMS)
                                         .diagramId(Constants.BLUE_GREEN_DEPLOY_SERVICE_ID)
                                         .addVersions(Constants.SERVICE_VERSION_1_1, Constants.SERVICE_VERSION_1_2)
                                         .build();
    }
}
