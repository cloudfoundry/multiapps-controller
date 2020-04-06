package com.sap.cloud.lm.sl.cf.process.metadata;

import java.util.HashSet;
import java.util.Set;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.web.api.model.ImmutableOperationMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.ImmutableParameterMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.OperationMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.ParameterMetadata.ParameterType;
import com.sap.cloud.lm.sl.mta.model.VersionRule;

public class CtsDeployMetadata {

    private static final Set<ImmutableParameterMetadata> PARAMS = new HashSet<>();

    static {
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_APP_ARCHIVE_ID)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_EXT_DESCRIPTOR_FILE_ID)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_NO_START)
                                             .type(ParameterType.BOOLEAN)
                                             .defaultValue(false)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_START_TIMEOUT)
                                             .type(ParameterType.INTEGER)
                                             .defaultValue(Constants.DEFAULT_START_TIMEOUT)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_USE_NAMESPACES)
                                             .type(ParameterType.BOOLEAN)
                                             .defaultValue(false)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_USE_NAMESPACES_FOR_SERVICES)
                                             .type(ParameterType.BOOLEAN)
                                             .defaultValue(false)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_VERSION_RULE)
                                             .defaultValue(VersionRule.SAME_HIGHER.toString())
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_DELETE_SERVICES)
                                             .type(ParameterType.BOOLEAN)
                                             .defaultValue(false)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_DELETE_SERVICE_KEYS)
                                             .type(ParameterType.BOOLEAN)
                                             .defaultValue(false)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_DELETE_SERVICE_BROKERS)
                                             .type(ParameterType.BOOLEAN)
                                             .defaultValue(false)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_FAIL_ON_CRASHED)
                                             .type(ParameterType.BOOLEAN)
                                             .defaultValue(false)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_MTA_ID)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_KEEP_FILES)
                                             .type(ParameterType.BOOLEAN)
                                             .defaultValue(false)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_NO_RESTART_SUBSCRIBED_APPS)
                                             .type(ParameterType.BOOLEAN)
                                             .defaultValue(false)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_GIT_URI)
                                             .defaultValue("")
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_GIT_REF)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_GIT_REPO_PATH)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_GIT_SKIP_SSL)
                                             .type(ParameterType.BOOLEAN)
                                             .defaultValue(false)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_NO_FAIL_ON_MISSING_PERMISSIONS)
                                             .type(ParameterType.BOOLEAN)
                                             .defaultValue(false)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_ABORT_ON_ERROR)
                                             .type(ParameterType.BOOLEAN)
                                             .defaultValue(true)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_VERIFY_ARCHIVE_SIGNATURE)
                                             .type(ParameterType.BOOLEAN)
                                             .defaultValue(false)
                                             .build());

        // Special CTS+ parameters:
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_CTS_PROCESS_ID)
                                             .required(true)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_FILE_LIST)
                                             .type(ParameterType.TABLE)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_DEPLOY_URI)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_USERNAME)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_PASSWORD)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_APPLICATION_TYPE)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_TRANSFER_TYPE)
                                             .build());
        PARAMS.add(ImmutableParameterMetadata.builder()
                                             .id(Constants.PARAM_GIT_REPOSITORY_LIST)
                                             .type(ParameterType.TABLE)
                                             .build());
    }

    private CtsDeployMetadata() {
    }

    public static OperationMetadata getMetadata() {
        return ImmutableOperationMetadata.builder()
                                         .diagramId(Constants.CTS_DEPLOY_SERVICE_ID)
                                         .addVersions(Constants.SERVICE_VERSION_1_0)
                                         .parameters(PARAMS)
                                         .build();
    }

}