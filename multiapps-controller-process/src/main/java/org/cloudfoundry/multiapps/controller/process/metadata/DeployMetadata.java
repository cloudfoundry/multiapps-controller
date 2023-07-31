package org.cloudfoundry.multiapps.controller.process.metadata;

import org.cloudfoundry.multiapps.controller.api.model.ImmutableOperationMetadata;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableParameterMetadata;
import org.cloudfoundry.multiapps.controller.api.model.OperationMetadata;
import org.cloudfoundry.multiapps.controller.api.model.ParameterType;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.metadata.parameters.StartTimeoutParameterConverter;
import org.cloudfoundry.multiapps.controller.process.metadata.parameters.VersionRuleParameterConverter;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

public class DeployMetadata {

    private DeployMetadata() {
    }

    public static OperationMetadata getMetadata() {
        return ImmutableOperationMetadata.builder()
                                         .diagramId(Constants.DEPLOY_SERVICE_ID)
                                         .addVersions(Constants.SERVICE_VERSION_1_1, Constants.SERVICE_VERSION_1_2)
                                         .addParameter(ImmutableParameterMetadata.builder()
                                                                                 .id(Variables.APP_ARCHIVE_ID.getName())
                                                                                 .type(ParameterType.STRING)
                                                                                 .build())
                                         .addParameter(ImmutableParameterMetadata.builder()
                                                                                 .id(Variables.EXT_DESCRIPTOR_FILE_ID.getName())
                                                                                 .type(ParameterType.STRING)
                                                                                 .build())
                                         .addParameter(ImmutableParameterMetadata.builder()
                                                                                 .id(Variables.NO_START.getName())
                                                                                 .type(ParameterType.BOOLEAN)
                                                                                 .build())
                                         .addParameter(ImmutableParameterMetadata.builder()
                                                                                 .id(Variables.START_TIMEOUT.getName())
                                                                                 .type(ParameterType.INTEGER)
                                                                                 .customConverter(new StartTimeoutParameterConverter())
                                                                                 .build())
                                         .addParameter(ImmutableParameterMetadata.builder()
                                                                                 .id(Variables.MTA_NAMESPACE.getName())
                                                                                 .type(ParameterType.STRING)
                                                                                 .build())
                                         .addParameter(ImmutableParameterMetadata.builder()
                                                                                 .id(Variables.APPLY_NAMESPACE.getName())
                                                                                 .type(ParameterType.BOOLEAN)
                                                                                 .build())
                                         .addParameter(ImmutableParameterMetadata.builder()
                                                                                 .id(Variables.VERSION_RULE.getName())
                                                                                 .type(ParameterType.STRING)
                                                                                 .customConverter(new VersionRuleParameterConverter())
                                                                                 .build())
                                         .addParameter(ImmutableParameterMetadata.builder()
                                                                                 .id(Variables.DELETE_SERVICES.getName())
                                                                                 .type(ParameterType.BOOLEAN)
                                                                                 .build())
                                         .addParameter(ImmutableParameterMetadata.builder()
                                                                                 .id(Variables.DELETE_SERVICE_KEYS.getName())
                                                                                 .type(ParameterType.BOOLEAN)
                                                                                 .build())
                                         .addParameter(ImmutableParameterMetadata.builder()
                                                                                 .id(Variables.DELETE_SERVICE_BROKERS.getName())
                                                                                 .type(ParameterType.BOOLEAN)
                                                                                 .build())
                                         .addParameter(ImmutableParameterMetadata.builder()
                                                                                 .id(Variables.FAIL_ON_CRASHED.getName())
                                                                                 .type(ParameterType.BOOLEAN)
                                                                                 .build())
                                         .addParameter(ImmutableParameterMetadata.builder()
                                                                                 .id(Variables.MTA_ID.getName())
                                                                                 .type(ParameterType.STRING)
                                                                                 .build())
                                         .addParameter(ImmutableParameterMetadata.builder()
                                                                                 .id(Variables.KEEP_FILES.getName())
                                                                                 .type(ParameterType.BOOLEAN)
                                                                                 .build())
                                         .addParameter(ImmutableParameterMetadata.builder()
                                                                                 .id(Variables.NO_RESTART_SUBSCRIBED_APPS.getName())
                                                                                 .type(ParameterType.BOOLEAN)
                                                                                 .build())
                                         .addParameter(ImmutableParameterMetadata.builder()
                                                                                 .id(Variables.NO_FAIL_ON_MISSING_PERMISSIONS.getName())
                                                                                 .type(ParameterType.BOOLEAN)
                                                                                 .build())
                                         .addParameter(ImmutableParameterMetadata.builder()
                                                                                 .id(Variables.ABORT_ON_ERROR.getName())
                                                                                 .type(ParameterType.BOOLEAN)
                                                                                 .build())
                                         .addParameter(ImmutableParameterMetadata.builder()
                                                                                 .id(Variables.MODULES_FOR_DEPLOYMENT.getName())
                                                                                 .type(ParameterType.STRING)
                                                                                 .build())
                                         .addParameter(ImmutableParameterMetadata.builder()
                                                                                 .id(Variables.RESOURCES_FOR_DEPLOYMENT.getName())
                                                                                 .type(ParameterType.STRING)
                                                                                 .build())
                                         .build();
    }

}
