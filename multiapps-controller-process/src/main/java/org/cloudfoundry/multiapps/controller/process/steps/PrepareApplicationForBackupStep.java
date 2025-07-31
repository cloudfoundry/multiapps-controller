package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;

import jakarta.inject.Named;
import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataAnnotations;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataLabels;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.util.MtaMetadataUtil;
import org.cloudfoundry.multiapps.controller.core.model.BlueGreenApplicationNameSuffix;
import org.cloudfoundry.multiapps.controller.core.util.NameUtil;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("prepareApplicationForBackupStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PrepareApplicationForBackupStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) throws Exception {
        CloudApplication cloudApplication = context.getVariable(Variables.APP_TO_PROCESS);
        CloudControllerClient client = context.getControllerClient();
        String mtaNamespace = context.getVariable(Variables.MTA_NAMESPACE);
        String mtaUserNamespaceWithSystemNamespace = NameUtil.computeUserNamespaceWithSystemNamespace(Constants.MTA_BACKUP_NAMESPACE,
                                                                                                      mtaNamespace);

        String newApplicationName = BlueGreenApplicationNameSuffix.removeDoubleSuffixes(cloudApplication.getName());
        newApplicationName = NameUtil.computeValidApplicationName(newApplicationName, Constants.MTA_BACKUP_NAMESPACE, true, false);

        getStepLogger().info(Messages.RENAMING_APPLICATION_0_TO_1_TO_BE_USED_FOR_ROLLBACK, cloudApplication.getName(), newApplicationName);
        client.rename(cloudApplication.getName(), newApplicationName);
        String hashedMtaNamespace = MtaMetadataUtil.getHashedLabel(mtaUserNamespaceWithSystemNamespace);
        client.updateApplicationMetadata(cloudApplication.getGuid(), Metadata.builder()
                                                                             .from(cloudApplication.getV3Metadata())
                                                                             .label(MtaMetadataLabels.MTA_NAMESPACE, hashedMtaNamespace)
                                                                             .annotation(MtaMetadataAnnotations.MTA_NAMESPACE,
                                                                                         mtaUserNamespaceWithSystemNamespace)
                                                                             .build());

        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format(Messages.ERROR_WHILE_BACKUP_APPLICATION, context.getVariable(Variables.APP_TO_PROCESS)
                                                                                    .getName());
    }

}
