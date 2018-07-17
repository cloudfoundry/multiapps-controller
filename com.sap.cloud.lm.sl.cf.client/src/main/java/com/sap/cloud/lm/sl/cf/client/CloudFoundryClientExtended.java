package com.sap.cloud.lm.sl.cf.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.ApplicationLogListener;
import org.cloudfoundry.client.lib.CloudFoundryClient;
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

public class CloudFoundryClientExtended extends CloudFoundryClient implements CloudFoundryOperationsExtended {

    private final ExecutionRetrier retrier = new ExecutionRetrier();

    public CloudFoundryClientExtended(CloudControllerClient cc) {
        super(cc);
    }

    @Override
    public List<String> getSpaceManagers2(String spaceName) {
        return executeWithRetry(() -> super.getSpaceManagers(spaceName).stream()
            .map(uuid -> uuid.toString())
            .collect(Collectors.toList()), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<String> getSpaceManagers2(UUID spaceGuid) {
        return executeWithRetry(() -> super.getSpaceManagers(spaceGuid).stream()
            .map(uuid -> uuid.toString())
            .collect(Collectors.toList()), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<String> getSpaceDevelopers2(String spaceName) {
        return executeWithRetry(() -> super.getSpaceDevelopers(spaceName).stream()
            .map(uuid -> uuid.toString())
            .collect(Collectors.toList()), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<String> getSpaceDevelopers2(UUID spaceGuid) {
        return executeWithRetry(() -> super.getSpaceDevelopers(spaceGuid).stream()
            .map(uuid -> uuid.toString())
            .collect(Collectors.toList()), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<String> getSpaceAuditors2(String spaceName) {
        return executeWithRetry(() -> super.getSpaceAuditors(spaceName).stream()
            .map(uuid -> uuid.toString())
            .collect(Collectors.toList()), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<String> getSpaceAuditors2(UUID spaceGuid) {
        return executeWithRetry(() -> super.getSpaceAuditors(spaceGuid).stream()
            .map(uuid -> uuid.toString())
            .collect(Collectors.toList()), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<String> getSpaceManagers2(String orgName, String spaceName) {
        return executeWithRetry(() -> super.getSpaceManagers(orgName, spaceName).stream()
            .map(uuid -> uuid.toString())
            .collect(Collectors.toList()), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<String> getSpaceDevelopers2(String orgName, String spaceName) {
        return executeWithRetry(() -> super.getSpaceDevelopers(orgName, spaceName).stream()
            .map(uuid -> uuid.toString())
            .collect(Collectors.toList()), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<String> getSpaceAuditors2(String orgName, String spaceName) {
        return executeWithRetry(() -> super.getSpaceAuditors(orgName, spaceName).stream()
            .map(uuid -> uuid.toString())
            .collect(Collectors.toList()), HttpStatus.NOT_FOUND);
    }

    @Override
    public void createService(CloudService service) {
        executeWithRetry(() -> super.createService(service));
    }

    @Override
    public void addDomain(String domainName) {
        executeWithRetry(() -> super.addDomain(domainName));
    }

    @Override
    public void addRoute(String host, String domainName) {
        executeWithRetry(() -> super.addRoute(host, domainName));
    }

    @Override
    public void bindService(String appName, String serviceName) {
        executeWithRetry(() -> super.bindService(appName, serviceName));
    }

    @Override
    public void createApplication(String appName, Staging staging, Integer disk, Integer memory, List<String> uris,
        List<String> serviceNames) {
        executeWithRetry(() -> super.createApplication(appName, staging, disk, memory, uris, serviceNames));
    }

    @Override
    public void createServiceBroker(CloudServiceBroker serviceBroker) {
        executeWithRetry(() -> super.createServiceBroker(serviceBroker));
    }

    @Override
    public void createUserProvidedService(CloudService service, Map<String, Object> credentials) {
        executeWithRetry(() -> super.createUserProvidedService(service, credentials));
    }

    @Override
    public void deleteApplication(String appName) {
        executeWithRetry(() -> super.deleteApplication(appName));
    }

    @Override
    public void deleteDomain(String domainName) {
        executeWithRetry(() -> super.deleteDomain(domainName));
    }

    @Override
    public List<CloudRoute> deleteOrphanedRoutes() {
        return executeWithRetry(() -> super.deleteOrphanedRoutes(), HttpStatus.NOT_FOUND);
    }

    @Override
    public void deleteRoute(String host, String domainName) {
        executeWithRetry(() -> super.deleteRoute(host, domainName));
    }

    @Override
    public void deleteService(String service) {
        executeWithRetry(() -> super.deleteService(service));
    }

    @Override
    public void deleteServiceBroker(String name) {
        executeWithRetry(() -> super.deleteServiceBroker(name));
    }

    @Override
    public CloudApplication getApplication(String appName) {
        return executeWithRetry(() -> super.getApplication(appName));
    }

    @Override
    public CloudApplication getApplication(String appName, boolean required) {
        return executeWithRetry(() -> super.getApplication(appName, required));
    }

    @Override
    public CloudApplication getApplication(UUID appGuid) {
        return executeWithRetry(() -> super.getApplication(appGuid));
    }

    @Override
    public CloudApplication getApplication(UUID appGuid, boolean required) {
        return executeWithRetry(() -> super.getApplication(appGuid, required));
    }

    @Override
    public InstancesInfo getApplicationInstances(String appName) {
        return executeWithRetry(() -> super.getApplicationInstances(appName));
    }

    @Override
    public InstancesInfo getApplicationInstances(CloudApplication app) {
        return executeWithRetry(() -> super.getApplicationInstances(app));
    }

    @Override
    public List<CloudApplication> getApplications() {
        return executeWithRetry(() -> super.getApplications(), HttpStatus.NOT_FOUND);
    }

    @Override
    public CloudDomain getDefaultDomain() {
        return executeWithRetry(() -> super.getDefaultDomain());
    }

    @Override
    public List<CloudDomain> getDomains() {
        return executeWithRetry(() -> super.getDomains(), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<CloudDomain> getDomainsForOrg() {
        return executeWithRetry(() -> super.getDomainsForOrg(), HttpStatus.NOT_FOUND);
    }

    @Override
    public CloudOrganization getOrganization(String orgName) {
        return executeWithRetry(() -> super.getOrganization(orgName));
    }

    @Override
    public CloudOrganization getOrganization(String orgName, boolean required) {
        return executeWithRetry(() -> super.getOrganization(orgName, required));
    }

    @Override
    public List<CloudDomain> getPrivateDomains() {
        return executeWithRetry(() -> super.getPrivateDomains(), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<ApplicationLog> getRecentLogs(String appName) {
        return executeWithRetry(() -> super.getRecentLogs(appName), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<CloudRoute> getRoutes(String domainName) {
        return executeWithRetry(() -> super.getRoutes(domainName), HttpStatus.NOT_FOUND);
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name) {
        return executeWithRetry(() -> super.getServiceBroker(name));
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name, boolean required) {
        return executeWithRetry(() -> super.getServiceBroker(name, required));

    }

    @Override
    public List<CloudServiceBroker> getServiceBrokers() {
        return executeWithRetry(() -> super.getServiceBrokers(), HttpStatus.NOT_FOUND);
    }

    @Override
    public CloudServiceInstance getServiceInstance(String service) {
        return executeWithRetry(() -> super.getServiceInstance(service));
    }

    @Override
    public CloudServiceInstance getServiceInstance(String service, boolean required) {
        return executeWithRetry(() -> super.getServiceInstance(service, required));
    }

    @Override
    public List<CloudDomain> getSharedDomains() {
        return executeWithRetry(() -> super.getSharedDomains(), HttpStatus.NOT_FOUND);
    }

    @Override
    public CloudSpace getSpace(String spaceName) {
        return executeWithRetry(() -> super.getSpace(spaceName));
    }

    @Override
    public CloudSpace getSpace(String spaceName, boolean required) {
        return executeWithRetry(() -> super.getSpace(spaceName, required));
    }

    @Override
    public List<CloudSpace> getSpaces() {
        return executeWithRetry(() -> super.getSpaces(), HttpStatus.NOT_FOUND);
    }

    @Override
    public String getStagingLogs(StartingInfo info, int offset) {
        return executeWithRetry(() -> super.getStagingLogs(info, offset), HttpStatus.NOT_FOUND);
    }

    @Override
    public void rename(String appName, String newName) {
        executeWithRetry(() -> super.rename(appName, newName));
    }

    @Override
    public StartingInfo restartApplication(String appName) {
        return executeWithRetry(() -> super.restartApplication(appName));
    }

    @Override
    public StartingInfo startApplication(String appName) {
        return executeWithRetry(() -> super.startApplication(appName));
    }

    @Override
    public void stopApplication(String appName) {
        executeWithRetry(() -> super.stopApplication(appName));
    }

    @Override
    public StreamingLogToken streamLogs(String appName, ApplicationLogListener listener) {
        return executeWithRetry(() -> super.streamLogs(appName, listener), HttpStatus.NOT_FOUND);
    }

    @Override
    public void unbindService(String appName, String serviceName) {
        executeWithRetry(() -> super.unbindService(appName, serviceName));
    }

    @Override
    public void updateApplicationDiskQuota(String appName, int disk) {
        executeWithRetry(() -> super.updateApplicationDiskQuota(appName, disk));
    }

    @Override
    public void updateApplicationEnv(String appName, Map<String, String> env) {
        executeWithRetry(() -> super.updateApplicationEnv(appName, env));
    }

    @Override
    public void updateApplicationEnv(String appName, List<String> env) {
        executeWithRetry(() -> super.updateApplicationEnv(appName, env));
    }

    @Override
    public void updateApplicationInstances(String appName, int instances) {
        executeWithRetry(() -> super.updateApplicationInstances(appName, instances));
    }

    @Override
    public void updateApplicationMemory(String appName, int memory) {
        executeWithRetry(() -> super.updateApplicationMemory(appName, memory));
    }

    @Override
    public void updateApplicationServices(String appName, List<String> services) {
        executeWithRetry(() -> super.updateApplicationServices(appName, services), HttpStatus.NOT_FOUND);
    }

    @Override
    public void updateApplicationStaging(String appName, Staging staging) {
        executeWithRetry(() -> super.updateApplicationStaging(appName, staging));
    }

    @Override
    public void updateApplicationUris(String appName, List<String> uris) {
        executeWithRetry(() -> super.updateApplicationUris(appName, uris), HttpStatus.NOT_FOUND);
    }

    @Override
    public void updateServiceBroker(CloudServiceBroker serviceBroker) {
        executeWithRetry(() -> super.updateServiceBroker(serviceBroker));
    }

    @Override
    public void updateServicePlanVisibilityForBroker(String name, boolean visibility) {
        executeWithRetry(() -> super.updateServicePlanVisibilityForBroker(name, visibility));
    }

    @Override
    public void uploadApplication(String appName, File file, UploadStatusCallback callback) throws IOException {
        executeWithRetry(() -> {
            try {
                super.uploadApplication(appName, file, callback);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void uploadApplication(String appName, InputStream inputStream, UploadStatusCallback callback) throws IOException {
        executeWithRetry(() -> {
            try {
                super.uploadApplication(appName, inputStream, callback);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void uploadApplication(String appName, ApplicationArchive archive, UploadStatusCallback callback) throws IOException {
        executeWithRetry(() -> {
            try {
                super.uploadApplication(appName, archive, callback);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public String asyncUploadApplication(String appName, File file, UploadStatusCallback callback) throws IOException {
        return executeWithRetry(() -> {
            try {
                return super.asyncUploadApplication(appName, file, callback);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public String asyncUploadApplication(String appName, ApplicationArchive archive, UploadStatusCallback callback) throws IOException {
        return executeWithRetry(() -> {
            try {
                return super.asyncUploadApplication(appName, archive, callback);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public Upload getUploadStatus(String uploadToken) {
        return executeWithRetry(() -> super.getUploadStatus(uploadToken));
    }

    @Override
    public CloudService getService(String service) {
        return executeWithRetry(() -> super.getService(service));
    }

    @Override
    public CloudService getService(String service, boolean required) {
        return executeWithRetry(() -> super.getService(service, required));
    }

    @Override
    public List<CloudService> getServices() {
        return executeWithRetry(() -> super.getServices(), HttpStatus.NOT_FOUND);
    }

    private <T> T executeWithRetry(Supplier<T> supplier, HttpStatus... httpStatusesToIgnore) {
        return retrier.executeWithRetry(supplier, httpStatusesToIgnore);
    }

    private void executeWithRetry(Runnable runnable, HttpStatus... httpStatusesToIgnore) {
        retrier.executeWithRetry(runnable, httpStatusesToIgnore);
    }
}
