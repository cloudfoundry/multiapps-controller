package org.cloudfoundry.multiapps.controller.process.metadata;

import org.cloudfoundry.multiapps.controller.api.model.ImmutableOperationMetadata;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableParameterMetadata;
import org.cloudfoundry.multiapps.controller.api.model.OperationMetadata;
import org.cloudfoundry.multiapps.controller.api.model.ParameterType;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

public class UndeployMetadata {

    private UndeployMetadata() {
    }

    public static OperationMetadata getMetadata() {
        return ImmutableOperationMetadata.builder()
                                         .diagramId(Constants.UNDEPLOY_SERVICE_ID)
                                         .addVersions(Constants.SERVICE_VERSION_1_0)
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
                                                                                 .id(Variables.MTA_ID.getName())
                                                                                 .required(true)
                                                                                 .type(ParameterType.STRING)
                                                                                 .build())
                                         .addParameter(ImmutableParameterMetadata.builder()
                                                                                 .id(Variables.MTA_NAMESPACE.getName())
                                                                                 .type(ParameterType.STRING)
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
                                         .build();
    }
}
