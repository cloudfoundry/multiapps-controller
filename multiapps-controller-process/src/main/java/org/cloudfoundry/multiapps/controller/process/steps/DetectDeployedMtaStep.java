package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.cf.clients.v3.CustomServiceKeysClientV3;
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

        DeployedMta deployedMta = detectDeployedMta(mtaId, mtaNamespace, client, context);

        detectDeployedServiceKeys(mtaId, mtaNamespace, deployedMta, client, context);

        return StepPhase.DONE;
    }

    private DeployedMta detectDeployedMta(String mtaId, String mtaNamespace, CloudControllerClient client, ProcessContext context) {

        getStepLogger().debug(Messages.DETECTING_MTA_BY_ID_AND_NAMESPACE, mtaId, mtaNamespace);
        Optional<DeployedMta> optionalDeployedMta = deployedMtaDetector.detectDeployedMtaByNameAndNamespace(mtaId, mtaNamespace, client);

        if (optionalDeployedMta.isEmpty()) {
            getStepLogger().info(Messages.NO_DEPLOYED_MTA_DETECTED);

            context.setVariable(Variables.DEPLOYED_MTA, null);
            return null;
        }

        DeployedMta deployedMta = optionalDeployedMta.get();
        context.setVariable(Variables.DEPLOYED_MTA, deployedMta);
        getStepLogger().debug(Messages.DEPLOYED_MTA, SecureSerialization.toJson(deployedMta));

        MtaMetadata metadata = deployedMta.getMetadata();
        getStepLogger().info(MessageFormat.format(Messages.DEPLOYED_MTA_DETECTED_WITH_VERSION, metadata.getNamespace(), metadata.getId(),
                                                  metadata.getVersion()));
        return deployedMta;
    }

    private List<DeployedMtaServiceKey> detectDeployedServiceKeys(String mtaId, String mtaNamespace, DeployedMta deployedMta,
                                                                  CloudControllerClient client, ProcessContext context) {
        List<DeployedMtaService> deployedMtaServices = deployedMta == null ? null : deployedMta.getServices();
        String spaceGuid = context.getVariable(Variables.SPACE_GUID);

        CustomServiceKeysClientV3 customV3Client = getCustomServiceKeysClient(client);
        List<DeployedMtaServiceKey> deployedServiceKeys = customV3Client.getServiceKeysByMetadataAndGuids(spaceGuid, mtaId, mtaNamespace,
                                                                                                          deployedMtaServices);
        context.setVariable(Variables.DEPLOYED_MTA_SERVICE_KEYS, deployedServiceKeys);
        getStepLogger().debug(Messages.DEPLOYED_MTA_SERVICE_KEYS, SecureSerialization.toJson(deployedServiceKeys));

        return deployedServiceKeys;
    }
    
    protected CustomServiceKeysClientV3 getCustomServiceKeysClient(CloudControllerClient client) {
        return new CustomServiceKeysClientV3(client);
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_DETECTING_DEPLOYED_MTA;
    }
}
