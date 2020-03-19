package com.sap.cloud.lm.sl.cf.process.steps;

import java.io.InputStream;
import java.util.Map;
import java.util.function.Function;
import java.util.jar.Manifest;

import javax.inject.Inject;

import com.sap.cloud.lm.sl.cf.core.helpers.MtaArchiveElements;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaArchiveHelper;
import com.sap.cloud.lm.sl.cf.core.persistence.service.OperationService;
import com.sap.cloud.lm.sl.cf.persistence.services.FileContentProcessor;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ProcessConflictPreventer;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.mta.handlers.ArchiveHandler;
import com.sap.cloud.lm.sl.mta.handlers.DescriptorParserFacade;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;

public class ProcessMtaArchiveStep extends SyncFlowableStep {

    protected Function<OperationService, ProcessConflictPreventer> conflictPreventerSupplier = ProcessConflictPreventer::new;
    @Inject
    private OperationService operationService;

    @Override
    protected StepPhase executeStep(ProcessContext context) throws FileStorageException {
        getStepLogger().debug(Messages.PROCESSING_MTA_ARCHIVE);

        String appArchiveId = StepsUtil.getRequiredString(context.getExecution(), Constants.PARAM_APP_ARCHIVE_ID);
        processApplicationArchive(context, appArchiveId);
        setMtaIdForProcess(context);
        getStepLogger().debug(Messages.MTA_ARCHIVE_PROCESSED);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_PROCESSING_MTA_ARCHIVE;
    }

    private void processApplicationArchive(ProcessContext context, String appArchiveId) throws FileStorageException {
        fileService.processFileContent(StepsUtil.getSpaceId(context.getExecution()), appArchiveId,
                                       createDeploymentDescriptorFileContentProcessor(context));
        fileService.processFileContent(StepsUtil.getSpaceId(context.getExecution()), appArchiveId,
                                       createManifestFileContentProcessor(context, appArchiveId));
    }

    private FileContentProcessor createDeploymentDescriptorFileContentProcessor(ProcessContext context) {
        return appArchiveStream -> {
            String descriptorString = ArchiveHandler.getDescriptor(appArchiveStream, configuration.getMaxMtaDescriptorSize());
            DescriptorParserFacade descriptorParserFacade = new DescriptorParserFacade();
            DeploymentDescriptor deploymentDescriptor = descriptorParserFacade.parseDeploymentDescriptor(descriptorString);
            context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, deploymentDescriptor);
        };
    }

    private FileContentProcessor createManifestFileContentProcessor(ProcessContext context, String appArchiveId) {
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
        context.getExecution()
               .setVariable(Constants.PARAM_MTA_ID, mtaId);
        conflictPreventerSupplier.apply(operationService)
                                 .acquireLock(mtaId, StepsUtil.getSpaceId(context.getExecution()),
                                              StepsUtil.getCorrelationId(context.getExecution()));
    }
}
