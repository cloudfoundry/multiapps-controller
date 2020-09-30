package org.cloudfoundry.multiapps.controller.process.steps;

import java.io.InputStream;
import java.util.Map;
import java.util.function.Function;
import java.util.jar.Manifest;

import javax.inject.Inject;

import org.cloudfoundry.multiapps.controller.core.helpers.DescriptorParserFacadeFactory;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaArchiveElements;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaArchiveHelper;
import org.cloudfoundry.multiapps.controller.persistence.services.FileContentConsumer;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ProcessConflictPreventer;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.handlers.ArchiveHandler;
import org.cloudfoundry.multiapps.mta.handlers.DescriptorParserFacade;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;

public class ProcessMtaArchiveStep extends SyncFlowableStep {

    @Inject
    private OperationService operationService;

    @Inject
    protected DescriptorParserFacadeFactory descriptorParserFactory;

    protected Function<OperationService, ProcessConflictPreventer> conflictPreventerSupplier = ProcessConflictPreventer::new;

    @Override
    protected StepPhase executeStep(ProcessContext context) throws FileStorageException {
        getStepLogger().debug(Messages.PROCESSING_MTA_ARCHIVE);

        String appArchiveId = context.getRequiredVariable(Variables.APP_ARCHIVE_ID);
        processApplicationArchive(context, appArchiveId);
        setMtaIdForProcess(context);
        acquireOperationLock(context);

        getStepLogger().debug(Messages.MTA_ARCHIVE_PROCESSED);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_PROCESSING_MTA_ARCHIVE;
    }

    private void processApplicationArchive(ProcessContext context, String appArchiveId) throws FileStorageException {
        fileService.consumeFileContent(context.getVariable(Variables.SPACE_GUID), appArchiveId,
                                       createDeploymentDescriptorFileContentConsumer(context));
        fileService.consumeFileContent(context.getVariable(Variables.SPACE_GUID), appArchiveId,
                                       createManifestFileContentConsumer(context, appArchiveId));
    }

    private FileContentConsumer createDeploymentDescriptorFileContentConsumer(ProcessContext context) {
        return appArchiveStream -> {
            String descriptorString = ArchiveHandler.getDescriptor(appArchiveStream, configuration.getMaxMtaDescriptorSize());
            DescriptorParserFacade descriptorParserFacade = descriptorParserFactory.getInstance();
            DeploymentDescriptor deploymentDescriptor = descriptorParserFacade.parseDeploymentDescriptor(descriptorString);
            context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, deploymentDescriptor);
        };
    }

    private FileContentConsumer createManifestFileContentConsumer(ProcessContext context, String appArchiveId) {
        return appArchiveStream -> {
            MtaArchiveHelper helper = createInitializedMtaArchiveHelper(appArchiveStream);
            getStepLogger().debug("MTA Archive ID: {0}", appArchiveId);
            MtaArchiveElements mtaArchiveElements = new MtaArchiveElements();
            addMtaArchiveModulesInMtaArchiveElements(context, helper, mtaArchiveElements);
            addMtaRequiredDependenciesInMtaArchiveElements(helper, mtaArchiveElements);
            addMtaArchiveResourcesInMtaArchiveElements(helper, mtaArchiveElements);
            context.setVariable(Variables.MTA_ARCHIVE_ELEMENTS, mtaArchiveElements);
        };
    }

    private MtaArchiveHelper createInitializedMtaArchiveHelper(InputStream appArchiveStream) {
        Manifest manifest = ArchiveHandler.getManifest(appArchiveStream, configuration.getMaxManifestSize());
        MtaArchiveHelper helper = getHelper(manifest);
        helper.init();
        return helper;
    }

    protected MtaArchiveHelper getHelper(Manifest manifest) {
        return new MtaArchiveHelper(manifest);
    }

    private void addMtaArchiveModulesInMtaArchiveElements(ProcessContext context, MtaArchiveHelper helper,
                                                          MtaArchiveElements mtaArchiveElements) {
        Map<String, String> mtaArchiveModules = helper.getMtaArchiveModules();
        mtaArchiveModules.forEach(mtaArchiveElements::addModuleFileName);
        getStepLogger().debug("MTA Archive Modules: {0}", mtaArchiveModules.keySet());
        context.setVariable(Variables.MTA_ARCHIVE_MODULES, mtaArchiveModules.keySet());
    }

    private void addMtaRequiredDependenciesInMtaArchiveElements(MtaArchiveHelper helper, MtaArchiveElements mtaArchiveElements) {
        Map<String, String> mtaArchiveRequiresDependencies = helper.getMtaRequiresDependencies();
        mtaArchiveRequiresDependencies.forEach(mtaArchiveElements::addRequiredDependencyFileName);
        getStepLogger().debug("MTA Archive Requires: {0}", mtaArchiveRequiresDependencies.keySet());
    }

    private void addMtaArchiveResourcesInMtaArchiveElements(MtaArchiveHelper helper, MtaArchiveElements mtaArchiveElements) {
        Map<String, String> mtaArchiveResources = helper.getMtaArchiveResources();
        mtaArchiveResources.forEach(mtaArchiveElements::addResourceFileName);
        getStepLogger().debug("MTA Archive Resources: {0}", mtaArchiveResources.keySet());
    }

    private void setMtaIdForProcess(ProcessContext context) {
        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR);
        String mtaId = deploymentDescriptor.getId();

        context.setVariable(Variables.MTA_ID, mtaId);
    }

    private void acquireOperationLock(ProcessContext context) {
        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR);
        String mtaId = deploymentDescriptor.getId();
        String namespace = context.getVariable(Variables.MTA_NAMESPACE);

        conflictPreventerSupplier.apply(operationService)
                                 .acquireLock(mtaId, namespace, context.getVariable(Variables.SPACE_GUID),
                                              context.getVariable(Variables.CORRELATION_ID));
    }
}
