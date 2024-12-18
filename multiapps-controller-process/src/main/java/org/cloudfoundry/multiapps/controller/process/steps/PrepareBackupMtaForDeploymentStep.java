package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.core.cf.detect.DeployedMtaDetector;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataLabels;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.util.NameUtil;
import org.cloudfoundry.multiapps.controller.persistence.dto.BackupDescriptor;
import org.cloudfoundry.multiapps.controller.persistence.services.DescriptorBackupService;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ProcessConflictPreventer;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.NoResultException;

@Named("prepareBackupMtaForDeploymentStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PrepareBackupMtaForDeploymentStep extends SyncFlowableStep {

    private DescriptorBackupService descriptorBackupService;
    private DeployedMtaDetector deployedMtaDetector;
    private OperationService operationService;
    protected Function<OperationService, ProcessConflictPreventer> conflictPreventerSupplier = ProcessConflictPreventer::new;

    @Inject
    public PrepareBackupMtaForDeploymentStep(DescriptorBackupService descriptorBackupService, DeployedMtaDetector deployedMtaDetector,
                                             OperationService operationService) {
        this.descriptorBackupService = descriptorBackupService;
        this.deployedMtaDetector = deployedMtaDetector;
        this.operationService = operationService;
    }

    @Override
    protected StepPhase executeStep(ProcessContext context) throws Exception {
        getStepLogger().info(Messages.PREPARE_TO_ROLLBACK_MTA, context.getVariable(Variables.MTA_ID));

        CloudControllerClient client = context.getControllerClient();
        String spaceGuid = context.getVariable(Variables.SPACE_GUID);
        String mtaId = context.getVariable(Variables.MTA_ID);
        String mtaNamespace = context.getVariable(Variables.MTA_NAMESPACE);
        String mtaNamespaceWithSystemNamespace = NameUtil.computeUserNamespaceWithSystemNamespace(Constants.MTA_BACKUP_NAMESPACE,
                                                                                                  mtaNamespace);

        acquireOperationLock(context, mtaId);

        Optional<DeployedMta> backupMtaOptional = deployedMtaDetector.detectDeployedMtaByNameAndNamespace(mtaId,
                                                                                                          mtaNamespaceWithSystemNamespace,
                                                                                                          client);

        Optional<DeployedMta> deployedMtaOptional = deployedMtaDetector.detectDeployedMtaByNameAndNamespace(mtaId, mtaNamespace, client);
        if (backupMtaOptional.isEmpty() || deployedMtaOptional.isEmpty()) {
            throw new ContentException(Messages.ROLLBACK_OF_MTA_ID_0_CANNOT_BE_DONE_MISSING_DEPLOYED_MTA, mtaId);
        }
        DeployedMta backupMta = backupMtaOptional.get();

        BackupDescriptor backupDescriptor = getBackupDescriptor(backupMta, mtaId, spaceGuid, mtaNamespace);

        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, backupDescriptor.getDescriptor());
        context.setVariable(Variables.MTA_MAJOR_SCHEMA_VERSION, backupDescriptor.getDescriptor()
                                                                                .getMajorSchemaVersion());
        context.setVariable(Variables.MTA_ARCHIVE_MODULES, backupDescriptor.getDescriptor()
                                                                           .getModules()
                                                                           .stream()
                                                                           .map(Module::getName)
                                                                           .collect(Collectors.toSet()));
        context.setVariable(Variables.DEPLOYED_MTA, deployedMtaOptional.get());
        context.setVariable(Variables.BACKUP_MTA, backupMta);

        return StepPhase.DONE;
    }

    private void acquireOperationLock(ProcessContext context, String mtaId) {
        conflictPreventerSupplier.apply(operationService)
                                 .acquireLock(mtaId, null, context.getVariable(Variables.SPACE_GUID),
                                              context.getVariable(Variables.CORRELATION_ID));
    }

    private BackupDescriptor getBackupDescriptor(DeployedMta backupMta, String mtaId, String spaceGuid, String mtaNamespace) {
        String descriptorChecksumOfBackupMta = backupMta.getApplications()
                                                        .get(0)
                                                        .getV3Metadata()
                                                        .getLabels()
                                                        .get(MtaMetadataLabels.MTA_DESCRIPTOR_CHECKSUM);

        if (descriptorChecksumOfBackupMta == null) {
            throw new ContentException(Messages.DESCRIPTOR_CHECKSUM_NOT_SET_IN_APPLICATION_ROLLBACK_CANNOT_BE_DONE);
        }

        if (!doesAllDeployedAppsChecksumMatch(backupMta, descriptorChecksumOfBackupMta)) {
            throw new ContentException(Messages.ROLLBACK_OPERATION_CANNOT_BE_DONE_BACKUP_APPLICATIONS_HAVE_DIFFERENT_CHECKSUMS);
        }

        BackupDescriptor backupDescriptor = null;
        try {
            backupDescriptor = descriptorBackupService.createQuery()
                                                      .mtaId(mtaId)
                                                      .spaceId(spaceGuid)
                                                      .namespace(mtaNamespace)
                                                      .checksum(descriptorChecksumOfBackupMta)
                                                      .singleResult();
        } catch (NoResultException e) {
            throw new ContentException(Messages.ROLLBACK_MTA_ID_0_CANNOT_BE_DONE_MISSING_DESCRIPTOR, mtaId);
        }
        return backupDescriptor;
    }

    private boolean doesAllDeployedAppsChecksumMatch(DeployedMta backupMta, String descriptorChecksumOfBackupMta) {
        return backupMta.getApplications()
                        .stream()
                        .allMatch(application -> descriptorChecksumOfBackupMta.equals(application.getV3Metadata()
                                                                                                 .getLabels()
                                                                                                 .get(MtaMetadataLabels.MTA_DESCRIPTOR_CHECKSUM)));
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_DURING_PREPARATION_BACKUP_MTA;
    }

}
