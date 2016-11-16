package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.StagingExtended;
import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientFactory.PlatformType;
import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationEntityUpdater;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.mta.handlers.ArchiveHandler;
import com.sap.cloud.lm.sl.mta.util.ValidatorUtil;
import com.sap.cloud.lm.sl.persistence.processors.DefaultFileDownloadProcessor;
import com.sap.cloud.lm.sl.persistence.services.FileContentProcessor;
import com.sap.cloud.lm.sl.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("createAppStep")
public class CreateAppStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateAppStep.class);

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Autowired
    protected ApplicationEntityUpdater applicationEntityUpdater;

    public static StepMetadata getMetadata() {
        return new StepMetadata("createAppTask", "Create App", "Create App");
    }

    protected Function<DelegateExecution, ClientExtensions> extensionsSupplier = (context) -> getClientExtensions(context, LOGGER);

    protected Function<DelegateExecution, CloudFoundryOperations> clientSupplier = (context) -> getCloudFoundryClient(context, LOGGER);

    protected Supplier<PlatformType> platformTypeSupplier = () -> ConfigurationUtil.getPlatformType();

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException, FileStorageException {
        logActivitiTask(context, LOGGER);

        // Get the next cloud application from the context:
        CloudApplicationExtended app = StepsUtil.getApp(context);

        try {
            info(context, format(Messages.CREATING_APP, app.getName()), LOGGER);

            CloudFoundryOperations client = clientSupplier.apply(context);

            // Get application parameters:
            String appName = app.getName();
            Map<String, String> env = app.getEnvAsMap();
            StagingExtended staging = app.getStaging();
            Integer diskQuota = (app.getDiskQuota() != 0) ? app.getDiskQuota() : null;
            Integer memory = (app.getMemory() != 0) ? app.getMemory() : null;
            List<String> uris = app.getUris();
            List<String> serviceNames = app.getServices();
            Map<String, Map<String, Object>> bindingParameters = getBindingParameters(context, app);

            // Check if an application with this name already exists (as a result of a previous
            // execution):
            CloudApplication existingApp = getApplication(client, app.getName());
            // If the application doesn't exist, create it:
            if (existingApp == null) {
                client.createApplication(appName, staging, diskQuota, memory, uris, Collections.emptyList());
                if (platformTypeSupplier.get() == PlatformType.CF) {
                    applicationEntityUpdater.updateApplicationStaging(client, appName, staging);
                }
            }
            // In all cases, update its environment:
            client.updateApplicationEnv(appName, env);
            if (existingApp == null) {
                for (String serviceName : serviceNames) {
                    Map<String, Object> bindingParametersForCurrentService = getBindingParametersForService(serviceName, bindingParameters);
                    bindService(context, client, appName, serviceName, bindingParametersForCurrentService);
                }
            }

            StepsUtil.setAppPropertiesChanged(context, true);

            debug(context, format(Messages.APP_CREATED, app.getName()), LOGGER);
            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            error(context, format(Messages.ERROR_CREATING_APP, app.getName()), e, LOGGER);
            throw e;
        } catch (CloudFoundryException exception) {
            SLException e = StepsUtil.createException(exception);
            error(context, format(Messages.ERROR_CREATING_APP, app.getName()), e, LOGGER);
            throw e;
        }
    }

    protected Map<String, Map<String, Object>> getBindingParameters(DelegateExecution context, CloudApplicationExtended app)
        throws SLException, FileStorageException {
        List<CloudServiceExtended> services = getServices(StepsUtil.getServicesToCreate(context), app.getServices());

        Map<String, Map<String, Object>> descriptorProvidedBindingParameters = app.getBindingParameters();
        if (descriptorProvidedBindingParameters == null) {
            descriptorProvidedBindingParameters = Collections.emptyMap();
        }
        Map<String, Map<String, Object>> fileProvidedBindingParameters = getFileProvidedBindingParameters(context, app.getModuleName(),
            services);
        Map<String, Map<String, Object>> bindingParameters = mergeBindingParameters(descriptorProvidedBindingParameters,
            fileProvidedBindingParameters);
        debug(context, format(Messages.BINDING_PARAMETERS_FOR_APPLICATION, app.getName(), secureSerializer.toJson(bindingParameters)),
            LOGGER);
        return bindingParameters;
    }

    private static List<CloudServiceExtended> getServices(List<CloudServiceExtended> services, List<String> serviceNames) {
        return services.stream().filter((service) -> serviceNames.contains(service.getName())).collect(Collectors.toList());
    }

    private Map<String, Map<String, Object>> getFileProvidedBindingParameters(DelegateExecution context, String moduleName,
        List<CloudServiceExtended> services) throws SLException, FileStorageException {
        Map<String, Map<String, Object>> result = new TreeMap<>();
        for (CloudServiceExtended service : services) {
            String requiredDependencyName = ValidatorUtil.getPrefixedName(moduleName, service.getResourceName(),
                com.sap.cloud.lm.sl.cf.core.Constants.MTA_ELEMENT_SEPARATOR);
            addFileProvidedBindingParameters(context, service.getName(), requiredDependencyName, result);
        }
        return result;
    }

    private void addFileProvidedBindingParameters(DelegateExecution context, String serviceName, String requiredDependencyName,
        Map<String, Map<String, Object>> result) throws SLException, FileStorageException {
        String archiveId = StepsUtil.getRequiredStringParameter(context, Constants.PARAM_APP_ARCHIVE_ID);
        String fileName = StepsUtil.getRequiresFileName(context, requiredDependencyName);
        if (fileName == null) {
            return;
        }
        FileContentProcessor fileProcessor = new FileContentProcessor() {
            @Override
            public void processFileContent(InputStream archive) throws SLException {
                try (InputStream file = ArchiveHandler.getInputStream(archive, fileName)) {
                    MapUtil.addNonNull(result, serviceName, JsonUtil.convertJsonToMap(file));
                } catch (IOException e) {
                    throw new SLException(e, Messages.ERROR_RETRIEVING_MTA_REQUIRED_DEPENDENCY_CONTENT, fileName);
                }
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

    private CloudApplication getApplication(CloudFoundryOperations client, String applicationName) {
        try {
            return client.getApplication(applicationName);
        } catch (CloudFoundryException e) {
            if (!e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                throw e;
            }
        }
        return null;
    }

    private CloudService getService(CloudFoundryOperations client, String serviceName) {
        try {
            return client.getService(serviceName);
        } catch (CloudFoundryException e) {
            if (!e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                throw e;
            }
        }
        return null;
    }

    protected void bindService(DelegateExecution context, CloudFoundryOperations client, String appName, String serviceName,
        Map<String, Object> bindingParameters) throws SLException {
        if (getService(client, serviceName) == null) {
            throw new SLException(Messages.CANNOT_BIND_APP_TO_NON_EXISTING_SERVICE, appName, serviceName);
        }
        if (bindingParameters != null) {
            bindServiceWithParameters(context, client, appName, serviceName, bindingParameters);
        } else {
            bindService(context, client, appName, serviceName);
        }
    }

    private void bindServiceWithParameters(DelegateExecution context, CloudFoundryOperations client, String appName, String serviceName,
        Map<String, Object> bindingParameters) {
        ClientExtensions clientExtensions = extensionsSupplier.apply(context);
        if (clientExtensions == null) {
            warn(context, format(Messages.CLIENT_DOES_NOT_SUPPORT_SERVICE_BINDING_PARAMETERS, appName, serviceName), LOGGER);
            bindService(context, client, appName, serviceName);
        } else {
            debug(context,
                format(Messages.BINDING_APP_TO_SERVICE_WITH_PARAMETERS, appName, serviceName, bindingParameters.get(serviceName)), LOGGER);
            clientExtensions.bindService(appName, serviceName, bindingParameters);
        }
    }

    private void bindService(DelegateExecution context, CloudFoundryOperations client, String appName, String serviceName) {
        debug(context, format(Messages.BINDING_APP_TO_SERVICE, appName, serviceName), LOGGER);
        client.bindService(appName, serviceName);
    }

    protected static Map<String, Object> getBindingParametersForService(String serviceName,
        Map<String, Map<String, Object>> bindingParameters) {
        return (bindingParameters == null) ? null : bindingParameters.get(serviceName);
    }

}
