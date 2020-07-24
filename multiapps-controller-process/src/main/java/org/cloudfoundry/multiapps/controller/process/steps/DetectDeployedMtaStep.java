package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.core.cf.detect.DeployedMtaDetector;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadata;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("detectDeployedMtaStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetectDeployedMtaStep extends SyncFlowableStep {

    @Inject
    private DeployedMtaDetector deployedMtaDetector;

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.DETECTING_DEPLOYED_MTA);

        CloudControllerClient client = context.getControllerClient();

        String mtaId = context.getVariable(Variables.MTA_ID);

        String mtaNamespace = context.getVariable(Variables.MTA_NAMESPACE);
        boolean envDetectionEnabled = context.getVariable(Variables.ENABLE_ENV_DETECTION);

        getStepLogger().debug("Detecting MTA by id {0} and namespace {1}", mtaId, mtaNamespace);

        Optional<DeployedMta> optionalDeployedMta = deployedMtaDetector.detectDeployedMtaByNameAndNamespace(mtaId, mtaNamespace, client,
                                                                                                            envDetectionEnabled);
        if (optionalDeployedMta.isPresent()) {
            DeployedMta deployedMta = optionalDeployedMta.get();
            context.setVariable(Variables.DEPLOYED_MTA, deployedMta);
            getStepLogger().debug(Messages.DEPLOYED_MTA, SecureSerialization.toJson(deployedMta));

            MtaMetadata metadata = deployedMta.getMetadata();
            getStepLogger().info(MessageFormat.format(Messages.DEPLOYED_MTA_DETECTED_WITH_VERSION, metadata.getNamespace(),
                                                      metadata.getId(), metadata.getVersion()));
        } else {
            getStepLogger().info(Messages.NO_DEPLOYED_MTA_DETECTED);
            context.setVariable(Variables.DEPLOYED_MTA, null);
        }
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_DETECTING_DEPLOYED_MTA;
    }
}
