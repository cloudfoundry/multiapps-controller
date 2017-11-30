package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.Manifest;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaArchiveHelper;
import com.sap.cloud.lm.sl.cf.core.util.Configuration;
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

@Component("processMtaArchiveStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ProcessMtaArchiveStep extends AbstractProcessStep {

    @Inject
    private OperationDao operationDao;
    @Inject
    private Configuration configuration;

    protected Function<OperationDao, ProcessConflictPreventer> conflictPreventerSupplier = (dao) -> new ProcessConflictPreventer(
        operationDao);

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {

        getStepLogger().logActivitiTask();
        try {
            getStepLogger().info(Messages.PROCESSING_MTA_ARCHIVE);

            String appArchiveId = StepsUtil.getRequiredStringParameter(context, Constants.PARAM_APP_ARCHIVE_ID);
            processApplicationArchive(context, appArchiveId);
            setMtaIdForProcess(context);
            getStepLogger().debug(Messages.MTA_ARCHIVE_PROCESSED);
            return ExecutionStatus.SUCCESS;
        } catch (FileStorageException fse) {
            SLException e = new SLException(fse, fse.getMessage());
            getStepLogger().error(e, Messages.ERROR_PROCESSING_MTA_ARCHIVE);
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_PROCESSING_MTA_ARCHIVE);
            throw e;
        }
    }

    private void processApplicationArchive(final DelegateExecution context, String appArchiveId) throws FileStorageException {
        FileDownloadProcessor deploymentDescriptorProcessor = new DefaultFileDownloadProcessor(StepsUtil.getSpaceId(context), appArchiveId,
            appArchiveStream -> {
                // Set deployment descriptor string in the context
                String descriptorString = ArchiveHandler.getDescriptor(appArchiveStream, configuration.getMaxMtaDescriptorSize());
                StepsUtil.setDeploymentDescriptorString(context, descriptorString);
            });
        fileService.processFileContent(deploymentDescriptorProcessor);

        FileDownloadProcessor manifestProcessor = new DefaultFileDownloadProcessor(StepsUtil.getSpaceId(context), appArchiveId,
            appArchiveStream -> {
                // Create and initialize helper
                Manifest manifest = ArchiveHandler.getManifest(appArchiveStream, Configuration.getInstance().getMaxManifestSize());
                MtaArchiveHelper helper = getHelper(manifest);
                helper.init();

                getStepLogger().debug("MTA Archive ID: {0}", appArchiveId);

                // Set MTA archive modules in the context
                Map<String, String> mtaArchiveModules = helper.getMtaArchiveModules();
                mtaArchiveModules.forEach((moduleName, fileName) -> StepsUtil.setModuleFileName(context, moduleName, fileName));
                getStepLogger().debug("MTA Archive Modules: {0}", mtaArchiveModules.keySet());
                StepsUtil.setMtaArchiveModules(context, mtaArchiveModules.keySet());

                Map<String, String> mtaArchiveRequiresDependencies = helper.getMtaRequiresDependencies();
                mtaArchiveRequiresDependencies.forEach(
                    (requiresName, fileName) -> StepsUtil.setRequiresFileName(context, requiresName, fileName));
                getStepLogger().debug("MTA Archive Requires: {0}", mtaArchiveRequiresDependencies.keySet());

                // Set MTA archive resources in the context
                Map<String, String> mtaArchiveResources = helper.getMtaArchiveResources();
                mtaArchiveResources.forEach((resourceName, fileName) -> StepsUtil.setResourceFileName(context, resourceName, fileName));
                getStepLogger().debug("MTA Archive Resources: {0}", mtaArchiveResources.keySet());

                // Set MTA modules in the context
                Set<String> mtaModules = helper.getMtaModules();
                getStepLogger().debug("MTA Modules: {0}", mtaModules);
                StepsUtil.setMtaModules(context, mtaModules);
            });
        fileService.processFileContent(manifestProcessor);
    }

    private void setMtaIdForProcess(DelegateExecution context) {
        String descriptorString = StepsUtil.getDeploymentDescriptorString(context);
        Map<String, Object> descriptorMap = YamlUtil.convertYamlToMap(descriptorString);
        String mtaId = (String) descriptorMap.get(DeploymentDescriptorParser.ID);
        context.setVariable(Constants.PARAM_MTA_ID, mtaId);
        conflictPreventerSupplier.apply(operationDao).attemptToAcquireLock(mtaId, StepsUtil.getSpaceId(context),
            StepsUtil.getCorrelationId(context));
    }

    protected MtaArchiveHelper getHelper(Manifest manifest) {
        return new MtaArchiveHelper(manifest);
    }

}
