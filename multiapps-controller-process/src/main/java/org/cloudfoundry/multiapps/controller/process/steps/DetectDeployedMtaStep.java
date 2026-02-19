package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.CloudCredentials;
import org.cloudfoundry.multiapps.controller.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import org.cloudfoundry.multiapps.controller.core.cf.clients.CustomServiceKeysClient;
import org.cloudfoundry.multiapps.controller.core.cf.clients.WebClientFactory;
import org.cloudfoundry.multiapps.controller.core.cf.detect.DeployedMtaDetector;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadata;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaService;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaServiceKey;
import org.cloudfoundry.multiapps.controller.core.security.serialization.DynamicSecureSerialization;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.core.util.NameUtil;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.security.util.SecureLoggingUtil;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("detectDeployedMtaStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetectDeployedMtaStep extends SyncFlowableStep {

    @Inject
    @Qualifier("deployedMtaDetector")
    private DeployedMtaDetector deployedMtaDetector;
    @Inject
    private TokenService tokenService;
    @Inject
    private WebClientFactory webClientFactory;

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.DETECTING_DEPLOYED_MTA);
        DynamicSecureSerialization dynamicSecureSerialization = SecureLoggingUtil.getDynamicSecureSerialization(context);

        String mtaId = context.getVariable(Variables.MTA_ID);
        String mtaNamespace = context.getVariable(Variables.MTA_NAMESPACE);
        CloudControllerClient client = context.getControllerClient();

        DeployedMta deployedMta = detectDeployedMta(mtaId, mtaNamespace, client, context, dynamicSecureSerialization);

        detectBackupMta(mtaId, mtaNamespace, client, context, dynamicSecureSerialization);

        List<DeployedMtaServiceKey> deployedServiceKeys = detectDeployedServiceKeys(mtaId, mtaNamespace, deployedMta, context);
        context.setVariable(Variables.DEPLOYED_MTA_SERVICE_KEYS, deployedServiceKeys);
        getStepLogger().debug(Messages.DEPLOYED_MTA_SERVICE_KEYS, dynamicSecureSerialization.toJson(deployedServiceKeys));

        return StepPhase.DONE;
    }

    private DeployedMta detectDeployedMta(String mtaId, String mtaNamespace, CloudControllerClient client, ProcessContext context,
                                          DynamicSecureSerialization dynamicSecureSerialization) {
        getStepLogger().debug(Messages.DETECTING_MTA_BY_ID_AND_NAMESPACE, mtaId, mtaNamespace);
        Optional<DeployedMta> optionalDeployedMta = deployedMtaDetector.detectDeployedMtaByNameAndNamespace(mtaId, mtaNamespace, client);

        if (optionalDeployedMta.isEmpty()) {
            logNoMtaDeployedDetected(mtaId, mtaNamespace);
            context.setVariable(Variables.DEPLOYED_MTA, null);
            return null;
        }

        DeployedMta deployedMta = optionalDeployedMta.get();
        context.setVariable(Variables.DEPLOYED_MTA, deployedMta);
        getStepLogger().debug(Messages.DEPLOYED_MTA, dynamicSecureSerialization.toJson(deployedMta));
        MtaMetadata metadata = deployedMta.getMetadata();
        logDetectedDeployedMta(mtaNamespace, metadata);
        return deployedMta;
    }

    private void detectBackupMta(String mtaId, String mtaNamespace, CloudControllerClient client, ProcessContext context,
                                 DynamicSecureSerialization dynamicSecureSerialization) {
        getStepLogger().debug(Messages.DETECTING_BACKUP_MTA_BY_ID_AND_NAMESPACE, mtaId, mtaNamespace);
        Optional<DeployedMta> optionalBackupMta = deployedMtaDetector.detectDeployedMtaByNameAndNamespace(mtaId,
                                                                                                          NameUtil.computeUserNamespaceWithSystemNamespace(
                                                                                                              Constants.MTA_BACKUP_NAMESPACE,
                                                                                                              mtaNamespace), client);

        if (optionalBackupMta.isEmpty()) {
            context.setVariable(Variables.BACKUP_MTA, null);
            return;
        }

        DeployedMta backupMta = optionalBackupMta.get();
        context.setVariable(Variables.BACKUP_MTA, backupMta);
        getStepLogger().debug(Messages.DETECTED_BACKUP_MTA, dynamicSecureSerialization.toJson(backupMta));
    }

    private List<DeployedMtaServiceKey> detectDeployedServiceKeys(String mtaId, String mtaNamespace, DeployedMta deployedMta,
                                                                  ProcessContext context) {
        List<DeployedMtaService> deployedManagedMtaServices = Optional.ofNullable(deployedMta)
                                                                      .map(DeployedMta::getServices)
                                                                      .orElse(List.of());
        String spaceGuid = context.getVariable(Variables.SPACE_GUID);
        String userGuid = context.getVariable(Variables.USER_GUID);
        OAuth2AccessTokenWithAdditionalInfo token = tokenService.getToken(userGuid);
        CloudCredentials credentials = new CloudCredentials(token, true);

        CustomServiceKeysClient serviceKeysClient = getCustomServiceKeysClient(credentials, context.getVariable(Variables.CORRELATION_ID));

        return serviceKeysClient.getServiceKeysByMetadataAndManagedServices(
            spaceGuid, mtaId, mtaNamespace, deployedManagedMtaServices
        );
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
            getStepLogger().info(
                MessageFormat.format(Messages.DETECTED_DEPLOYED_MTA_WITH_NAMESPACE, metadata.getId(), metadata.getVersion(),
                                     metadata.getNamespace()));
            return;
        }
        getStepLogger().info(MessageFormat.format(Messages.DETECTED_DEPLOYED_MTA, metadata.getId(), metadata.getVersion()));
    }

    protected CustomServiceKeysClient getCustomServiceKeysClient(CloudCredentials credentials, String correlationId) {
        return new CustomServiceKeysClient(configuration, webClientFactory, credentials, correlationId);
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_DETECTING_DEPLOYED_MTA;
    }
}
