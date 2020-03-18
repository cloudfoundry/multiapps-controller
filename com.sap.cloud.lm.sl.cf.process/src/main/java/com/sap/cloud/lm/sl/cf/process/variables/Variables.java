package com.sap.cloud.lm.sl.cf.process.variables;

import org.cloudfoundry.client.lib.domain.UploadToken;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaArchiveElements;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Hook;

public interface Variables {

    Variable<DeploymentDescriptor> DEPLOYMENT_DESCRIPTOR = ImmutableVariable.<DeploymentDescriptor> builder()
                                                                            .name(Constants.VAR_MTA_DEPLOYMENT_DESCRIPTOR)
                                                                            .type(Variable.typeReference(DeploymentDescriptor.class))
                                                                            .serializationStrategy(SerializationStrategy.JSON_STRING)
                                                                            .build();
    Variable<DeploymentDescriptor> DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS = ImmutableVariable.<DeploymentDescriptor> builder()
                                                                                                   .name(Constants.VAR_MTA_DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS)
                                                                                                   .type(Variable.typeReference(DeploymentDescriptor.class))
                                                                                                   .serializationStrategy(SerializationStrategy.JSON_STRING)
                                                                                                   .build();
    Variable<DeploymentDescriptor> COMPLETE_DEPLOYMENT_DESCRIPTOR = ImmutableVariable.<DeploymentDescriptor> builder()
                                                                                     .name(Constants.VAR_COMPLETE_MTA_DEPLOYMENT_DESCRIPTOR)
                                                                                     .type(Variable.typeReference(DeploymentDescriptor.class))
                                                                                     .serializationStrategy(SerializationStrategy.JSON_STRING)
                                                                                     .build();
    Variable<CloudApplicationExtended> APP_TO_PROCESS = ImmutableVariable.<CloudApplicationExtended> builder()
                                                                         .name(Constants.VAR_APP_TO_PROCESS)
                                                                         .type(Variable.typeReference(CloudApplicationExtended.class))
                                                                         .serializationStrategy(SerializationStrategy.JSON_STRING)
                                                                         .build();
    Variable<MtaArchiveElements> MTA_ARCHIVE_ELEMENTS = ImmutableVariable.<MtaArchiveElements> builder()
                                                                         .name(Constants.VAR_MTA_ARCHIVE_ELEMENTS)
                                                                         .type(Variable.typeReference(MtaArchiveElements.class))
                                                                         .defaultValue(new MtaArchiveElements())
                                                                         .serializationStrategy(SerializationStrategy.JSON_STRING)
                                                                         .build();
    Variable<CloudServiceExtended> SERVICE_TO_PROCESS = ImmutableVariable.<CloudServiceExtended> builder()
                                                                         .name(Constants.VAR_SERVICE_TO_PROCESS)
                                                                         .type(Variable.typeReference(CloudServiceExtended.class))
                                                                         .serializationStrategy(SerializationStrategy.JSON_STRING)
                                                                         .build();
    Variable<UploadToken> UPLOAD_TOKEN = ImmutableVariable.<UploadToken> builder()
                                                          .name(Constants.VAR_UPLOAD_TOKEN)
                                                          .type(Variable.typeReference(UploadToken.class))
                                                          .serializationStrategy(SerializationStrategy.JSON_STRING)
                                                          .build();
    Variable<Hook> HOOK_FOR_EXECUTION = ImmutableVariable.<Hook> builder()
                                                         .name(Constants.VAR_HOOK_FOR_EXECUTION)
                                                         .type(Variable.typeReference(Hook.class))
                                                         .serializationStrategy(SerializationStrategy.JSON_STRING)
                                                         .build();

}
