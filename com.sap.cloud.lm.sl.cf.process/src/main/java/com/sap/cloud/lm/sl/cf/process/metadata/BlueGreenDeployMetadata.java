package com.sap.cloud.lm.sl.cf.process.metadata;

import java.util.HashSet;
import java.util.Set;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.web.api.model.OperationMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.ParameterMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.ParameterMetadata.ParameterType;
import com.sap.cloud.lm.sl.mta.model.VersionRule;

public class BlueGreenDeployMetadata {

    private static final Set<ParameterMetadata> PARAMS = new HashSet<>();

    static {
        PARAMS.add(ParameterMetadata.builder()
                                    .id(Constants.PARAM_APP_ARCHIVE_ID)
                                    .type(ParameterType.STRING)
                                    .build());
        PARAMS.add(ParameterMetadata.builder()
                                    .id(Constants.PARAM_EXT_DESCRIPTOR_FILE_ID)
                                    .type(ParameterType.STRING)
                                    .build());
        PARAMS.add(ParameterMetadata.builder()
                                    .id(Constants.PARAM_NO_START)
                                    .defaultValue(false)
                                    .type(ParameterType.BOOLEAN)
                                    .build());
        PARAMS.add(ParameterMetadata.builder()
                                    .id(Constants.PARAM_START_TIMEOUT)
                                    .defaultValue(Constants.DEFAULT_START_TIMEOUT)
                                    .type(ParameterType.INTEGER)
                                    .build());
        PARAMS.add(ParameterMetadata.builder()
                                    .id(Constants.PARAM_UPLOAD_TIMEOUT)
                                    .type(ParameterType.INTEGER)
                                    .build());
        PARAMS.add(ParameterMetadata.builder()
                                    .id(Constants.PARAM_USE_NAMESPACES)
                                    .defaultValue(false)
                                    .type(ParameterType.BOOLEAN)
                                    .build());
        PARAMS.add(ParameterMetadata.builder()
                                    .id(Constants.PARAM_USE_NAMESPACES_FOR_SERVICES)
                                    .defaultValue(false)
                                    .type(ParameterType.BOOLEAN)
                                    .build());
        PARAMS.add(ParameterMetadata.builder()
                                    .id(Constants.PARAM_VERSION_RULE)
                                    .defaultValue(VersionRule.SAME_HIGHER.toString())
                                    .type(ParameterType.STRING)
                                    .build());
        PARAMS.add(ParameterMetadata.builder()
                                    .id(Constants.PARAM_DELETE_SERVICES)
                                    .defaultValue(false)
                                    .type(ParameterType.BOOLEAN)
                                    .build());
        PARAMS.add(ParameterMetadata.builder()
                                    .id(Constants.PARAM_DELETE_SERVICE_KEYS)
                                    .defaultValue(false)
                                    .type(ParameterType.BOOLEAN)
                                    .build());
        PARAMS.add(ParameterMetadata.builder()
                                    .id(Constants.PARAM_DELETE_SERVICE_BROKERS)
                                    .defaultValue(false)
                                    .type(ParameterType.BOOLEAN)
                                    .build());
        PARAMS.add(ParameterMetadata.builder()
                                    .id(Constants.PARAM_FAIL_ON_CRASHED)
                                    .defaultValue(true)
                                    .type(ParameterType.BOOLEAN)
                                    .build());
        PARAMS.add(ParameterMetadata.builder()
                                    .id(Constants.PARAM_MTA_ID)
                                    .type(ParameterType.STRING)
                                    .build());
        PARAMS.add(ParameterMetadata.builder()
                                    .id(Constants.PARAM_KEEP_FILES)
                                    .defaultValue(false)
                                    .type(ParameterType.BOOLEAN)
                                    .build());
        PARAMS.add(ParameterMetadata.builder()
                                    .id(Constants.PARAM_NO_RESTART_SUBSCRIBED_APPS)
                                    .defaultValue(false)
                                    .type(ParameterType.BOOLEAN)
                                    .build());
        PARAMS.add(ParameterMetadata.builder()
                                    .id(Constants.PARAM_GIT_URI)
                                    .defaultValue("")
                                    .type(ParameterType.STRING)
                                    .build());
        PARAMS.add(ParameterMetadata.builder()
                                    .id(Constants.PARAM_GIT_REF)
                                    .type(ParameterType.STRING)
                                    .build());
        PARAMS.add(ParameterMetadata.builder()
                                    .id(Constants.PARAM_GIT_REPO_PATH)
                                    .type(ParameterType.STRING)
                                    .build());
        PARAMS.add(ParameterMetadata.builder()
                                    .id(Constants.PARAM_GIT_SKIP_SSL)
                                    .defaultValue(false)
                                    .type(ParameterType.BOOLEAN)
                                    .build());
        PARAMS.add(ParameterMetadata.builder()
                                    .id(Constants.PARAM_NO_FAIL_ON_MISSING_PERMISSIONS)
                                    .defaultValue(false)
                                    .type(ParameterType.BOOLEAN)
                                    .build());
        PARAMS.add(ParameterMetadata.builder()
                                    .id(Constants.PARAM_ABORT_ON_ERROR)
                                    .type(ParameterType.BOOLEAN)
                                    .defaultValue(false)
                                    .build());
        PARAMS.add(ParameterMetadata.builder()
                                    .id(Constants.PARAM_SKIP_OWNERSHIP_VALIDATION)
                                    .type(ParameterType.BOOLEAN)
                                    .defaultValue(false)
                                    .build());
        PARAMS.add(ParameterMetadata.builder()
                                    .id(Constants.PARAM_MODULES_FOR_DEPLOYMENT)
                                    .type(ParameterType.STRING)
                                    .build());
        PARAMS.add(ParameterMetadata.builder()
                                    .id(Constants.PARAM_RESOURCES_FOR_DEPLOYMENT)
                                    .type(ParameterType.STRING)
                                    .build());

        // Special blue green deploy parameters:
        PARAMS.add(ParameterMetadata.builder()
                                    .id(Constants.PARAM_NO_CONFIRM)
                                    .type(ParameterType.BOOLEAN)
                                    .defaultValue(false)
                                    .build());
    }

    private BlueGreenDeployMetadata() {
    }

    public static OperationMetadata getMetadata() {
        return OperationMetadata.builder()
                                .parameters(PARAMS)
                                .diagramId(Constants.BLUE_GREEN_DEPLOY_SERVICE_ID)
                                .versions(Constants.SERVICE_VERSION_1_1, Constants.SERVICE_VERSION_1_2)
                                .build();
    }
}
