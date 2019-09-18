package com.sap.cloud.lm.sl.cf.process.steps;

import java.io.InputStream;
import java.util.Map;
import java.util.function.Function;
import java.util.jar.Manifest;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.core.helpers.MtaArchiveElements;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaArchiveHelper;
import com.sap.cloud.lm.sl.cf.core.persistence.service.OperationService;
import com.sap.cloud.lm.sl.cf.persistence.processors.DefaultFileDownloadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.processors.FileDownloadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.services.FileContentProcessor;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ProcessConflictPreventer;
import com.sap.cloud.lm.sl.mta.handlers.ArchiveHandler;
import com.sap.cloud.lm.sl.mta.handlers.DescriptorParserFacade;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;

@Named("processMtaArchiveStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ProcessMtaArchiveStep extends SyncFlowableStep {

    protected Function<OperationService, ProcessConflictPreventer> conflictPreventerSupplier = ProcessConflictPreventer::new;
    @Inject
    private OperationService operationService;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws FileStorageException {
        getStepLogger().debug(Messages.PROCESSING_MTA_ARCHIVE);

        String appArchiveId = StepsUtil.getRequiredString(execution.getContext(), Constants.PARAM_APP_ARCHIVE_ID);
        processApplicationArchive(execution.getContext(), appArchiveId);
        setMtaIdForProcess(execution.getContext());
        getStepLogger().debug(Messages.MTA_ARCHIVE_PROCESSED);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(DelegateExecution context) {
        return Messages.ERROR_PROCESSING_MTA_ARCHIVE;
    }

    private void processApplicationArchive(final DelegateExecution context, String appArchiveId) throws FileStorageException {
        FileDownloadProcessor deploymentDescriptorProcessor = new DefaultFileDownloadProcessor(StepsUtil.getSpaceId(context),
                                                                                               appArchiveId,
                                                                                               createDeploymentDescriptorFileContentProcessor(context));
        fileService.processFileContent(deploymentDescriptorProcessor);
        FileDownloadProcessor manifestProcessor = new DefaultFileDownloadProcessor(StepsUtil.getSpaceId(context),
                                                                                   appArchiveId,
                                                                                   createManifestFileContentProcessor(appArchiveId,
                                                                                                                      context));
        fileService.processFileContent(manifestProcessor);
    }

    private FileContentProcessor createDeploymentDescriptorFileContentProcessor(DelegateExecution context) {
        return appArchiveStream -> {
            String descriptorString = ArchiveHandler.getDescriptor(appArchiveStream, configuration.getMaxMtaDescriptorSize());
            DescriptorParserFacade descriptorParserFacade = new DescriptorParserFacade();
            DeploymentDescriptor deploymentDescriptor = descriptorParserFacade.parseDeploymentDescriptor(descriptorString);
            StepsUtil.setDeploymentDescriptor(context, deploymentDescriptor);
        };
    }

    private FileContentProcessor createManifestFileContentProcessor(String appArchiveId, DelegateExecution context) {
        return appArchiveStream -> {
            MtaArchiveHelper helper = createInitializedMtaArchiveHelper(appArchiveStream);
            getStepLogger().debug("MTA Archive ID: {0}", appArchiveId);
            MtaArchiveElements mtaArchiveElements = new MtaArchiveElements();
            addMtaArchiveModulesInMtaArchiveElements(helper, mtaArchiveElements, context);
            addMtaRequiredDependenciesInMtaArchiveElements(helper, mtaArchiveElements);
            addMtaArchiveResourcesInMtaArchiveElements(helper, mtaArchiveElements);
            StepsUtil.setMtaArchiveElements(context, mtaArchiveElements);
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

    private void addMtaArchiveModulesInMtaArchiveElements(MtaArchiveHelper helper, MtaArchiveElements mtaArchiveElements,
                                                          DelegateExecution context) {
        Map<String, String> mtaArchiveModules = helper.getMtaArchiveModules();
        mtaArchiveModules.forEach(mtaArchiveElements::addModuleFileName);
        getStepLogger().debug("MTA Archive Modules: {0}", mtaArchiveModules.keySet());
        StepsUtil.setMtaArchiveModules(context, mtaArchiveModules.keySet());
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

    private void setMtaIdForProcess(DelegateExecution context) {
        DeploymentDescriptor deploymentDescriptor = StepsUtil.getDeploymentDescriptor(context);
        String mtaId = deploymentDescriptor.getId();
        context.setVariable(Constants.PARAM_MTA_ID, mtaId);
        conflictPreventerSupplier.apply(operationService)
                                 .acquireLock(mtaId, StepsUtil.getSpaceId(context), StepsUtil.getCorrelationId(context));
    }
}
