package com.sap.cloud.lm.sl.cf.process.steps;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.Staging;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.XsCloudControllerClient;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceKeyToInject;
import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ApplicationStagingUpdater;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceBindingCreator;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.persistence.processors.DefaultFileDownloadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.services.FileContentProcessor;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ServiceOperationUtil;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.mta.handlers.ArchiveHandler;
import com.sap.cloud.lm.sl.mta.util.ValidatorUtil;

@Component("createAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CreateAppStep extends SyncFlowableStep {

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Autowired
    protected ApplicationStagingUpdater applicationStagingUpdater;

    @Autowired(required = false)
    ServiceBindingCreator serviceBindingCreator;

    @Autowired
    protected ApplicationConfiguration configuration;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws FileStorageException {
        // Get the next cloud application from the context:
        CloudApplicationExtended app = StepsUtil.getApp(execution.getContext());

        try {
            getStepLogger().info(Messages.CREATING_APP_FROM_MODULE, app.getName(), app.getModuleName());

            CloudControllerClient client = execution.getControllerClient();

            // Get application parameters:
            String appName = app.getName();
            Map<String, String> env = app.getEnvAsMap();
            Staging staging = app.getStaging();
            Integer diskQuota = (app.getDiskQuota() != 0) ? app.getDiskQuota() : null;
            Integer memory = (app.getMemory() != 0) ? app.getMemory() : null;
            List<String> uris = app.getUris();
            List<String> services = app.getServices();
            Map<String, Map<String, Object>> bindingParameters = getBindingParameters(execution.getContext(), app);

            // Check if an application with this name already exists (as a result of a previous
            // execution):
            CloudApplication existingApp = client.getApplication(app.getName(), false);

            // If the application doesn't exist, create it:
            if (existingApp == null) {
                if (app.getDockerInfo() != null) {
                    execution.getStepLogger()
                        .info(Messages.CREATING_APP_FROM_DOCKER_IMAGE, app.getName(), app.getDockerInfo()
                            .getImage());
                }
                client.createApplication(appName, staging, diskQuota, memory, uris, Collections.emptyList(), app.getDockerInfo());

                if (configuration.getPlatformType() == PlatformType.CF) {
                    applicationStagingUpdater.updateApplicationStaging(client, appName, staging);
                }
            }

            injectServiceKeysCredentialsInAppEnv(execution.getContext(), client, app, env);

            // In all cases, update its environment:
            client.updateApplicationEnv(appName, env);

            if (existingApp == null) {
                for (String serviceName : services) {
                    Map<String, Object> bindingParametersForCurrentService = getBindingParametersForService(serviceName, bindingParameters);
                    bindService(execution, client, appName, serviceName, bindingParametersForCurrentService);
                }
            }

            StepsUtil.setVcapAppPropertiesChanged(execution.getContext(), true);
            StepsUtil.setVcapServicesPropertiesChanged(execution.getContext(), true);
            StepsUtil.setUserPropertiesChanged(execution.getContext(), true);

            getStepLogger().debug(Messages.APP_CREATED, app.getName());
            return StepPhase.DONE;
        } catch (CloudOperationException coe) {
            CloudControllerException e = new CloudControllerException(coe);
            getStepLogger().error(e, Messages.ERROR_CREATING_APP, app.getName());
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_CREATING_APP, app.getName());
            throw e;
        }
    }

    protected void injectServiceKeysCredentialsInAppEnv(DelegateExecution context, CloudControllerClient client,
        CloudApplicationExtended app, Map<String, String> appEnv) {
        Map<String, String> appServiceKeysCredentials = buildServiceKeysCredentials(client, app, appEnv);
        app.setEnv(MapUtil.upcast(appEnv));
        updateContextWithServiceKeysCredentials(context, app, appServiceKeysCredentials);
    }

    private Map<String, String> buildServiceKeysCredentials(CloudControllerClient client, CloudApplicationExtended app,
        Map<String, String> appEnv) {
        Map<String, String> appServiceKeysCredentials = new HashMap<>();
        for (ServiceKeyToInject serviceKeyToInject : app.getServiceKeysToInject()) {
            String serviceKeyCredentials = JsonUtil.toJson(ServiceOperationUtil.getServiceKeyCredentials(client,
                serviceKeyToInject.getServiceName(), serviceKeyToInject.getServiceKeyName()), true);
            appEnv.put(serviceKeyToInject.getEnvVarName(), serviceKeyCredentials);
            appServiceKeysCredentials.put(serviceKeyToInject.getEnvVarName(), serviceKeyCredentials);
        }
        return appServiceKeysCredentials;
    }

    private void updateContextWithServiceKeysCredentials(DelegateExecution context, CloudApplicationExtended app,
        Map<String, String> appServiceKeysCredentials) {
        Map<String, Map<String, String>> serviceKeysCredentialsToInject = StepsUtil.getServiceKeysCredentialsToInject(context);
        serviceKeysCredentialsToInject.put(app.getName(), appServiceKeysCredentials);

        // Update current process context
        StepsUtil.setApp(context, app);
        StepsUtil.setServiceKeysCredentialsToInject(context, serviceKeysCredentialsToInject);
    }

    protected Map<String, Map<String, Object>> getBindingParameters(DelegateExecution context, CloudApplicationExtended app)
        throws FileStorageException {
        List<CloudServiceExtended> services = getServices(StepsUtil.getServicesToBind(context), app.getServices());

        Map<String, Map<String, Object>> descriptorProvidedBindingParameters = app.getBindingParameters();
        if (descriptorProvidedBindingParameters == null) {
            descriptorProvidedBindingParameters = Collections.emptyMap();
        }
        Map<String, Map<String, Object>> fileProvidedBindingParameters = getFileProvidedBindingParameters(context, app.getModuleName(),
            services);
        Map<String, Map<String, Object>> bindingParameters = mergeBindingParameters(descriptorProvidedBindingParameters,
            fileProvidedBindingParameters);
        getStepLogger().debug(Messages.BINDING_PARAMETERS_FOR_APPLICATION, app.getName(), secureSerializer.toJson(bindingParameters));
        return bindingParameters;
    }

    protected static List<CloudServiceExtended> getServices(List<CloudServiceExtended> services, List<String> serviceNames) {
        return services.stream()
            .filter(service -> serviceNames.contains(service.getName()))
            .collect(Collectors.toList());
    }

    private Map<String, Map<String, Object>> getFileProvidedBindingParameters(DelegateExecution context, String moduleName,
        List<CloudServiceExtended> services) throws FileStorageException {
        Map<String, Map<String, Object>> result = new TreeMap<>();
        for (CloudServiceExtended service : services) {
            String requiredDependencyName = ValidatorUtil.getPrefixedName(moduleName, service.getResourceName(),
                com.sap.cloud.lm.sl.cf.core.Constants.MTA_ELEMENT_SEPARATOR);
            addFileProvidedBindingParameters(context, service.getName(), requiredDependencyName, result);
        }
        return result;
    }

    private void addFileProvidedBindingParameters(DelegateExecution context, String serviceName, String requiredDependencyName,
        Map<String, Map<String, Object>> result) throws FileStorageException {
        String archiveId = StepsUtil.getRequiredStringParameter(context, Constants.PARAM_APP_ARCHIVE_ID);
        String fileName = StepsUtil.getRequiresFileName(context, requiredDependencyName);
        if (fileName == null) {
            return;
        }
        FileContentProcessor fileProcessor = archive -> {
            try (InputStream file = ArchiveHandler.getInputStream(archive, fileName, configuration.getMaxManifestSize())) {
                MapUtil.addNonNull(result, serviceName, JsonUtil.convertJsonToMap(file));
            } catch (IOException e) {
                throw new SLException(e, Messages.ERROR_RETRIEVING_MTA_REQUIRED_DEPENDENCY_CONTENT, fileName);
            }
        };
        fileService.processFileContent(new DefaultFileDownloadProcessor(StepsUtil.getSpaceId(context), archiveId, fileProcessor));
    }

    private static Map<String, Map<String, Object>> mergeBindingParameters(
        Map<String, Map<String, Object>> descriptorProvidedBindingParameters,
        Map<String, Map<String, Object>> fileProvidedBindingParameters) {
        Map<String, Map<String, Object>> bindingParameters = new HashMap<>();
        Set<String> serviceNames = new HashSet<>(descriptorProvidedBindingParameters.keySet());
        serviceNames.addAll(fileProvidedBindingParameters.keySet());
        for (String serviceName : serviceNames) {
            bindingParameters.put(serviceName,
                MapUtil.mergeSafely(fileProvidedBindingParameters.get(serviceName), descriptorProvidedBindingParameters.get(serviceName)));
        }
        return bindingParameters;
    }

    private CloudServiceExtended findServiceCloudModel(List<CloudServiceExtended> servicesCloudModel, String serviceName) {
        return servicesCloudModel.stream()
            .filter(service -> service.getName()
                .equals(serviceName))
            .findAny()
            .orElse(null);
    }

    protected void bindService(ExecutionWrapper execution, CloudControllerClient client, String appName, String serviceName,
        Map<String, Object> bindingParameters) {

        try {
            bindServiceToApplication(execution, client, appName, serviceName, bindingParameters);
        } catch (CloudOperationException e) {
            List<CloudServiceExtended> servicesToBind = StepsUtil.getServicesToBind(execution.getContext());
            CloudServiceExtended serviceToBind = findServiceCloudModel(servicesToBind, serviceName);

            if (serviceToBind != null && serviceToBind.isOptional()) {
                getStepLogger().warn(e, Messages.COULD_NOT_BIND_APP_TO_OPTIONAL_SERVICE, appName, serviceName);
                return;
            }
            throw new SLException(e, Messages.COULD_NOT_BIND_APP_TO_SERVICE, appName, serviceName, e.getMessage());
        }
    }

    private void bindServiceToApplication(ExecutionWrapper execution, CloudControllerClient client, String appName, String serviceName,
        Map<String, Object> bindingParameters) {
        if (bindingParameters != null) {
            XsCloudControllerClient xsClient = execution.getXsControllerClient();
            bindServiceWithParameters(xsClient, client, appName, serviceName, bindingParameters);
        } else {
            bindService(client, appName, serviceName);
        }
    }

    // TODO Fix update of service bindings parameters
    private void bindServiceWithParameters(XsCloudControllerClient xsClient, CloudControllerClient client, String appName,
        String serviceName, Map<String, Object> bindingParameters) {
        getStepLogger().debug(Messages.BINDING_APP_TO_SERVICE_WITH_PARAMETERS, appName, serviceName, bindingParameters.get(serviceName));
        if (xsClient == null) {
            serviceBindingCreator.bindService(client, appName, serviceName, bindingParameters);
        } else {
            xsClient.bindService(appName, serviceName, bindingParameters);
        }
    }

    private void bindService(CloudControllerClient client, String appName, String serviceName) {
        getStepLogger().debug(Messages.BINDING_APP_TO_SERVICE, appName, serviceName);
        client.bindService(appName, serviceName);
    }

    protected static Map<String, Object> getBindingParametersForService(String serviceName,
        Map<String, Map<String, Object>> bindingParameters) {
        return (bindingParameters == null) ? null : bindingParameters.get(serviceName);
    }

    protected static Map<String, Object> getBindingParametersOrDefault(CloudServiceBinding cloudServiceBinding) {
        Map<String, Object> bindingParameters = cloudServiceBinding.getBindingOptions();
        return MapUtils.isEmpty(bindingParameters) ? null : bindingParameters;
    }

}
