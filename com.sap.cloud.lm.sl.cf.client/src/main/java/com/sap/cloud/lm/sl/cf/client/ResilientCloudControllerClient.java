package com.sap.cloud.lm.sl.cf.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.cloudfoundry.client.lib.ApplicationServicesUpdateCallback;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerClientImpl;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.RestLogCallback;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.UploadStatusCallback;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudBuild;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudEvent;
import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudQuota;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.client.lib.domain.CloudSecurityGroup;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.CloudServiceKey;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.CloudStack;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.cloudfoundry.client.lib.domain.CloudUser;
import org.cloudfoundry.client.lib.domain.DockerInfo;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.client.lib.domain.Upload;
import org.cloudfoundry.client.lib.domain.UploadToken;
import org.cloudfoundry.client.lib.rest.CloudControllerRestClient;
import org.cloudfoundry.client.v3.Metadata;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.client.ResponseErrorHandler;

import com.sap.cloud.lm.sl.cf.client.util.ResilientCloudOperationExecutor;

public class ResilientCloudControllerClient implements CloudControllerClient {

    private final CloudControllerClientImpl delegate;

    public ResilientCloudControllerClient(CloudControllerRestClient delegate) {
        this.delegate = new CloudControllerClientImpl(delegate);
    }

    @Override
    public void createService(CloudService service) {
        executeWithRetry(() -> delegate.createService(service));
    }

    @Override
    public void addDomain(String domainName) {
        executeWithRetry(() -> delegate.addDomain(domainName));
    }

    @Override
    public void addRoute(String host, String domainName, String path) {
        executeWithRetry(() -> delegate.addRoute(host, domainName, path));
    }

    @Override
    public void bindService(String applicationName, String serviceName) {
        executeWithRetry(() -> delegate.bindService(applicationName, serviceName));
    }

    @Override
    public void bindService(String applicationName, String serviceName, Map<String, Object> parameters,
                            ApplicationServicesUpdateCallback applicationServicesUpdateCallback) {
        executeWithRetry(() -> delegate.bindService(applicationName, serviceName, parameters, applicationServicesUpdateCallback));
    }

    @Override
    public void createApplication(String applicationName, Staging staging, Integer disk, Integer memory, List<String> uris,
                                  List<String> serviceNames, DockerInfo dockerInfo) {
        executeWithRetry(() -> delegate.createApplication(applicationName, staging, disk, memory, uris, serviceNames, dockerInfo));
    }

    @Override
    public void createServiceBroker(CloudServiceBroker serviceBroker) {
        executeWithRetry(() -> delegate.createServiceBroker(serviceBroker));
    }

    @Override
    public void createUserProvidedService(CloudService service, Map<String, Object> credentials) {
        executeWithRetry(() -> delegate.createUserProvidedService(service, credentials));
    }

    @Override
    public void deleteApplication(String applicationName) {
        executeWithRetry(() -> delegate.deleteApplication(applicationName));
    }

    @Override
    public void deleteDomain(String domainName) {
        executeWithRetry(() -> delegate.deleteDomain(domainName));
    }

    @Override
    public List<CloudRoute> deleteOrphanedRoutes() {
        return executeWithRetry(delegate::deleteOrphanedRoutes, HttpStatus.NOT_FOUND);
    }

    @Override
    public void deleteRoute(String host, String domainName, String path) {
        executeWithRetry(() -> delegate.deleteRoute(host, domainName, path));
    }

    @Override
    public void deleteService(String service) {
        executeWithRetry(() -> delegate.deleteService(service));
    }

    @Override
    public void deleteServiceBroker(String name) {
        executeWithRetry(() -> delegate.deleteServiceBroker(name));
    }

    @Override
    public CloudApplication getApplication(String applicationName) {
        return executeWithRetry(() -> delegate.getApplication(applicationName));
    }

    @Override
    public CloudApplication getApplication(String applicationName, boolean required) {
        return executeWithRetry(() -> delegate.getApplication(applicationName, required));
    }

    @Override
    public CloudApplication getApplication(UUID appGuid) {
        return executeWithRetry(() -> delegate.getApplication(appGuid));
    }

    @Override
    public InstancesInfo getApplicationInstances(String applicationName) {
        return executeWithRetry(() -> delegate.getApplicationInstances(applicationName));
    }

    @Override
    public InstancesInfo getApplicationInstances(CloudApplication app) {
        return executeWithRetry(() -> delegate.getApplicationInstances(app));
    }

    @Override
    public List<CloudApplication> getApplications() {
        return executeWithRetry(() -> delegate.getApplications(), HttpStatus.NOT_FOUND);
    }

    @Override
    public CloudDomain getDefaultDomain() {
        return executeWithRetry(delegate::getDefaultDomain);
    }

    @Override
    public List<CloudDomain> getDomains() {
        return executeWithRetry(delegate::getDomains, HttpStatus.NOT_FOUND);
    }

    @Override
    public List<CloudDomain> getDomainsForOrganization() {
        return executeWithRetry(delegate::getDomainsForOrganization, HttpStatus.NOT_FOUND);
    }

    @Override
    public CloudOrganization getOrganization(String organizationName) {
        return executeWithRetry(() -> delegate.getOrganization(organizationName));
    }

    @Override
    public CloudOrganization getOrganization(String organizationName, boolean required) {
        return executeWithRetry(() -> delegate.getOrganization(organizationName, required));
    }

    @Override
    public List<CloudDomain> getPrivateDomains() {
        return executeWithRetry(delegate::getPrivateDomains, HttpStatus.NOT_FOUND);
    }

    @Override
    public List<ApplicationLog> getRecentLogs(String applicationName) {
        return executeWithRetry(() -> delegate.getRecentLogs(applicationName), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<ApplicationLog> getRecentLogs(UUID applicationGuid) {
        return executeWithRetry(() -> delegate.getRecentLogs(applicationGuid), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<CloudRoute> getRoutes(String domainName) {
        return executeWithRetry(() -> delegate.getRoutes(domainName), HttpStatus.NOT_FOUND);
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name) {
        return executeWithRetry(() -> delegate.getServiceBroker(name));
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name, boolean required) {
        return executeWithRetry(() -> delegate.getServiceBroker(name, required));
    }

    @Override
    public List<CloudServiceBroker> getServiceBrokers() {
        return executeWithRetry(delegate::getServiceBrokers, HttpStatus.NOT_FOUND);
    }

    @Override
    public CloudServiceInstance getServiceInstance(String service) {
        return executeWithRetry(() -> delegate.getServiceInstance(service));
    }

    @Override
    public Map<String, Object> getServiceParameters(UUID guid) {
        return executeWithRetry(() -> delegate.getServiceParameters(guid));
    }

    @Override
    public CloudServiceInstance getServiceInstance(String service, boolean required) {
        return executeWithRetry(() -> delegate.getServiceInstance(service, required));
    }

    @Override
    public List<CloudDomain> getSharedDomains() {
        return executeWithRetry(delegate::getSharedDomains, HttpStatus.NOT_FOUND);
    }

    @Override
    public CloudSpace getSpace(UUID spaceGuid) {
        return executeWithRetry(() -> delegate.getSpace(spaceGuid));
    }

    @Override
    public CloudSpace getSpace(String organizationName, String spaceName) {
        return executeWithRetry(() -> delegate.getSpace(organizationName, spaceName));
    }

    @Override
    public CloudSpace getSpace(String organizationName, String spaceName, boolean required) {
        return executeWithRetry(() -> delegate.getSpace(organizationName, spaceName, required));
    }

    @Override
    public CloudSpace getSpace(String spaceName) {
        return executeWithRetry(() -> delegate.getSpace(spaceName));
    }

    @Override
    public CloudSpace getSpace(String spaceName, boolean required) {
        return executeWithRetry(() -> delegate.getSpace(spaceName, required));
    }

    @Override
    public List<CloudSpace> getSpaces() {
        return executeWithRetry(() -> delegate.getSpaces(), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<CloudSpace> getSpaces(String organizationName) {
        return executeWithRetry(() -> delegate.getSpaces(organizationName), HttpStatus.NOT_FOUND);
    }

    @Override
    public void rename(String applicationName, String newName) {
        executeWithRetry(() -> delegate.rename(applicationName, newName));
    }

    @Override
    public StartingInfo restartApplication(String applicationName) {
        return executeWithRetry(() -> delegate.restartApplication(applicationName));
    }

    @Override
    public StartingInfo startApplication(String applicationName) {
        return executeWithRetry(() -> delegate.startApplication(applicationName));
    }

    @Override
    public void stopApplication(String applicationName) {
        executeWithRetry(() -> delegate.stopApplication(applicationName));
    }

    @Override
    public void unbindService(String applicationName, String serviceName) {
        executeWithRetry(() -> delegate.unbindService(applicationName, serviceName));
    }

    @Override
    public void updateApplicationDiskQuota(String applicationName, int disk) {
        executeWithRetry(() -> delegate.updateApplicationDiskQuota(applicationName, disk));
    }

    @Override
    public void updateApplicationEnv(String applicationName, Map<String, String> env) {
        executeWithRetry(() -> delegate.updateApplicationEnv(applicationName, env));
    }

    @Override
    public void updateApplicationInstances(String applicationName, int instances) {
        executeWithRetry(() -> delegate.updateApplicationInstances(applicationName, instances));
    }

    @Override
    public void updateApplicationMemory(String applicationName, int memory) {
        executeWithRetry(() -> delegate.updateApplicationMemory(applicationName, memory));
    }

    @Override
    public List<String> updateApplicationServices(String applicationName,
                                                  Map<String, Map<String, Object>> serviceNamesWithBindingParameters,
                                                  ApplicationServicesUpdateCallback applicationServicesUpdateCallback) {
        return executeWithRetry(() -> delegate.updateApplicationServices(applicationName, serviceNamesWithBindingParameters,
                                                                         applicationServicesUpdateCallback),
                                HttpStatus.NOT_FOUND);
    }

    @Override
    public void updateApplicationStaging(String applicationName, Staging staging) {
        executeWithRetry(() -> delegate.updateApplicationStaging(applicationName, staging));
    }

    @Override
    public void updateApplicationUris(String applicationName, List<String> uris) {
        executeWithRetry(() -> delegate.updateApplicationUris(applicationName, uris), HttpStatus.NOT_FOUND);
    }

    @Override
    public void updateServiceBroker(CloudServiceBroker serviceBroker) {
        executeWithRetry(() -> delegate.updateServiceBroker(serviceBroker));
    }

    @Override
    public void updateServicePlanVisibilityForBroker(String name, boolean visibility) {
        executeWithRetry(() -> delegate.updateServicePlanVisibilityForBroker(name, visibility));
    }

    @Override
    public void uploadApplication(String applicationName, File file, UploadStatusCallback callback) {
        executeWithRetry(() -> {
            try {
                delegate.uploadApplication(applicationName, file, callback);
            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        });
    }

    @Override
    public void uploadApplication(String applicationName, InputStream inputStream, UploadStatusCallback callback) {
        executeWithRetry(() -> {
            try {
                delegate.uploadApplication(applicationName, inputStream, callback);
            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        });
    }

    @Override
    public UploadToken asyncUploadApplication(String applicationName, File file, UploadStatusCallback callback) {
        return executeWithRetry(() -> {
            try {
                return delegate.asyncUploadApplication(applicationName, file, callback);
            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        });
    }

    @Override
    public Upload getUploadStatus(UUID packageGuid) {
        return executeWithRetry(() -> delegate.getUploadStatus(packageGuid));
    }

    @Override
    public CloudService getService(String service) {
        return executeWithRetry(() -> delegate.getService(service));
    }

    @Override
    public CloudService getService(String service, boolean required) {
        return executeWithRetry(() -> delegate.getService(service, required));
    }

    @Override
    public List<CloudService> getServices() {
        return executeWithRetry(delegate::getServices, HttpStatus.NOT_FOUND);
    }

    @Override
    public void associateAuditorWithSpace(String spaceName) {
        executeWithRetry(() -> delegate.associateAuditorWithSpace(spaceName));
    }

    @Override
    public void associateAuditorWithSpace(String organizationName, String spaceName) {
        executeWithRetry(() -> delegate.associateAuditorWithSpace(organizationName, spaceName));
    }

    @Override
    public void associateAuditorWithSpace(String organizationName, String spaceName, String userGuid) {
        executeWithRetry(() -> delegate.associateAuditorWithSpace(organizationName, spaceName, userGuid));
    }

    @Override
    public void associateDeveloperWithSpace(String spaceName) {
        executeWithRetry(() -> delegate.associateDeveloperWithSpace(spaceName));
    }

    @Override
    public void associateDeveloperWithSpace(String organizationName, String spaceName) {
        executeWithRetry(() -> delegate.associateDeveloperWithSpace(organizationName, spaceName));
    }

    @Override
    public void associateDeveloperWithSpace(String organizationName, String spaceName, String userGuid) {
        executeWithRetry(() -> delegate.associateDeveloperWithSpace(organizationName, spaceName, userGuid));
    }

    @Override
    public void associateManagerWithSpace(String spaceName) {
        executeWithRetry(() -> delegate.associateManagerWithSpace(spaceName));
    }

    @Override
    public void associateManagerWithSpace(String organizationName, String spaceName) {
        executeWithRetry(() -> delegate.associateManagerWithSpace(organizationName, spaceName));
    }

    @Override
    public void associateManagerWithSpace(String organizationName, String spaceName, String userGuid) {
        executeWithRetry(() -> delegate.associateManagerWithSpace(organizationName, spaceName, userGuid));
    }

    @Override
    public void bindRunningSecurityGroup(String securityGroupName) {
        executeWithRetry(() -> delegate.bindRunningSecurityGroup(securityGroupName));
    }

    @Override
    public void bindSecurityGroup(String organizationName, String spaceName, String securityGroupName) {
        executeWithRetry(() -> delegate.bindSecurityGroup(organizationName, spaceName, securityGroupName));
    }

    @Override
    public void bindStagingSecurityGroup(String securityGroupName) {
        executeWithRetry(() -> delegate.bindStagingSecurityGroup(securityGroupName));
    }

    @Override
    public void createApplication(String applicationName, Staging staging, Integer memory, List<String> uris, List<String> serviceNames) {
        executeWithRetry(() -> delegate.createApplication(applicationName, staging, memory, uris, serviceNames));
    }

    @Override
    public void createQuota(CloudQuota quota) {
        executeWithRetry(() -> delegate.createQuota(quota));
    }

    @Override
    public void createSecurityGroup(CloudSecurityGroup securityGroup) {
        executeWithRetry(() -> delegate.createSecurityGroup(securityGroup));
    }

    @Override
    public void createSecurityGroup(String name, InputStream jsonRulesFile) {
        executeWithRetry(() -> delegate.createSecurityGroup(name, jsonRulesFile));
    }

    @Override
    public CloudServiceKey createServiceKey(String serviceName, String serviceKeyName, Map<String, Object> parameters) {
        return executeWithRetry(() -> delegate.createServiceKey(serviceName, serviceKeyName, parameters));
    }

    @Override
    public void createSpace(String spaceName) {
        executeWithRetry(() -> delegate.createSpace(spaceName));
    }

    @Override
    public void createUserProvidedService(CloudService service, Map<String, Object> credentials, String syslogDrainUrl) {
        executeWithRetry(() -> delegate.createUserProvidedService(service, credentials, syslogDrainUrl));
    }

    @Override
    public void deleteAllApplications() {
        executeWithRetry(delegate::deleteAllApplications);
    }

    @Override
    public void deleteAllServices() {
        executeWithRetry(delegate::deleteAllServices);
    }

    @Override
    public void deleteQuota(String quotaName) {
        executeWithRetry(() -> delegate.deleteQuota(quotaName));
    }

    @Override
    public void deleteSecurityGroup(String securityGroupName) {
        executeWithRetry(() -> delegate.deleteSecurityGroup(securityGroupName));
    }

    @Override
    public void deleteServiceKey(String service, String serviceKey) {
        executeWithRetry(() -> delegate.deleteServiceKey(service, serviceKey));
    }

    @Override
    public void deleteSpace(String spaceName) {
        executeWithRetry(() -> delegate.deleteSpace(spaceName));
    }

    @Override
    public Map<String, String> getApplicationEnvironment(String applicationName) {
        return executeWithRetry(() -> delegate.getApplicationEnvironment(applicationName));
    }

    @Override
    public Map<String, String> getApplicationEnvironment(UUID appGuid) {
        return executeWithRetry(() -> delegate.getApplicationEnvironment(appGuid));
    }

    @Override
    public List<CloudEvent> getApplicationEvents(String applicationName) {
        return executeWithRetry(() -> delegate.getApplicationEvents(applicationName), HttpStatus.NOT_FOUND);
    }

    @Override
    public URL getCloudControllerUrl() {
        return executeWithRetry(delegate::getCloudControllerUrl);
    }

    @Override
    public CloudInfo getCloudInfo() {
        return executeWithRetry(delegate::getCloudInfo);
    }

    @Override
    public Map<String, String> getCrashLogs(String applicationName) {
        return executeWithRetry(() -> delegate.getCrashLogs(applicationName), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<CloudEvent> getEvents() {
        return executeWithRetry(delegate::getEvents, HttpStatus.NOT_FOUND);
    }

    @Override
    public String getFile(String applicationName, int instanceIndex, String filePath) {
        return executeWithRetry(() -> delegate.getFile(applicationName, instanceIndex, filePath));
    }

    @Override
    public String getFile(String applicationName, int instanceIndex, String filePath, int startPosition) {
        return executeWithRetry(() -> delegate.getFile(applicationName, instanceIndex, filePath, startPosition));
    }

    @Override
    public String getFile(String applicationName, int instanceIndex, String filePath, int startPosition, int endPosition) {
        return executeWithRetry(() -> delegate.getFile(applicationName, instanceIndex, filePath, startPosition, endPosition));
    }

    @Override
    public String getFileTail(String applicationName, int instanceIndex, String filePath, int length) {
        return executeWithRetry(() -> delegate.getFileTail(applicationName, instanceIndex, filePath, length));
    }

    @Override
    public Map<String, String> getLogs(String applicationName) {
        return executeWithRetry(() -> delegate.getLogs(applicationName), HttpStatus.NOT_FOUND);
    }

    @Override
    public Map<String, CloudUser> getOrganizationUsers(String organizationName) {
        return executeWithRetry(() -> delegate.getOrganizationUsers(organizationName), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<CloudOrganization> getOrganizations() {
        return executeWithRetry(delegate::getOrganizations, HttpStatus.NOT_FOUND);
    }

    @Override
    public CloudQuota getQuota(String quotaName) {
        return executeWithRetry(() -> delegate.getQuota(quotaName));
    }

    @Override
    public CloudQuota getQuota(String quotaName, boolean required) {
        return executeWithRetry(() -> delegate.getQuota(quotaName, required));
    }

    @Override
    public List<CloudQuota> getQuotas() {
        return executeWithRetry(delegate::getQuotas, HttpStatus.NOT_FOUND);
    }

    @Override
    public List<CloudSecurityGroup> getRunningSecurityGroups() {
        return executeWithRetry(delegate::getRunningSecurityGroups, HttpStatus.NOT_FOUND);
    }

    @Override
    public CloudSecurityGroup getSecurityGroup(String securityGroupName) {
        return executeWithRetry(() -> delegate.getSecurityGroup(securityGroupName));
    }

    @Override
    public CloudSecurityGroup getSecurityGroup(String securityGroupName, boolean required) {
        return executeWithRetry(() -> delegate.getSecurityGroup(securityGroupName, required));
    }

    @Override
    public List<CloudSecurityGroup> getSecurityGroups() {
        return executeWithRetry(delegate::getSecurityGroups, HttpStatus.NOT_FOUND);
    }

    @Override
    public List<CloudServiceKey> getServiceKeys(String serviceName) {
        return executeWithRetry(() -> delegate.getServiceKeys(serviceName), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<CloudServiceOffering> getServiceOfferings() {
        return executeWithRetry(delegate::getServiceOfferings, HttpStatus.NOT_FOUND);
    }

    @Override
    public List<UUID> getSpaceAuditors() {
        return executeWithRetry(() -> delegate.getSpaceAuditors(), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<UUID> getSpaceAuditors(String spaceName) {
        return executeWithRetry(() -> delegate.getSpaceAuditors(spaceName), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<UUID> getSpaceAuditors(String organizationName, String spaceName) {
        return executeWithRetry(() -> delegate.getSpaceAuditors(organizationName, spaceName), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<UUID> getSpaceAuditors(UUID spaceGuid) {
        return executeWithRetry(() -> delegate.getSpaceAuditors(spaceGuid), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<UUID> getSpaceDevelopers() {
        return executeWithRetry(() -> delegate.getSpaceDevelopers(), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<UUID> getSpaceDevelopers(String spaceName) {
        return executeWithRetry(() -> delegate.getSpaceDevelopers(spaceName), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<UUID> getSpaceDevelopers(String organizationName, String spaceName) {
        return executeWithRetry(() -> delegate.getSpaceDevelopers(organizationName, spaceName), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<UUID> getSpaceDevelopers(UUID spaceGuid) {
        return executeWithRetry(() -> delegate.getSpaceDevelopers(spaceGuid), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<UUID> getSpaceManagers() {
        return executeWithRetry(() -> delegate.getSpaceManagers(), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<UUID> getSpaceManagers(String spaceName) {
        return executeWithRetry(() -> delegate.getSpaceManagers(spaceName), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<UUID> getSpaceManagers(String organizationName, String spaceName) {
        return executeWithRetry(() -> delegate.getSpaceManagers(organizationName, spaceName), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<UUID> getSpaceManagers(UUID spaceGuid) {
        return executeWithRetry(() -> delegate.getSpaceManagers(spaceGuid), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<CloudSpace> getSpacesBoundToSecurityGroup(String securityGroupName) {
        return executeWithRetry(() -> delegate.getSpacesBoundToSecurityGroup(securityGroupName), HttpStatus.NOT_FOUND);
    }

    @Override
    public CloudStack getStack(String name) {
        return executeWithRetry(() -> delegate.getStack(name));
    }

    @Override
    public CloudStack getStack(String name, boolean required) {
        return executeWithRetry(() -> delegate.getStack(name, required));
    }

    @Override
    public List<CloudStack> getStacks() {
        return executeWithRetry(delegate::getStacks, HttpStatus.NOT_FOUND);
    }

    @Override
    public List<CloudSecurityGroup> getStagingSecurityGroups() {
        return executeWithRetry(delegate::getStagingSecurityGroups, HttpStatus.NOT_FOUND);
    }

    @Override
    public OAuth2AccessToken login() {
        return executeWithRetry(delegate::login);
    }

    @Override
    public void logout() {
        executeWithRetry(delegate::logout);
    }

    @Override
    public void register(String email, String password) {
        executeWithRetry(() -> delegate.register(email, password));
    }

    @Override
    public void registerRestLogListener(RestLogCallback callBack) {
        executeWithRetry(() -> delegate.registerRestLogListener(callBack));
    }

    @Override
    public void removeDomain(String domainName) {
        executeWithRetry(() -> delegate.removeDomain(domainName));
    }

    @Override
    public void setQuotaToOrganization(String organizationName, String quotaName) {
        executeWithRetry(() -> delegate.setQuotaToOrganization(organizationName, quotaName));
    }

    @Override
    public void setResponseErrorHandler(ResponseErrorHandler errorHandler) {
        executeWithRetry(() -> delegate.setResponseErrorHandler(errorHandler));
    }

    @Override
    public void unRegisterRestLogListener(RestLogCallback callBack) {
        executeWithRetry(() -> delegate.unRegisterRestLogListener(callBack));
    }

    @Override
    public void unbindRunningSecurityGroup(String securityGroupName) {
        executeWithRetry(() -> delegate.unbindRunningSecurityGroup(securityGroupName));
    }

    @Override
    public void unbindSecurityGroup(String organizationName, String spaceName, String securityGroupName) {
        executeWithRetry(() -> delegate.unbindSecurityGroup(organizationName, spaceName, securityGroupName));
    }

    @Override
    public void unbindStagingSecurityGroup(String securityGroupName) {
        executeWithRetry(() -> delegate.unbindStagingSecurityGroup(securityGroupName));
    }

    @Override
    public void unregister() {
        executeWithRetry(delegate::unregister);
    }

    @Override
    public void updatePassword(String newPassword) {
        executeWithRetry(() -> delegate.updatePassword(newPassword));
    }

    @Override
    public void updatePassword(CloudCredentials credentials, String newPassword) {
        executeWithRetry(() -> delegate.updatePassword(credentials, newPassword));
    }

    @Override
    public void updateQuota(CloudQuota quota, String name) {
        executeWithRetry(() -> delegate.updateQuota(quota, name));
    }

    @Override
    public void updateSecurityGroup(CloudSecurityGroup securityGroup) {
        executeWithRetry(() -> delegate.updateSecurityGroup(securityGroup));
    }

    @Override
    public void updateSecurityGroup(String name, InputStream jsonRulesFile) {
        executeWithRetry(() -> delegate.updateSecurityGroup(name, jsonRulesFile));
    }

    @Override
    public void uploadApplication(String applicationName, String file) {
        executeWithRetry(() -> {
            try {
                delegate.uploadApplication(applicationName, file);
            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        });
    }

    @Override
    public void uploadApplication(String applicationName, File file) {
        executeWithRetry(() -> {
            try {
                delegate.uploadApplication(applicationName, file);
            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        });
    }

    @Override
    public void uploadApplication(String applicationName, InputStream inputStream) {
        executeWithRetry(() -> {
            try {
                delegate.uploadApplication(applicationName, inputStream);
            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        });
    }

    @Override
    public UploadToken asyncUploadApplication(String applicationName, File file) {
        return executeWithRetry(() -> {
            try {
                return delegate.asyncUploadApplication(applicationName, file);
            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        });
    }

    @Override
    public CloudTask getTask(UUID taskGuid) {
        return executeWithRetry(() -> delegate.getTask(taskGuid));
    }

    @Override
    public List<CloudTask> getTasks(String applicationName) {
        return executeWithRetry(() -> delegate.getTasks(applicationName), HttpStatus.NOT_FOUND);
    }

    @Override
    public CloudTask runTask(String applicationName, CloudTask task) {
        return executeWithRetry(() -> delegate.runTask(applicationName, task));
    }

    @Override
    public CloudTask cancelTask(UUID taskGuid) {
        return executeWithRetry(() -> delegate.cancelTask(taskGuid));
    }

    @Override
    public CloudBuild createBuild(UUID packageGuid) {
        return executeWithRetry(() -> delegate.createBuild(packageGuid));
    }

    @Override
    public List<CloudBuild> getBuildsForPackage(UUID packageGuid) {
        return executeWithRetry(() -> delegate.getBuildsForPackage(packageGuid));
    }

    @Override
    public CloudBuild getBuild(UUID buildGuid) {
        return executeWithRetry(() -> delegate.getBuild(buildGuid));
    }

    @Override
    public void bindDropletToApp(UUID dropletGuid, UUID appGuid) {
        executeWithRetry(() -> delegate.bindDropletToApp(dropletGuid, appGuid));
    }

    @Override
    public List<CloudBuild> getBuildsForApplication(UUID applicationGuid) {
        return executeWithRetry(() -> delegate.getBuildsForApplication(applicationGuid));
    }

    @Override
    public void unbindService(String applicationName, String serviceName,
                              ApplicationServicesUpdateCallback applicationServicesUpdateCallback) {
        executeWithRetry(() -> delegate.unbindService(applicationName, serviceName, applicationServicesUpdateCallback));
    }

    @Override
    public List<CloudApplication> getApplicationsByMetadataLabelSelector(String labelSelector) {
        return executeWithRetry(() -> delegate.getApplicationsByMetadataLabelSelector(labelSelector));
    }

    @Override
    public List<CloudService> getServicesByMetadataLabelSelector(String labelSelector) {
        return executeWithRetry(() -> delegate.getServicesByMetadataLabelSelector(labelSelector));
    }

    @Override
    public void updateApplicationMetadata(UUID guid, Metadata metadata) {
        executeWithRetry(() -> delegate.updateApplicationMetadata(guid, metadata));
    }

    @Override
    public void updateServiceMetadata(UUID guid, Metadata metadata) {
        executeWithRetry(() -> delegate.updateServiceMetadata(guid, metadata));
    }

    private void executeWithRetry(Runnable operation, HttpStatus... statusesToIgnore) {
        executeWithRetry(() -> {
            operation.run();
            return null;
        }, statusesToIgnore);
    }

    private <T> T executeWithRetry(Supplier<T> operation, HttpStatus... statusesToIgnore) {
        ResilientCloudOperationExecutor executor = new ResilientCloudOperationExecutor().withStatusesToIgnore(statusesToIgnore);
        return executor.execute(operation);
    }
}
