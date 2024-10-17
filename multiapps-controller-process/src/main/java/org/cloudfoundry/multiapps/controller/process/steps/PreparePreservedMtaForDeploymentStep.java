package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.NoResultException;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.core.cf.detect.DeployedMtaDetector;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataLabels;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.util.NameUtil;
import org.cloudfoundry.multiapps.controller.persistence.dto.PreservedDescriptor;
import org.cloudfoundry.multiapps.controller.persistence.services.DescriptorPreserverService;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ProcessConflictPreventer;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;

@Named("preparePreservedMtaForDeploymentStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PreparePreservedMtaForDeploymentStep extends SyncFlowableStep {

    private DescriptorPreserverService descriptorPreserverService;
    private DeployedMtaDetector deployedMtaDetector;
    private OperationService operationService;
    protected Function<OperationService, ProcessConflictPreventer> conflictPreventerSupplier = ProcessConflictPreventer::new;

    @Inject
    public PreparePreservedMtaForDeploymentStep(DescriptorPreserverService descriptorPreserverService,
                                                DeployedMtaDetector deployedMtaDetector, OperationService operationService) {
        this.descriptorPreserverService = descriptorPreserverService;
        this.deployedMtaDetector = deployedMtaDetector;
        this.operationService = operationService;
    }

    @Override
    protected StepPhase executeStep(ProcessContext context) throws Exception {
        getStepLogger().info(Messages.PREPARE_TO_REVERT_MTA, context.getVariable(Variables.MTA_ID));

        CloudControllerClient client = context.getControllerClient();
        String spaceGuid = context.getVariable(Variables.SPACE_GUID);
        String mtaId = context.getVariable(Variables.MTA_ID);
        String mtaNamespace = context.getVariable(Variables.MTA_NAMESPACE);
        String mtaNamespaceWithSystemNamespace = NameUtil.computeUserNamespaceWithSystemNamespace(Constants.MTA_PRESERVED_NAMESPACE,
                                                                                                  mtaNamespace);

        acquireOperationLock(context, mtaId);

        Optional<DeployedMta> preservedMtaOptional = deployedMtaDetector.detectDeployedMtaByNameAndNamespace(mtaId,
                                                                                                             mtaNamespaceWithSystemNamespace,
                                                                                                             client);

        Optional<DeployedMta> deployedMtaOptional = deployedMtaDetector.detectDeployedMtaByNameAndNamespace(mtaId, mtaNamespace, client);
        if (preservedMtaOptional.isEmpty() || deployedMtaOptional.isEmpty()) {
            throw new ContentException(Messages.REVERT_OF_MTA_ID_0_CANNOT_BE_DONE_MISSING_DEPLOYED_MTA, mtaId);
        }
        DeployedMta preservedMta = preservedMtaOptional.get();

        PreservedDescriptor preservedDescriptor = getPreservedDescriptor(preservedMta, mtaId, spaceGuid, mtaNamespace);

        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, preservedDescriptor.getDescriptor());
        context.setVariable(Variables.MTA_MAJOR_SCHEMA_VERSION, preservedDescriptor.getDescriptor()
                                                                                   .getMajorSchemaVersion());
        context.setVariable(Variables.MTA_ARCHIVE_MODULES, preservedDescriptor.getDescriptor()
                                                                              .getModules()
                                                                              .stream()
                                                                              .map(Module::getName)
                                                                              .collect(Collectors.toSet()));
        context.setVariable(Variables.DEPLOYED_MTA, deployedMtaOptional.get());
        context.setVariable(Variables.PRESERVED_MTA, preservedMta);

        return StepPhase.DONE;
    }

    private void acquireOperationLock(ProcessContext context, String mtaId) {
        conflictPreventerSupplier.apply(operationService)
                                 .acquireLock(mtaId, null, context.getVariable(Variables.SPACE_GUID),
                                              context.getVariable(Variables.CORRELATION_ID));
    }

    private PreservedDescriptor getPreservedDescriptor(DeployedMta preservedMta, String mtaId, String spaceGuid, String mtaNamespace) {
        String descriptorChecksumOfPreservedMta = preservedMta.getApplications()
                                                              .get(0)
                                                              .getV3Metadata()
                                                              .getLabels()
                                                              .get(MtaMetadataLabels.MTA_DESCRIPTOR_CHECKSUM);

        if (descriptorChecksumOfPreservedMta == null) {
            throw new ContentException(Messages.DESCRIPTOR_CHECKSUM_NOT_SET_IN_APPLICATION_ROLLBACK_CANNOT_BE_DONE);
        }

        if (!doesAllDeployedAppsChecksumMatch(preservedMta, descriptorChecksumOfPreservedMta)) {
            throw new ContentException(Messages.REVERT_OPERATION_CANNOT_BE_DONE_PRESERVED_APPLICATIONS_HAVE_DIFFERENT_CHECKSUMS);
        }

        PreservedDescriptor preservedDescriptor = null;
        try {
            preservedDescriptor = descriptorPreserverService.createQuery()
                                                            .mtaId(mtaId)
                                                            .spaceId(spaceGuid)
                                                            .namespace(mtaNamespace)
                                                            .checksum(descriptorChecksumOfPreservedMta)
                                                            .singleResult();
        } catch (NoResultException e) {
            throw new ContentException(Messages.REVERT_MTA_ID_0_CANNOT_BE_DONE_MISSING_DESCRIPTOR, mtaId);
        }
        return preservedDescriptor;
    }

    private boolean doesAllDeployedAppsChecksumMatch(DeployedMta preservedMta, String descriptorChecksumOfPreservedMta) {
        return preservedMta.getApplications()
                           .stream()
                           .allMatch(application -> descriptorChecksumOfPreservedMta.equals(application.getV3Metadata()
                                                                                                       .getLabels()
                                                                                                       .get(MtaMetadataLabels.MTA_DESCRIPTOR_CHECKSUM)));
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_DURING_PREPARATION_PRESERVED_MTA;
    }

}
