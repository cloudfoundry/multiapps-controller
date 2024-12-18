package org.cloudfoundry.multiapps.controller.process.steps;

import jakarta.inject.Named;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataAnnotations;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataLabels;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.util.MtaMetadataUtil;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;

@Named("removeMtaPreservedMetadataStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RemoveMtaPreservedMetadataStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) throws Exception {
        getStepLogger().debug(Messages.DELETING_METADATA_OF_PRESERVED_MTA_APPLICATIONS);
        DeployedMta preservedMta = context.getVariable(Variables.PRESERVED_MTA);
        CloudControllerClient client = context.getControllerClient();

        String mtaNamespace = context.getVariable(Variables.MTA_NAMESPACE);
        String hashedMtaNamespace = mtaNamespace != null ? MtaMetadataUtil.getHashedLabel(mtaNamespace) : null;

        for (DeployedMtaApplication application : preservedMta.getApplications()) {
            getStepLogger().debug(Messages.REMOVE_MTA_PRESERVED_METADATA_FOR_APPLICATION_0, application.getName());
            client.updateApplicationMetadata(application.getGuid(), Metadata.builder()
                                                                            .from(application.getV3Metadata())
                                                                            .label(MtaMetadataLabels.MTA_NAMESPACE, hashedMtaNamespace)
                                                                            .annotation(MtaMetadataAnnotations.MTA_NAMESPACE, mtaNamespace)
                                                                            .build());
        }

        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_DURING_REMOVAL_MTA_PRESREVED_METADATA;
    }

}
