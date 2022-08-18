package org.cloudfoundry.multiapps.controller.process.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaArchiveElements;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.persistence.services.FileContentProcessor;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.handlers.ArchiveHandler;
import org.cloudfoundry.multiapps.mta.util.NameUtil;
import org.cloudfoundry.multiapps.mta.util.PropertiesUtil;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;

public class ServiceBindingParametersGetter {

    private final ProcessContext context;
    private final FileService fileService;
    private final long maxManifestSize;

    public ServiceBindingParametersGetter(ProcessContext context, FileService fileService, long maxManifestSize) {
        this.context = context;
        this.fileService = fileService;
        this.maxManifestSize = maxManifestSize;
    }

    public Map<String, Object> getServiceBindingParametersFromMta(CloudApplicationExtended app, String serviceName)
        throws FileStorageException {
        Optional<CloudServiceInstanceExtended> service = getService(context.getVariable(Variables.SERVICES_TO_BIND), serviceName);
        if (service.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> fileProvidedBindingParameters = getFileProvidedBindingParameters(app.getModuleName(), service.get());
        Map<String, Object> descriptorProvidedBindingParameters = getDescriptorProvidedBindingParameters(app, service.get());
        Map<String, Object> bindingParameters = PropertiesUtil.mergeExtensionProperties(fileProvidedBindingParameters,
                                                                                        descriptorProvidedBindingParameters);
        context.getStepLogger()
               .debug(Messages.BINDING_PARAMETERS_FOR_APPLICATION, app.getName(), SecureSerialization.toJson(bindingParameters));
        return bindingParameters;
    }

    private Optional<CloudServiceInstanceExtended> getService(List<CloudServiceInstanceExtended> services, String serviceName) {
        return services.stream()
                       .filter(service -> service.getName()
                                                 .equals(serviceName))
                       .findFirst();
    }

    private Map<String, Object> getFileProvidedBindingParameters(String moduleName, CloudServiceInstanceExtended service)
        throws FileStorageException {

        String requiredDependencyName = NameUtil.getPrefixedName(moduleName, service.getResourceName(),
                                                                 org.cloudfoundry.multiapps.controller.core.Constants.MTA_ELEMENT_SEPARATOR);
        return getFileProvidedBindingParameters(requiredDependencyName);

    }

    private Map<String, Object> getFileProvidedBindingParameters(String requiredDependencyName) throws FileStorageException {
        String archiveId = context.getRequiredVariable(Variables.APP_ARCHIVE_ID);
        MtaArchiveElements mtaArchiveElements = context.getVariable(Variables.MTA_ARCHIVE_ELEMENTS);
        String fileName = mtaArchiveElements.getRequiredDependencyFileName(requiredDependencyName);
        if (fileName == null) {
            return Collections.emptyMap();
        }
        FileContentProcessor<Map<String, Object>> fileProcessor = archive -> {
            try (InputStream file = ArchiveHandler.getInputStream(archive, fileName, maxManifestSize)) {
                return JsonUtil.convertJsonToMap(file);
            } catch (IOException e) {
                throw new SLException(e, Messages.ERROR_RETRIEVING_MTA_REQUIRED_DEPENDENCY_CONTENT, fileName);
            }
        };
        return fileService.processFileContent(context.getVariable(Variables.SPACE_GUID), archiveId, fileProcessor);
    }

    private Map<String, Object> getDescriptorProvidedBindingParameters(CloudApplicationExtended app, CloudServiceInstanceExtended service) {
        return app.getBindingParameters()
                  .getOrDefault(service.getResourceName(), Collections.emptyMap());
    }

    public Map<String, Object> getServiceBindingParametersFromExistingInstance(CloudApplication application, String serviceName) {
        CloudControllerClient client = context.getControllerClient();
        UUID serviceGuid = client.getRequiredServiceInstanceGuid(serviceName);
        CloudServiceBinding serviceBinding = client.getServiceBindingForApplication(application.getGuid(), serviceGuid);
        if (serviceBinding == null) {
            throw new SLException(Messages.SERVICE_INSTANCE_0_NOT_BOUND_TO_APP_1, serviceName, application.getName());
        }

        try {
            return client.getServiceBindingParameters(serviceBinding.getGuid());
        } catch (CloudOperationException e) {
            if (HttpStatus.NOT_IMPLEMENTED == e.getStatusCode() || HttpStatus.BAD_REQUEST == e.getStatusCode()) {
                // ignore 501 and 400 error codes from service brokers
                context.getStepLogger()
                       .warnWithoutProgressMessage(Messages.CANNOT_RETRIEVE_PARAMETERS_OF_BINDING_BETWEEN_APPLICATION_0_AND_SERVICE_INSTANCE_1,
                                                   application.getName(), serviceName);
                return null;
            } else if (HttpStatus.BAD_GATEWAY == e.getStatusCode()) {
                // TODO: this is a temporary fix for external error code mapping leading to incorrect 502 errors
                context.getStepLogger()
                       .warnWithoutProgressMessage(Messages.CANNOT_RETRIEVE_PARAMETERS_OF_BINDING_BETWEEN_APPLICATION_0_AND_SERVICE_INSTANCE_1_FIX,
                                                   application.getName(), serviceName);
                return null;
            }
            throw e;
        }
    }

}
