package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;

import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.core.cf.CloudHandlerFactory;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaDescriptorMerger;
import org.cloudfoundry.multiapps.controller.persistence.dto.BackupDescriptor;
import org.cloudfoundry.multiapps.controller.persistence.dto.ImmutableBackupDescriptor;
import org.cloudfoundry.multiapps.controller.persistence.services.DescriptorBackupService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.ExtensionDescriptor;
import org.cloudfoundry.multiapps.mta.model.Platform;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.util.DigestUtils;

import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named("mergeDescriptorsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class MergeDescriptorsStep extends SyncFlowableStep {

    @Inject
    private DescriptorBackupService descriptorBackupService;

    protected MtaDescriptorMerger getMtaDescriptorMerger(CloudHandlerFactory factory, Platform platform) {
        return new MtaDescriptorMerger(factory, platform, getStepLogger());
    }

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.MERGING_DESCRIPTORS);
        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR);
        List<ExtensionDescriptor> extensionDescriptors = context.getVariable(Variables.MTA_EXTENSION_DESCRIPTOR_CHAIN);
        CloudHandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context.getExecution());
        Platform platform = configuration.getPlatform();
        DeploymentDescriptor descriptor = getMtaDescriptorMerger(handlerFactory, platform).merge(deploymentDescriptor,
                                                                                                 extensionDescriptors);
        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, descriptor);
        context.setVariable(Variables.CHECKSUM_OF_MERGED_DESCRIPTOR, DigestUtils.md5DigestAsHex(JsonUtil.toJson(descriptor)
                                                                                                        .getBytes()));
        backupDeploymentDescriptor(context, descriptor);
        getStepLogger().debug(Messages.DESCRIPTORS_MERGED);
        return StepPhase.DONE;
    }

    private void backupDeploymentDescriptor(ProcessContext context, DeploymentDescriptor descriptor) {
        if (!context.getVariable(Variables.SHOULD_BACKUP_EXISTING_APPS)) {
            return;
        }
        String currentDeploymentDescriptorsChecksum = context.getVariable(Variables.CHECKSUM_OF_MERGED_DESCRIPTOR);
        String spaceGuid = context.getVariable(Variables.SPACE_GUID);
        String mtaId = descriptor.getId();
        String mtaNamesapce = context.getVariable(Variables.MTA_NAMESPACE);
        List<BackupDescriptor> backupDescriptors = descriptorBackupService.createQuery()
                                                                          .mtaId(mtaId)
                                                                          .spaceId(spaceGuid)
                                                                          .namespace(mtaNamesapce)
                                                                          .checksum(currentDeploymentDescriptorsChecksum)
                                                                          .list();
        if (backupDescriptors.isEmpty()) {
            descriptorBackupService.add(ImmutableBackupDescriptor.builder()
                                                                 .descriptor(descriptor)
                                                                 .mtaId(mtaId)
                                                                 .mtaVersion(descriptor.getVersion())
                                                                 .spaceId(spaceGuid)
                                                                 .namespace(mtaNamesapce)
                                                                 .checksum(currentDeploymentDescriptorsChecksum)
                                                                 .build());
        }
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_MERGING_DESCRIPTORS;
    }

}
