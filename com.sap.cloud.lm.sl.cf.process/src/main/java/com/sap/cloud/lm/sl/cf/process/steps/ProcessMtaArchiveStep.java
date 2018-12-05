package com.sap.cloud.lm.sl.cf.process.steps;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.jar.Manifest;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaArchiveHelper;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.persistence.processors.DefaultFileDownloadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.processors.FileDownloadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.services.FileContentProcessor;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ProcessConflictPreventer;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.handlers.ArchiveHandler;
import com.sap.cloud.lm.sl.mta.handlers.DescriptorParserFacade;
import com.sap.cloud.lm.sl.mta.model.Version;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.Resource;
import com.sap.cloud.lm.sl.mta.util.PropertiesUtil;

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
    protected StepPhase executeStep(ExecutionWrapper execution) {
        try {
            getStepLogger().debug(Messages.PROCESSING_MTA_ARCHIVE);

            String appArchiveId = StepsUtil.getRequiredStringParameter(execution.getContext(), Constants.PARAM_APP_ARCHIVE_ID);
            processApplicationArchive(execution.getContext(), appArchiveId);
            setMtaIdForProcess(execution.getContext());
            getStepLogger().debug(Messages.MTA_ARCHIVE_PROCESSED);
            return StepPhase.DONE;
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
                String descriptorString = ArchiveHandler.getDescriptor(appArchiveStream, configuration.getMaxMtaDescriptorSize());
                DescriptorParserFacade descriptorParserFacade = new DescriptorParserFacade();
                DeploymentDescriptor deploymentDescriptor = descriptorParserFacade.parseDeploymentDescriptor(descriptorString);
                StepsUtil.setUnresolvedDeploymentDescriptor(context, deploymentDescriptor);
            });
        fileService.processFileContent(deploymentDescriptorProcessor);

        FileDownloadProcessor manifestProcessor = new DefaultFileDownloadProcessor(StepsUtil.getSpaceId(context), appArchiveId,
            appArchiveStream -> {
                // Create and initialize helper
                Manifest manifest = ArchiveHandler.getManifest(appArchiveStream, configuration.getMaxManifestSize());
                MtaArchiveHelper helper = getHelper(manifest);
                helper.init();

                getStepLogger().debug("MTA Archive ID: {0}", appArchiveId);

                // Set MTA archive modules in the context
                Map<String, String> mtaArchiveModules = helper.getMtaArchiveModules();
                mtaArchiveModules.forEach((moduleName, fileName) -> StepsUtil.setModuleFileName(context, moduleName, fileName));
                getStepLogger().debug("MTA Archive Modules: {0}", mtaArchiveModules.keySet());
                StepsUtil.setMtaArchiveModules(context, mtaArchiveModules.keySet());

                Map<String, String> mtaArchiveRequiresDependencies = helper.getMtaRequiresDependencies();
                mtaArchiveRequiresDependencies
                    .forEach((requiresName, fileName) -> StepsUtil.setRequiresFileName(context, requiresName, fileName));
                getStepLogger().debug("MTA Archive Requires: {0}", mtaArchiveRequiresDependencies.keySet());

                // Set MTA archive resources in the context
                Map<String, String> mtaArchiveResources = helper.getMtaArchiveResources();
                mtaArchiveResources.forEach((resourceName, fileName) -> StepsUtil.setResourceFileName(context, resourceName, fileName));
                getStepLogger().debug("MTA Archive Resources: {0}", mtaArchiveResources.keySet());
            });
        fileService.processFileContent(manifestProcessor);

        setResourcesParameters(context, appArchiveId);
    }

    private void setResourcesParameters(DelegateExecution context, String appArchiveId) throws FileStorageException {
        DeploymentDescriptor deploymentDescriptor = StepsUtil.getUnresolvedDeploymentDescriptor(context);

        PropertiesAccessor propertiesAccessor = new HandlerFactory(Version.parseVersion(deploymentDescriptor.getSchemaVersion())
            .getMajor()).getPropertiesAccessor();

        for (Resource resource : deploymentDescriptor.getResources2()) {
            String resourceFileName = StepsUtil.getResourceFileName(context, resource.getName());
            if (resourceFileName == null) {
                continue;
            }
            Map<String, Object> parameters = propertiesAccessor.getParameters(resource);

            Map<String, Object> contentConfigParameters = JsonUtil
                .convertJsonToMap(getResourceFileContent(context, appArchiveId, resourceFileName));
            Map<String, Object> mergedConfigParameters = PropertiesUtil.mergeExtensionProperties(contentConfigParameters,
                getConfigParameters(resource, propertiesAccessor));
            parameters.put(SupportedParameters.SERVICE_CONFIG, mergedConfigParameters);

            propertiesAccessor.setParameters(resource, parameters);
        }

        StepsUtil.setUnresolvedDeploymentDescriptor(context, deploymentDescriptor);
    }

    private Map<String, Object> getConfigParameters(Resource resource, PropertiesAccessor propertiesAccessor) {
        Object currentResourceConfigParameters = propertiesAccessor.getParameters(resource)
            .get(SupportedParameters.SERVICE_CONFIG);

        if (currentResourceConfigParameters == null) {
            return Collections.emptyMap();
        }

        return (Map<String, Object>) currentResourceConfigParameters;
    }

    private String getResourceFileContent(DelegateExecution context, String appArchiveId, String fileName) throws FileStorageException {
        StringBuilder fileContent = new StringBuilder();
        FileContentProcessor parametersContentProcessor = appArchiveStream -> {
            try (InputStream inputStream = ArchiveHandler.getInputStream(appArchiveStream, fileName, configuration.getMaxManifestSize())) {
                fileContent.append(IOUtils.toString(inputStream, StandardCharsets.UTF_8));
            }
        };
        fileService
            .processFileContent(new DefaultFileDownloadProcessor(StepsUtil.getSpaceId(context), appArchiveId, parametersContentProcessor));

        return fileContent.toString();
    }

    protected MtaArchiveHelper getHelper(Manifest manifest) {
        return new MtaArchiveHelper(manifest);
    }

    private void setMtaIdForProcess(DelegateExecution context) {
        DeploymentDescriptor deploymentDescriptor = StepsUtil.getUnresolvedDeploymentDescriptor(context);
        String mtaId = deploymentDescriptor.getId();
        context.setVariable(Constants.PARAM_MTA_ID, mtaId);
        conflictPreventerSupplier.apply(operationDao)
            .attemptToAcquireLock(mtaId, StepsUtil.getSpaceId(context), StepsUtil.getCorrelationId(context));
    }

}
