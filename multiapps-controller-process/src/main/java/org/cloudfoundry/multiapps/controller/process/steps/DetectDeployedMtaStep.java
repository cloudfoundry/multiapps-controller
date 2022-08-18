package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.multiapps.controller.core.cf.clients.CustomServiceKeysClient;
import org.cloudfoundry.multiapps.controller.core.cf.detect.DeployedMtaDetector;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadata;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaService;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaServiceKey;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;

@Named("detectDeployedMtaStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetectDeployedMtaStep extends SyncFlowableStep {

    @Inject
    @Qualifier("deployedMtaDetector")
    private DeployedMtaDetector deployedMtaDetector;

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.DETECTING_DEPLOYED_MTA);

        String mtaId = context.getVariable(Variables.MTA_ID);
        String mtaNamespace = context.getVariable(Variables.MTA_NAMESPACE);
        CloudControllerClient client = context.getControllerClient();

        DeployedMta deployedMta = detectDeployedMta(mtaId, mtaNamespace, client);
        context.setVariable(Variables.DEPLOYED_MTA, deployedMta);

        var deployedServiceKeys = detectDeployedServiceKeys(mtaId, mtaNamespace, deployedMta, client, context);
        context.setVariable(Variables.DEPLOYED_MTA_SERVICE_KEYS, deployedServiceKeys);
        getStepLogger().debug(Messages.DEPLOYED_MTA_SERVICE_KEYS, SecureSerialization.toJson(deployedServiceKeys));

        return StepPhase.DONE;
    }

    private DeployedMta detectDeployedMta(String mtaId, String mtaNamespace, CloudControllerClient client) {
        getStepLogger().debug(Messages.DETECTING_MTA_BY_ID_AND_NAMESPACE, mtaId, mtaNamespace);
        Optional<DeployedMta> optionalDeployedMta = deployedMtaDetector.detectDeployedMtaByNameAndNamespace(mtaId, mtaNamespace, client);

        if (optionalDeployedMta.isEmpty()) {
            logNoMtaDeployedDetected(mtaId, mtaNamespace);
            return null;
        }

        DeployedMta deployedMta = optionalDeployedMta.get();
        getStepLogger().debug(Messages.DEPLOYED_MTA, SecureSerialization.toJson(deployedMta));
        MtaMetadata metadata = deployedMta.getMetadata();
        logDetectedDeployedMta(mtaNamespace, metadata);
        return deployedMta;
    }

    private List<DeployedMtaServiceKey> detectDeployedServiceKeys(String mtaId, String mtaNamespace, DeployedMta deployedMta,
                                                                  CloudControllerClient client, ProcessContext context) {
        List<DeployedMtaService> deployedMtaServices = deployedMta == null ? null : deployedMta.getServices();
        String spaceGuid = context.getVariable(Variables.SPACE_GUID);

        CustomServiceKeysClient serviceKeysClient = getCustomServiceKeysClient(client);
        return serviceKeysClient.getServiceKeysByMetadataAndGuids(spaceGuid, mtaId, mtaNamespace, deployedMtaServices);
    }

    private void logNoMtaDeployedDetected(String mtaId, String mtaNamespace) {
        if (StringUtils.isNotEmpty(mtaNamespace)) {
            getStepLogger().info(MessageFormat.format(Messages.NO_DEPLOYED_MTA_DETECTED_WITH_NAMESPACE, mtaId, mtaNamespace));
            return;
        }
        getStepLogger().info(MessageFormat.format(Messages.NO_DEPLOYED_MTA_DETECTED, mtaId));
    }

    private void logDetectedDeployedMta(String mtaNamespace, MtaMetadata metadata) {
        if (StringUtils.isNotEmpty(mtaNamespace)) {
            getStepLogger().info(MessageFormat.format(Messages.DETECTED_DEPLOYED_MTA_WITH_NAMESPACE, metadata.getId(),
                                                      metadata.getVersion(), metadata.getNamespace()));
            return;
        }
        getStepLogger().info(MessageFormat.format(Messages.DETECTED_DEPLOYED_MTA, metadata.getId(), metadata.getVersion()));
    }

    protected CustomServiceKeysClient getCustomServiceKeysClient(CloudControllerClient client) {
        return new CustomServiceKeysClient(client);
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_DETECTING_DEPLOYED_MTA;
    }
}
