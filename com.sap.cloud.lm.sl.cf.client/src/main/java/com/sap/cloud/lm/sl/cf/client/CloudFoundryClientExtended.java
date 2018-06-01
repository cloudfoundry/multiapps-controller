package com.sap.cloud.lm.sl.cf.client;

import static com.sap.cloud.lm.sl.cf.client.util.FunctionUtil.callable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.ApplicationLogListener;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.StreamingLogToken;
import org.cloudfoundry.client.lib.UploadStatusCallback;
import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.client.lib.domain.Upload;
import org.cloudfoundry.client.lib.rest.CloudControllerClient;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.client.util.ExecutionRetrier;
import com.sap.cloud.lm.sl.cf.client.util.TimeoutExecutor;

public class CloudFoundryClientExtended extends CloudFoundryClient implements CloudFoundryOperationsExtended {

    private final ExecutionRetrier retrier = new ExecutionRetrier();
    private TimeoutExecutor timeoutExecutor;

    public CloudFoundryClientExtended(CloudControllerClient cc) {
        super(cc);
    }

    @Override
    public void withTimeoutExecutor(TimeoutExecutor timeoutExecutor) {
        this.timeoutExecutor = timeoutExecutor;
    }

    @Override
    public List<String> getSpaceManagers2(String spaceName) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getSpaceManagers(spaceName).stream()
                .map(uuid -> uuid.toString())
                .collect(Collectors.toList()), HttpStatus.NOT_FOUND));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), cause, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Override
    public List<String> getSpaceManagers2(UUID spaceGuid) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getSpaceManagers(spaceGuid).stream()
                .map(uuid -> uuid.toString())
                .collect(Collectors.toList()), HttpStatus.NOT_FOUND));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), cause, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Override
    public List<String> getSpaceDevelopers2(String spaceName) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getSpaceDevelopers(spaceName).stream()
                .map(uuid -> uuid.toString())
                .collect(Collectors.toList()), HttpStatus.NOT_FOUND));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Override
    public List<String> getSpaceDevelopers2(UUID spaceGuid) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getSpaceDevelopers(spaceGuid).stream()
                .map(uuid -> uuid.toString())
                .collect(Collectors.toList()), HttpStatus.NOT_FOUND));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public List<String> getSpaceAuditors2(String spaceName) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getSpaceAuditors(spaceName).stream()
                .map(uuid -> uuid.toString())
                .collect(Collectors.toList()), HttpStatus.NOT_FOUND));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Override
    public List<String> getSpaceAuditors2(UUID spaceGuid) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getSpaceAuditors(spaceGuid).stream()
                .map(uuid -> uuid.toString())
                .collect(Collectors.toList()), HttpStatus.NOT_FOUND));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public List<String> getSpaceManagers2(String orgName, String spaceName) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getSpaceManagers(orgName, spaceName).stream()
                .map(uuid -> uuid.toString())
                .collect(Collectors.toList()), HttpStatus.NOT_FOUND));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public List<String> getSpaceDevelopers2(String orgName, String spaceName) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getSpaceDevelopers(orgName, spaceName).stream()
                .map(uuid -> uuid.toString())
                .collect(Collectors.toList()), HttpStatus.NOT_FOUND));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public List<String> getSpaceAuditors2(String orgName, String spaceName) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getSpaceAuditors(orgName, spaceName).stream()
                .map(uuid -> uuid.toString())
                .collect(Collectors.toList()), HttpStatus.NOT_FOUND));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Override
    public void createService(CloudService service) {
        try {
            executeWithTimeout(callable(() -> executeWithRetry(() -> super.createService(service))));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void addDomain(String domainName) {
        try {
            executeWithTimeout(callable(() -> executeWithRetry(() -> super.addDomain(domainName))));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void addRoute(String host, String domainName) {
        try {
            executeWithTimeout(callable(() -> executeWithRetry(() -> super.addRoute(host, domainName))));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void bindService(String appName, String serviceName) {
        try {
            executeWithTimeout(callable(() -> executeWithRetry(() -> super.bindService(appName, serviceName))));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void createApplication(String appName, Staging staging, Integer disk, Integer memory, List<String> uris,
        List<String> serviceNames) {
        try {
            executeWithTimeout(
                callable(() -> executeWithRetry(() -> super.createApplication(appName, staging, disk, memory, uris, serviceNames))));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void createServiceBroker(CloudServiceBroker serviceBroker) {
        try {
            executeWithTimeout(callable(() -> executeWithRetry(() -> super.createServiceBroker(serviceBroker))));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void createUserProvidedService(CloudService service, Map<String, Object> credentials) {
        try {
            executeWithTimeout(callable(() -> executeWithRetry(() -> super.createUserProvidedService(service, credentials))));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void deleteApplication(String appName) {
        try {
            executeWithTimeout(callable(() -> executeWithRetry(() -> {
                super.deleteApplication(appName);
            })));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void deleteDomain(String domainName) {
        try {
            executeWithTimeout(callable(() -> executeWithRetry(() -> super.deleteDomain(domainName))));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public List<CloudRoute> deleteOrphanedRoutes() {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.deleteOrphanedRoutes(), HttpStatus.NOT_FOUND));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void deleteRoute(String host, String domainName) {
        try {
            executeWithTimeout(callable(() -> executeWithRetry(() -> super.deleteRoute(host, domainName))));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void deleteService(String service) {
        try {
            executeWithTimeout(callable(() -> executeWithRetry(() -> super.deleteService(service))));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void deleteServiceBroker(String name) {
        try {
            executeWithTimeout(callable(() -> executeWithRetry(() -> super.deleteServiceBroker(name))));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public CloudApplication getApplication(String appName) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getApplication(appName)));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public CloudApplication getApplication(String appName, boolean required) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getApplication(appName, required)));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public CloudApplication getApplication(UUID appGuid) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getApplication(appGuid)));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public CloudApplication getApplication(UUID appGuid, boolean required) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getApplication(appGuid, required)));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public InstancesInfo getApplicationInstances(String appName) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getApplicationInstances(appName)));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public InstancesInfo getApplicationInstances(CloudApplication app) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getApplicationInstances(app)));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public List<CloudApplication> getApplications() {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getApplications(), HttpStatus.NOT_FOUND));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public CloudDomain getDefaultDomain() {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getDefaultDomain()));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public List<CloudDomain> getDomains() {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getDomains(), HttpStatus.NOT_FOUND));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public List<CloudDomain> getDomainsForOrg() {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getDomainsForOrg(), HttpStatus.NOT_FOUND));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public CloudOrganization getOrganization(String orgName) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getOrganization(orgName)));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public CloudOrganization getOrganization(String orgName, boolean required) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getOrganization(orgName, required)));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public List<CloudDomain> getPrivateDomains() {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getPrivateDomains(), HttpStatus.NOT_FOUND));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public List<ApplicationLog> getRecentLogs(String appName) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getRecentLogs(appName), HttpStatus.NOT_FOUND));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public List<CloudRoute> getRoutes(String domainName) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getRoutes(domainName), HttpStatus.NOT_FOUND));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getServiceBroker(name)));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name, boolean required) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getServiceBroker(name, required)));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @Override
    public List<CloudServiceBroker> getServiceBrokers() {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getServiceBrokers(), HttpStatus.NOT_FOUND));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public CloudServiceInstance getServiceInstance(String service) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getServiceInstance(service)));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public CloudServiceInstance getServiceInstance(String service, boolean required) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getServiceInstance(service, required)));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public List<CloudDomain> getSharedDomains() {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getSharedDomains(), HttpStatus.NOT_FOUND));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public CloudSpace getSpace(String spaceName) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getSpace(spaceName)));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public CloudSpace getSpace(String spaceName, boolean required) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getSpace(spaceName, required)));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public List<CloudSpace> getSpaces() {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getSpaces(), HttpStatus.NOT_FOUND));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public String getStagingLogs(StartingInfo info, int offset) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getStagingLogs(info, offset), HttpStatus.NOT_FOUND));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void rename(String appName, String newName) {
        try {
            executeWithTimeout(callable(() -> executeWithRetry(() -> super.rename(appName, newName))));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public StartingInfo restartApplication(String appName) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.restartApplication(appName)));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public StartingInfo startApplication(String appName) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.startApplication(appName)));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void stopApplication(String appName) {
        try {
            executeWithTimeout(callable(() -> executeWithRetry(() -> super.stopApplication(appName))));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public StreamingLogToken streamLogs(String appName, ApplicationLogListener listener) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.streamLogs(appName, listener), HttpStatus.NOT_FOUND));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void unbindService(String appName, String serviceName) {
        try {
            executeWithTimeout(callable(() -> executeWithRetry(() -> super.unbindService(appName, serviceName))));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void updateApplicationDiskQuota(String appName, int disk) {
        try {
            executeWithTimeout(callable(() -> executeWithRetry(() -> super.updateApplicationDiskQuota(appName, disk))));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void updateApplicationEnv(String appName, Map<String, String> env) {
        try {
            executeWithTimeout(callable(() -> executeWithRetry(() -> super.updateApplicationEnv(appName, env))));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void updateApplicationEnv(String appName, List<String> env) {
        try {
            executeWithTimeout(callable(() -> executeWithRetry(() -> super.updateApplicationEnv(appName, env))));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void updateApplicationInstances(String appName, int instances) {
        try {
            executeWithTimeout(callable(() -> executeWithRetry(() -> super.updateApplicationInstances(appName, instances))));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void updateApplicationMemory(String appName, int memory) {
        try {
            executeWithTimeout(callable(() -> executeWithRetry(() -> super.updateApplicationMemory(appName, memory))));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void updateApplicationServices(String appName, List<String> services) {
        try {
            executeWithTimeout(
                callable(() -> executeWithRetry(() -> super.updateApplicationServices(appName, services), HttpStatus.NOT_FOUND)));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void updateApplicationStaging(String appName, Staging staging) {
        try {
            executeWithTimeout(callable(() -> executeWithRetry(() -> super.updateApplicationStaging(appName, staging))));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void updateApplicationUris(String appName, List<String> uris) {
        try {
            executeWithTimeout(callable(() -> executeWithRetry(() -> super.updateApplicationUris(appName, uris), HttpStatus.NOT_FOUND)));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void updateServiceBroker(CloudServiceBroker serviceBroker) {
        try {
            executeWithTimeout(callable(() -> executeWithRetry(() -> super.updateServiceBroker(serviceBroker))));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void updateServicePlanVisibilityForBroker(String name, boolean visibility) {
        try {
            executeWithTimeout(callable(() -> executeWithRetry(() -> super.updateServicePlanVisibilityForBroker(name, visibility))));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void uploadApplication(String appName, File file, UploadStatusCallback callback) throws IOException {
        try {
            executeWithTimeout(callable(() -> executeWithRetry(() -> {
                try {
                    super.uploadApplication(appName, file, callback);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            })));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void uploadApplication(String appName, InputStream inputStream, UploadStatusCallback callback) throws IOException {
        try {
            executeWithTimeout(callable(() -> executeWithRetry(() -> {
                try {
                    super.uploadApplication(appName, inputStream, callback);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            })));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void uploadApplication(String appName, ApplicationArchive archive, UploadStatusCallback callback) throws IOException {
        try {
            executeWithTimeout(callable(() -> executeWithRetry(() -> {
                try {
                    super.uploadApplication(appName, archive, callback);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            })));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public String asyncUploadApplication(String appName, File file, UploadStatusCallback callback) throws IOException {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> {
                try {
                    return super.asyncUploadApplication(appName, file, callback);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public String asyncUploadApplication(String appName, ApplicationArchive archive, UploadStatusCallback callback) throws IOException {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> {
                try {
                    return super.asyncUploadApplication(appName, archive, callback);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Upload getUploadStatus(String uploadToken) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getUploadStatus(uploadToken)));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public CloudService getService(String service) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getService(service)));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public CloudService getService(String service, boolean required) {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getService(service, required)));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public List<CloudService> getServices() {
        try {
            return executeWithTimeout(() -> executeWithRetry(() -> super.getServices(), HttpStatus.NOT_FOUND));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw fromException(cause.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private <T> T executeWithTimeout(Callable<T> callable) throws ExecutionException {
        try {
            if (timeoutExecutor == null) {
                return callable.call();
            }

            return timeoutExecutor.executeWithTimeout(callable);
        } catch (ExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    private static CloudFoundryException fromException(String message, Throwable e, HttpStatus status) {
        if (e instanceof CloudFoundryException) {
            throw (CloudFoundryException) e;
        }
        CloudFoundryException ex = new CloudFoundryException(status, message + ": " + e.getMessage());
        ex.initCause(e);
        return ex;
    }

    private <T> T executeWithRetry(Supplier<T> supplier, HttpStatus... httpStatusesToIgnore) {
        return retrier.executeWithRetry(supplier, httpStatusesToIgnore);
    }

    private void executeWithRetry(Runnable runnable, HttpStatus... httpStatusesToIgnore) {
        retrier.executeWithRetry(runnable, httpStatusesToIgnore);
    }
}
