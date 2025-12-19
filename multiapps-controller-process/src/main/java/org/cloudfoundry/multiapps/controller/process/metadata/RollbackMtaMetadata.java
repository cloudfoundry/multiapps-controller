package org.cloudfoundry.multiapps.controller.process.metadata;

import org.cloudfoundry.multiapps.controller.api.model.ImmutableOperationMetadata;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableParameterMetadata;
import org.cloudfoundry.multiapps.controller.api.model.OperationMetadata;
import org.cloudfoundry.multiapps.controller.api.model.ParameterType;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.metadata.parameters.TimeoutParameterConverter;
import org.cloudfoundry.multiapps.controller.process.metadata.parameters.VersionRuleParameterConverter;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.VersionRule;

public class RollbackMtaMetadata {

    private RollbackMtaMetadata() {
    }

    public static OperationMetadata getMetadata() {
        return ImmutableOperationMetadata.builder()
                                         .diagramId(Constants.ROLLBACK_MTA_SERVICE_ID)
                                         .addVersions(Constants.SERVICE_VERSION_1_1, Constants.SERVICE_VERSION_1_2)
                                         .addParameter(ImmutableParameterMetadata.builder()
                                                                                 .id(Variables.MTA_NAMESPACE.getName())
                                                                                 .type(ParameterType.STRING)
                                                                                 .build())
                                         .addParameter(ImmutableParameterMetadata.builder()
                                                                                 .id(Variables.VERSION_RULE.getName())
                                                                                 .type(ParameterType.STRING)
                                                                                 .customConverter(new VersionRuleParameterConverter())
                                                                                 .defaultValue(VersionRule.ALL)
                                                                                 .build())
                                         .addParameter(ImmutableParameterMetadata.builder()
                                                                                 .id(Variables.MTA_ID.getName())
                                                                                 .type(ParameterType.STRING)
                                                                                 .required(true)
                                                                                 .build())
                                         .addParameter(ImmutableParameterMetadata.builder()
                                                                                 .id(Variables.DELETE_SERVICES.getName())
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
                                                                                 .id(Variables.APPS_START_TIMEOUT_PROCESS_VARIABLE.getName())
                                                                                 .type(ParameterType.INTEGER)
                                                                                 .customConverter(new TimeoutParameterConverter(
                                                                                     Variables.APPS_START_TIMEOUT_PROCESS_VARIABLE))
                                                                                 .build())
                                         .addParameter(ImmutableParameterMetadata.builder()
                                                                                 .id(Variables.APPS_STAGE_TIMEOUT_PROCESS_VARIABLE.getName())
                                                                                 .type(ParameterType.INTEGER)
                                                                                 .customConverter(new TimeoutParameterConverter(
                                                                                     Variables.APPS_STAGE_TIMEOUT_PROCESS_VARIABLE))
                                                                                 .build())
                                         .addParameter(ImmutableParameterMetadata.builder()
                                                                                 .id(Variables.APPS_UPLOAD_TIMEOUT_PROCESS_VARIABLE.getName())
                                                                                 .type(ParameterType.INTEGER)
                                                                                 .customConverter(new TimeoutParameterConverter(
                                                                                     Variables.APPS_UPLOAD_TIMEOUT_PROCESS_VARIABLE))
                                                                                 .build())
                                         .addParameter(ImmutableParameterMetadata.builder()
                                                                                 .id(Variables.APPS_TASK_EXECUTION_TIMEOUT_PROCESS_VARIABLE.getName())
                                                                                 .type(ParameterType.INTEGER)
                                                                                 .customConverter(new TimeoutParameterConverter(
                                                                                     Variables.APPS_TASK_EXECUTION_TIMEOUT_PROCESS_VARIABLE))
                                                                                 .build())
                                         .addParameter(ImmutableParameterMetadata.builder()
                                                                                 .id(Variables.PROCESS_USER_PROVIDED_SERVICES.getName())
                                                                                 .type(ParameterType.BOOLEAN)
                                                                                 .build())
                                         .addParameter(ImmutableParameterMetadata.builder()
                                                                                 .id(Variables.IS_SECURITY_ENABLED.getName())
                                                                                 .type(ParameterType.BOOLEAN)
                                                                                 .defaultValue(false)
                                                                                 .build())
                                         .build();
    }

}
