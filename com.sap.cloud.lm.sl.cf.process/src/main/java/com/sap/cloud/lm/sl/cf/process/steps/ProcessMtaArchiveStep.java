package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Map;
import java.util.function.Function;
import java.util.jar.Manifest;

import javax.inject.Inject;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaArchiveElements;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaArchiveHelper;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.persistence.processors.DefaultFileDownloadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.processors.FileDownloadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ProcessConflictPreventer;
import com.sap.cloud.lm.sl.mta.handlers.ArchiveHandler;
import com.sap.cloud.lm.sl.mta.handlers.DescriptorParserFacade;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;

@Component("processMtaArchiveStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ProcessMtaArchiveStep extends SyncFlowableStep {

    @Inject
    private OperationDao operationDao;
    @Inject
    private ApplicationConfiguration configuration;

    protected Function<OperationDao, ProcessConflictPreventer> conflictPreventerSupplier = dao -> new ProcessConflictPreventer(
        operationDao);

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
        FileDownloadProcessor deploymentDescriptorProcessor = new DefaultFileDownloadProcessor(StepsUtil.getSpaceId(context), appArchiveId,
            appArchiveStream -> {
                String descriptorString = ArchiveHandler.getDescriptor(appArchiveStream, configuration.getMaxMtaDescriptorSize());
                DescriptorParserFacade descriptorParserFacade = new DescriptorParserFacade();
                DeploymentDescriptor deploymentDescriptor = descriptorParserFacade.parseDeploymentDescriptor(descriptorString);
                StepsUtil.setDeploymentDescriptor(context, deploymentDescriptor);
            });

        fileService.processFileContent(deploymentDescriptorProcessor);

        FileDownloadProcessor manifestProcessor = new DefaultFileDownloadProcessor(StepsUtil.getSpaceId(context), appArchiveId,
            appArchiveStream -> {
                // Create and initialize helper
                Manifest manifest = ArchiveHandler.getManifest(appArchiveStream, configuration.getMaxManifestSize());
                MtaArchiveHelper helper = getHelper(manifest);
                helper.init();

                getStepLogger().debug("MTA Archive ID: {0}", appArchiveId);

                MtaArchiveElements mtaArchiveElements = new MtaArchiveElements();
                // Set MTA archive modules in the context
                Map<String, String> mtaArchiveModules = helper.getMtaArchiveModules();
                mtaArchiveModules.forEach((moduleName, fileName) -> mtaArchiveElements.addModuleFileName(moduleName, fileName));
                getStepLogger().debug("MTA Archive Modules: {0}", mtaArchiveModules.keySet());
                StepsUtil.setMtaArchiveModules(context, mtaArchiveModules.keySet());

                Map<String, String> mtaArchiveRequiresDependencies = helper.getMtaRequiresDependencies();
                mtaArchiveRequiresDependencies
                    .forEach((requiresName, fileName) -> mtaArchiveElements.addRequiredDependencyFileName(requiresName, fileName));
                getStepLogger().debug("MTA Archive Requires: {0}", mtaArchiveRequiresDependencies.keySet());

                // Set MTA archive resources in the context
                Map<String, String> mtaArchiveResources = helper.getMtaArchiveResources();
                mtaArchiveResources.forEach((resourceName, fileName) -> mtaArchiveElements.addResourceFileName(resourceName, fileName));
                getStepLogger().debug("MTA Archive Resources: {0}", mtaArchiveResources.keySet());

                StepsUtil.setMtaArchiveElements(context, mtaArchiveElements);
            });
        fileService.processFileContent(manifestProcessor);
    }

    protected MtaArchiveHelper getHelper(Manifest manifest) {
        return new MtaArchiveHelper(manifest);
    }

    private void setMtaIdForProcess(DelegateExecution context) {
        DeploymentDescriptor deploymentDescriptor = StepsUtil.getDeploymentDescriptor(context);
        String mtaId = deploymentDescriptor.getId();
        context.setVariable(Constants.PARAM_MTA_ID, mtaId);
        conflictPreventerSupplier.apply(operationDao)
            .acquireLock(mtaId, StepsUtil.getSpaceId(context), StepsUtil.getCorrelationId(context));
    }

}
