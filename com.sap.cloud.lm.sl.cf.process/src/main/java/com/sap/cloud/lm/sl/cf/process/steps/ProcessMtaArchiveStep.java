package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.Manifest;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.dao.OngoingOperationDao;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaArchiveHelper;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ProcessConflictPreventer;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.handlers.ArchiveHandler;
import com.sap.cloud.lm.sl.mta.parsers.v1_0.DeploymentDescriptorParser;
import com.sap.cloud.lm.sl.mta.util.YamlUtil;
import com.sap.cloud.lm.sl.persistence.processors.DefaultFileDownloadProcessor;
import com.sap.cloud.lm.sl.persistence.processors.FileDownloadProcessor;
import com.sap.cloud.lm.sl.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("processMtaArchiveStep")
public class ProcessMtaArchiveStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessMtaArchiveStep.class);

    @Inject
    private OngoingOperationDao ongoingOperationDao;

    protected Function<OngoingOperationDao, ProcessConflictPreventer> conflictPreventerSupplier = (dao) -> new ProcessConflictPreventer(
        ongoingOperationDao);

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("processArchiveTask").displayName("Process Archive").description("Process Archive").build();
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {

        logActivitiTask(context, LOGGER);
        try {
            info(context, Messages.PROCESSING_MTA_ARCHIVE, LOGGER);

            String appArchiveId = StepsUtil.getRequiredStringParameter(context, Constants.PARAM_APP_ARCHIVE_ID);
            processApplicationArchive(context, appArchiveId);
            setMtaIdForProcess(context);
            debug(context, Messages.MTA_ARCHIVE_PROCESSED, LOGGER);
            return ExecutionStatus.SUCCESS;
        } catch (FileStorageException fse) {
            SLException e = new SLException(fse, fse.getMessage());
            error(context, Messages.ERROR_PROCESSING_MTA_ARCHIVE, e, LOGGER);
            throw e;
        } catch (SLException e) {
            error(context, Messages.ERROR_PROCESSING_MTA_ARCHIVE, e, LOGGER);
            throw e;
        }
    }

    private void processApplicationArchive(final DelegateExecution context, String appArchiveId) throws FileStorageException {
        FileDownloadProcessor deploymentDescriptorProcessor = new DefaultFileDownloadProcessor(StepsUtil.getSpaceId(context), appArchiveId,
            appArchiveStream -> {
                // Set deployment descriptor string in the context
                String descriptorString = ArchiveHandler.getDescriptor(appArchiveStream, ConfigurationUtil.getMaxMtaDescriptorSize());
                StepsUtil.setDeploymentDescriptorString(context, descriptorString);
            });
        fileService.processFileContent(deploymentDescriptorProcessor);

        FileDownloadProcessor manifestProcessor = new DefaultFileDownloadProcessor(StepsUtil.getSpaceId(context), appArchiveId,
            appArchiveStream -> {
                // Create and initialize helper
                Manifest manifest = ArchiveHandler.getManifest(appArchiveStream);
                MtaArchiveHelper helper = getHelper(manifest);
                helper.init();

                // Set MTA archive modules in the context
                Map<String, String> mtaArchiveModules = helper.getMtaArchiveModules();
                mtaArchiveModules.forEach((moduleName, fileName) -> StepsUtil.setModuleFileName(context, moduleName, fileName));
                debug(context, format("MTA Archive Modules: {0}", mtaArchiveModules.keySet()), LOGGER);
                StepsUtil.setMtaArchiveModules(context, mtaArchiveModules.keySet());

                Map<String, String> mtaArchiveRequiresDependencies = helper.getMtaRequiresDependencies();
                mtaArchiveRequiresDependencies.forEach(
                    (requiresName, fileName) -> StepsUtil.setRequiresFileName(context, requiresName, fileName));
                debug(context, format("MTA Archive Requires: {0}", mtaArchiveRequiresDependencies.keySet()), LOGGER);

                // Set MTA archive resources in the context
                Map<String, String> mtaArchiveResources = helper.getMtaArchiveResources();
                mtaArchiveResources.forEach((resourceName, fileName) -> StepsUtil.setResourceFileName(context, resourceName, fileName));
                debug(context, format("MTA Archive Resources: {0}", mtaArchiveResources.keySet()), LOGGER);

                // Set MTA modules in the context
                Set<String> mtaModules = helper.getMtaModules();
                debug(context, format("MTA Modules: {0}", mtaModules), LOGGER);
                StepsUtil.setMtaModules(context, mtaModules);
            });
        fileService.processFileContent(manifestProcessor);
    }

    private void setMtaIdForProcess(DelegateExecution context) {
        String descriptorString = StepsUtil.getDeploymentDescriptorString(context);
        Map<String, Object> descriptorMap = YamlUtil.convertYamlToMap(descriptorString);
        String mtaId = (String) descriptorMap.get(DeploymentDescriptorParser.ID);
        context.setVariable(Constants.PARAM_MTA_ID, mtaId);
        conflictPreventerSupplier.apply(ongoingOperationDao).attemptToAcquireLock(mtaId, StepsUtil.getSpaceId(context),
            StepsUtil.getCorrelationId(context));
    }

    protected MtaArchiveHelper getHelper(Manifest manifest) {
        return new MtaArchiveHelper(manifest);
    }
}
