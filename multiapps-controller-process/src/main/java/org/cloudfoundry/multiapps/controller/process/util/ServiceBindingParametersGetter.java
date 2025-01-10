package org.cloudfoundry.multiapps.controller.process.util;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.BindingDetails;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaArchiveElements;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.handlers.ArchiveHandler;
import org.cloudfoundry.multiapps.mta.util.NameUtil;
import org.cloudfoundry.multiapps.mta.util.PropertiesUtil;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ServiceBindingParametersGetter {

    private final ProcessContext context;

    private final ArchiveEntryExtractor archiveEntryExtractor;
    private final long maxManifestSize;
    private final FileService fileService;

    public ServiceBindingParametersGetter(ProcessContext context, ArchiveEntryExtractor archiveEntryExtractor, long maxManifestSize, FileService fileService) {
        this.context = context;
        this.archiveEntryExtractor = archiveEntryExtractor;
        this.maxManifestSize = maxManifestSize;
        this.fileService = fileService;
    }

    public Map<String, Object> getServiceBindingParametersFromMta(CloudApplicationExtended app, String serviceName) {
        Optional<CloudServiceInstanceExtended> service = getService(context.getVariable(Variables.SERVICES_TO_BIND), serviceName);
        if (service.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> bindingParameters = getDescriptorProvidedBindingParameters(app, service.get());

        if (shouldResolveFileBindingParameters(context)) {
            Map<String, Object> fileProvidedBindingParameters = getFileProvidedBindingParameters(app.getModuleName(), service.get());
            bindingParameters = PropertiesUtil.mergeExtensionProperties(fileProvidedBindingParameters, bindingParameters);
        }

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

    private Map<String, Object> getFileProvidedBindingParameters(String moduleName, CloudServiceInstanceExtended service) {
        String requiredDependencyName = NameUtil.getPrefixedName(moduleName, service.getResourceName(),
                                                                 org.cloudfoundry.multiapps.controller.core.Constants.MTA_ELEMENT_SEPARATOR);
        return getFileProvidedBindingParameters(requiredDependencyName);
    }

    private Map<String, Object> getFileProvidedBindingParameters(String requiredDependencyName) {
        MtaArchiveElements mtaArchiveElements = context.getVariable(Variables.MTA_ARCHIVE_ELEMENTS);
        String fileName = mtaArchiveElements.getRequiredDependencyFileName(requiredDependencyName);
        if (fileName == null) {
            return Collections.emptyMap();
        }
        String spaceGuid = context.getRequiredVariable(Variables.SPACE_GUID);
        String appArchiveId = context.getRequiredVariable(Variables.APP_ARCHIVE_ID);
        // TODO: backwards compatibility for one tact
        List<ArchiveEntryWithStreamPositions> archiveEntriesWithStreamPositions = context.getVariable(Variables.ARCHIVE_ENTRIES_POSITIONS);
        if (archiveEntriesWithStreamPositions == null) {
            try {
                return fileService.processFileContent(spaceGuid, appArchiveId, appArchiveStream -> {
                    InputStream fileStream = ArchiveHandler.getInputStream(appArchiveStream, fileName, maxManifestSize);
                    return JsonUtil.convertJsonToMap(fileStream);
                });
            } catch (FileStorageException e) {
                throw new SLException(e, e.getMessage());
            }
        }
        // TODO: backwards compatibility for one tact
        ArchiveEntryWithStreamPositions archiveEntryWithStreamPositions = ArchiveEntryExtractorUtil.findEntry(fileName, context.getVariable(
            Variables.ARCHIVE_ENTRIES_POSITIONS));
        byte[] serviceBindingParametersFileContent = archiveEntryExtractor.extractEntryBytes(ImmutableFileEntryProperties.builder()
                                                                                                                         .guid(appArchiveId)
                                                                                                                         .name(
                                                                                                                             archiveEntryWithStreamPositions.getName())
                                                                                                                         .spaceGuid(context.getRequiredVariable(
                                                                                                                             Variables.SPACE_GUID))
                                                                                                                         .maxFileSizeInBytes(maxManifestSize)
                                                                                                                         .build(),
                                                                                             archiveEntryWithStreamPositions);
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serviceBindingParametersFileContent)) {
            return JsonUtil.convertJsonToMap(byteArrayInputStream);
        } catch (IOException e) {
            throw new SLException(e, Messages.ERROR_RETRIEVING_MTA_REQUIRED_DEPENDENCY_CONTENT, fileName);
        }
    }

    private Map<String, Object> getDescriptorProvidedBindingParameters(CloudApplicationExtended app, CloudServiceInstanceExtended service) {
        BindingDetails bindingDetails = getDescriptorProvidedBindingParametersAndBindingName(app, service);
        return (bindingDetails != null && bindingDetails.getConfig() != null) ? bindingDetails.getConfig() : Collections.emptyMap();
    }

    private boolean shouldResolveFileBindingParameters(ProcessContext context) {
        return !context.getVariable(Variables.SHOULD_BACKUP_PREVIOUS_VERSION);
    }

    private BindingDetails getDescriptorProvidedBindingParametersAndBindingName(CloudApplicationExtended app, CloudServiceInstanceExtended service) {
        return app.getBindingParameters()
                  .get(service.getResourceName());
    }

    public String getDescriptorProvidedBindingName(CloudApplicationExtended app, String serviceName) {
        Optional<CloudServiceInstanceExtended> service = getService(context.getVariable(Variables.SERVICES_TO_BIND), serviceName);
        if (service.isEmpty()) {
            return null;
        }
        BindingDetails bindingDetails = getDescriptorProvidedBindingParametersAndBindingName(app, service.get());
        return (bindingDetails != null && bindingDetails.getBindingName() != null) ? bindingDetails.getBindingName() : null;
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
