package com.sap.cloud.lm.sl.cf.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.ApplicationLogListener;
import org.cloudfoundry.client.lib.ClientHttpResponseCallback;
import org.cloudfoundry.client.lib.CloudControllerClientImpl;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.RestLogCallback;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.StreamingLogToken;
import org.cloudfoundry.client.lib.UploadStatusCallback;
import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.ApplicationStats;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.DebugMode;
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
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.CloudStack;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.cloudfoundry.client.lib.domain.CloudUser;
import org.cloudfoundry.client.lib.domain.CrashesInfo;
import org.cloudfoundry.client.lib.domain.DockerInfo;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.cloudfoundry.client.lib.domain.ServiceKey;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.client.lib.domain.Upload;
import org.cloudfoundry.client.lib.domain.UploadToken;
import org.cloudfoundry.client.lib.rest.CloudControllerRestClient;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.client.ResponseErrorHandler;

import com.sap.cloud.lm.sl.cf.client.util.ExecutionRetrier;

public class ResilientCloudControllerClient implements CloudControllerClientSupportingCustomUserIds {

    private final ExecutionRetrier retrier = new ExecutionRetrier();
    private final CloudControllerClientImpl cc;

    public ResilientCloudControllerClient(CloudControllerRestClient cc) {
        this.cc = new CloudControllerClientImpl(cc);
    }

    @Override
    public List<String> getSpaceAuditorIdsAsStrings(String spaceName) {
        List<UUID> uuids = executeWithRetry(() -> cc.getSpaceAuditors(spaceName), HttpStatus.NOT_FOUND);
        return toStrings(uuids);
    }

    @Override
    public List<String> getSpaceManagerIdsAsStrings(String spaceName) {
        List<UUID> uuids = executeWithRetry(() -> cc.getSpaceManagers(spaceName), HttpStatus.NOT_FOUND);
        return toStrings(uuids);
    }

    @Override
    public List<String> getSpaceDeveloperIdsAsStrings(String spaceName) {
        List<UUID> uuids = executeWithRetry(() -> cc.getSpaceDevelopers(spaceName), HttpStatus.NOT_FOUND);
        return toStrings(uuids);
    }

    @Override
    public List<String> getSpaceAuditorIdsAsStrings(UUID spaceGuid) {
        List<UUID> uuids = executeWithRetry(() -> cc.getSpaceAuditors(spaceGuid), HttpStatus.NOT_FOUND);
        return toStrings(uuids);
    }

    @Override
    public List<String> getSpaceManagerIdsAsStrings(UUID spaceGuid) {
        List<UUID> uuids = executeWithRetry(() -> cc.getSpaceManagers(spaceGuid), HttpStatus.NOT_FOUND);
        return toStrings(uuids);
    }

    @Override
    public List<String> getSpaceDeveloperIdsAsStrings(UUID spaceGuid) {
        List<UUID> uuids = executeWithRetry(() -> cc.getSpaceDevelopers(spaceGuid), HttpStatus.NOT_FOUND);
        return toStrings(uuids);
    }

    @Override
    public List<String> getSpaceAuditorIdsAsStrings(String orgName, String spaceName) {
        List<UUID> uuids = executeWithRetry(() -> cc.getSpaceAuditors(orgName, spaceName), HttpStatus.NOT_FOUND);
        return toStrings(uuids);
    }

    @Override
    public List<String> getSpaceManagerIdsAsStrings(String orgName, String spaceName) {
        List<UUID> uuids = executeWithRetry(() -> cc.getSpaceManagers(orgName, spaceName), HttpStatus.NOT_FOUND);
        return toStrings(uuids);
    }

    @Override
    public List<String> getSpaceDeveloperIdsAsStrings(String orgName, String spaceName) {
        List<UUID> uuids = executeWithRetry(() -> cc.getSpaceDevelopers(orgName, spaceName), HttpStatus.NOT_FOUND);
        return toStrings(uuids);
    }

    @Override
    public void createService(CloudService service) {
        executeWithRetry(() -> cc.createService(service));
    }

    @Override
    public void addDomain(String domainName) {
        executeWithRetry(() -> cc.addDomain(domainName));
    }

    @Override
    public void addRoute(String host, String domainName) {
        executeWithRetry(() -> cc.addRoute(host, domainName));
    }

    @Override
    public void bindService(String appName, String serviceName) {
        executeWithRetry(() -> cc.bindService(appName, serviceName));
    }

    @Override
    public void createApplication(String appName, Staging staging, Integer disk, Integer memory, List<String> uris,
        List<String> serviceNames, DockerInfo dockerInfo) {
        executeWithRetry(() -> cc.createApplication(appName, staging, disk, memory, uris, serviceNames, dockerInfo));
    }

    @Override
    public void createServiceBroker(CloudServiceBroker serviceBroker) {
        executeWithRetry(() -> cc.createServiceBroker(serviceBroker));
    }

    @Override
    public void createUserProvidedService(CloudService service, Map<String, Object> credentials) {
        executeWithRetry(() -> cc.createUserProvidedService(service, credentials));
    }

    @Override
    public void deleteApplication(String appName) {
        executeWithRetry(() -> cc.deleteApplication(appName));
    }

    @Override
    public void deleteDomain(String domainName) {
        executeWithRetry(() -> cc.deleteDomain(domainName));
    }

    @Override
    public List<CloudRoute> deleteOrphanedRoutes() {
        return executeWithRetry(() -> cc.deleteOrphanedRoutes(), HttpStatus.NOT_FOUND);
    }

    @Override
    public void deleteRoute(String host, String domainName) {
        executeWithRetry(() -> cc.deleteRoute(host, domainName));
    }

    @Override
    public void deleteService(String service) {
        executeWithRetry(() -> cc.deleteService(service));
    }

    @Override
    public void deleteServiceBroker(String name) {
        executeWithRetry(() -> cc.deleteServiceBroker(name));
    }

    @Override
    public CloudApplication getApplication(String appName) {
        return executeWithRetry(() -> cc.getApplication(appName));
    }

    @Override
    public CloudApplication getApplication(String appName, boolean required) {
        return executeWithRetry(() -> cc.getApplication(appName, required));
    }

    @Override
    public CloudApplication getApplication(UUID appGuid) {
        return executeWithRetry(() -> cc.getApplication(appGuid));
    }

    @Override
    public CloudApplication getApplication(UUID appGuid, boolean required) {
        return executeWithRetry(() -> cc.getApplication(appGuid, required));
    }

    @Override
    public InstancesInfo getApplicationInstances(String appName) {
        return executeWithRetry(() -> cc.getApplicationInstances(appName));
    }

    @Override
    public InstancesInfo getApplicationInstances(CloudApplication app) {
        return executeWithRetry(() -> cc.getApplicationInstances(app));
    }

    @Override
    public List<CloudApplication> getApplications() {
        return executeWithRetry(() -> cc.getApplications(), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<CloudApplication> getApplications(String inlineDepth) {
        return executeWithRetry(() -> cc.getApplications(inlineDepth), HttpStatus.NOT_FOUND);
    }

    @Override
    public CloudDomain getDefaultDomain() {
        return executeWithRetry(() -> cc.getDefaultDomain());
    }

    @Override
    public List<CloudDomain> getDomains() {
        return executeWithRetry(() -> cc.getDomains(), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<CloudDomain> getDomainsForOrg() {
        return executeWithRetry(() -> cc.getDomainsForOrg(), HttpStatus.NOT_FOUND);
    }

    @Override
    public CloudOrganization getOrganization(String orgName) {
        return executeWithRetry(() -> cc.getOrganization(orgName));
    }

    @Override
    public CloudOrganization getOrganization(String orgName, boolean required) {
        return executeWithRetry(() -> cc.getOrganization(orgName, required));
    }

    @Override
    public List<CloudDomain> getPrivateDomains() {
        return executeWithRetry(() -> cc.getPrivateDomains(), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<ApplicationLog> getRecentLogs(String appName) {
        return executeWithRetry(() -> cc.getRecentLogs(appName), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<CloudRoute> getRoutes(String domainName) {
        return executeWithRetry(() -> cc.getRoutes(domainName), HttpStatus.NOT_FOUND);
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name) {
        return executeWithRetry(() -> cc.getServiceBroker(name));
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name, boolean required) {
        return executeWithRetry(() -> cc.getServiceBroker(name, required));

    }

    @Override
    public List<CloudServiceBroker> getServiceBrokers() {
        return executeWithRetry(() -> cc.getServiceBrokers(), HttpStatus.NOT_FOUND);
    }

    @Override
    public CloudServiceInstance getServiceInstance(String service) {
        return executeWithRetry(() -> cc.getServiceInstance(service));
    }

    @Override
    public CloudServiceInstance getServiceInstance(String service, boolean required) {
        return executeWithRetry(() -> cc.getServiceInstance(service, required));
    }

    @Override
    public List<CloudDomain> getSharedDomains() {
        return executeWithRetry(() -> cc.getSharedDomains(), HttpStatus.NOT_FOUND);
    }

    @Override
    public CloudSpace getSpace(String spaceName) {
        return executeWithRetry(() -> cc.getSpace(spaceName));
    }

    @Override
    public CloudSpace getSpace(String spaceName, boolean required) {
        return executeWithRetry(() -> cc.getSpace(spaceName, required));
    }

    @Override
    public List<CloudSpace> getSpaces() {
        return executeWithRetry(() -> cc.getSpaces(), HttpStatus.NOT_FOUND);
    }

    @Override
    public String getStagingLogs(StartingInfo info, int offset) {
        return executeWithRetry(() -> cc.getStagingLogs(info, offset), HttpStatus.NOT_FOUND);
    }

    @Override
    public void rename(String appName, String newName) {
        executeWithRetry(() -> cc.rename(appName, newName));
    }

    @Override
    public StartingInfo restartApplication(String appName) {
        return executeWithRetry(() -> cc.restartApplication(appName));
    }

    @Override
    public StartingInfo startApplication(String appName) {
        return executeWithRetry(() -> cc.startApplication(appName));
    }

    @Override
    public void stopApplication(String appName) {
        executeWithRetry(() -> cc.stopApplication(appName));
    }

    @Override
    public StreamingLogToken streamLogs(String appName, ApplicationLogListener listener) {
        return executeWithRetry(() -> cc.streamLogs(appName, listener), HttpStatus.NOT_FOUND);
    }

    @Override
    public void unbindService(String appName, String serviceName) {
        executeWithRetry(() -> cc.unbindService(appName, serviceName));
    }

    @Override
    public void updateApplicationDiskQuota(String appName, int disk) {
        executeWithRetry(() -> cc.updateApplicationDiskQuota(appName, disk));
    }

    @Override
    public void updateApplicationEnv(String appName, Map<String, String> env) {
        executeWithRetry(() -> cc.updateApplicationEnv(appName, env));
    }

    @Override
    public void updateApplicationEnv(String appName, List<String> env) {
        executeWithRetry(() -> cc.updateApplicationEnv(appName, env));
    }

    @Override
    public void updateApplicationInstances(String appName, int instances) {
        executeWithRetry(() -> cc.updateApplicationInstances(appName, instances));
    }

    @Override
    public void updateApplicationMemory(String appName, int memory) {
        executeWithRetry(() -> cc.updateApplicationMemory(appName, memory));
    }

    @Override
    public void updateApplicationServices(String appName, List<String> services) {
        executeWithRetry(() -> cc.updateApplicationServices(appName, services), HttpStatus.NOT_FOUND);
    }

    @Override
    public void updateApplicationStaging(String appName, Staging staging) {
        executeWithRetry(() -> cc.updateApplicationStaging(appName, staging));
    }

    @Override
    public void updateApplicationUris(String appName, List<String> uris) {
        executeWithRetry(() -> cc.updateApplicationUris(appName, uris), HttpStatus.NOT_FOUND);
    }

    @Override
    public void updateServiceBroker(CloudServiceBroker serviceBroker) {
        executeWithRetry(() -> cc.updateServiceBroker(serviceBroker));
    }

    @Override
    public void updateServicePlanVisibilityForBroker(String name, boolean visibility) {
        executeWithRetry(() -> cc.updateServicePlanVisibilityForBroker(name, visibility));
    }

    @Override
    public void uploadApplication(String appName, File file, UploadStatusCallback callback) throws IOException {
        executeWithRetry(() -> {
            try {
                cc.uploadApplication(appName, file, callback);
            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        });
    }

    @Override
    public void uploadApplication(String appName, InputStream inputStream, UploadStatusCallback callback) throws IOException {
        executeWithRetry(() -> {
            try {
                cc.uploadApplication(appName, inputStream, callback);
            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        });
    }

    @Override
    public void uploadApplication(String appName, ApplicationArchive archive, UploadStatusCallback callback) throws IOException {
        executeWithRetry(() -> {
            try {
                cc.uploadApplication(appName, archive, callback);
            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        });
    }

    @Override
    public UploadToken asyncUploadApplication(String appName, File file, UploadStatusCallback callback) throws IOException {
        return executeWithRetry(() -> {
            try {
                return cc.asyncUploadApplication(appName, file, callback);
            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        });
    }

    @Override
    public UploadToken asyncUploadApplication(String appName, ApplicationArchive archive, UploadStatusCallback callback) throws IOException {
        return executeWithRetry(() -> {
            try {
                return cc.asyncUploadApplication(appName, archive, callback);
            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        });
    }

    @Override
    public Upload getUploadStatus(String uploadToken) {
        return executeWithRetry(() -> cc.getUploadStatus(uploadToken));
    }

    @Override
    public CloudService getService(String service) {
        return executeWithRetry(() -> cc.getService(service));
    }

    @Override
    public CloudService getService(String service, boolean required) {
        return executeWithRetry(() -> cc.getService(service, required));
    }

    @Override
    public List<CloudService> getServices() {
        return executeWithRetry(() -> cc.getServices(), HttpStatus.NOT_FOUND);
    }

    @Override
    public void associateAuditorWithSpace(String spaceName) {
        executeWithRetry(() -> cc.associateAuditorWithSpace(spaceName));
    }

    @Override
    public void associateAuditorWithSpace(String orgName, String spaceName) {
        executeWithRetry(() -> cc.associateAuditorWithSpace(orgName, spaceName));
    }

    @Override
    public void associateAuditorWithSpace(String orgName, String spaceName, String userGuid) {
        executeWithRetry(() -> cc.associateAuditorWithSpace(orgName, spaceName, userGuid));
    }

    @Override
    public void associateDeveloperWithSpace(String spaceName) {
        executeWithRetry(() -> cc.associateDeveloperWithSpace(spaceName));
    }

    @Override
    public void associateDeveloperWithSpace(String orgName, String spaceName) {
        executeWithRetry(() -> cc.associateDeveloperWithSpace(orgName, spaceName));
    }

    @Override
    public void associateDeveloperWithSpace(String orgName, String spaceName, String userGuid) {
        executeWithRetry(() -> cc.associateDeveloperWithSpace(orgName, spaceName, userGuid));
    }

    @Override
    public void associateManagerWithSpace(String spaceName) {
        executeWithRetry(() -> cc.associateManagerWithSpace(spaceName));
    }

    @Override
    public void associateManagerWithSpace(String orgName, String spaceName) {
        executeWithRetry(() -> cc.associateManagerWithSpace(orgName, spaceName));
    }

    @Override
    public void associateManagerWithSpace(String orgName, String spaceName, String userGuid) {
        executeWithRetry(() -> cc.associateManagerWithSpace(orgName, spaceName, userGuid));
    }

    @Override
    public void bindRunningSecurityGroup(String securityGroupName) {
        executeWithRetry(() -> cc.bindRunningSecurityGroup(securityGroupName));
    }

    @Override
    public void bindSecurityGroup(String orgName, String spaceName, String securityGroupName) {
        executeWithRetry(() -> cc.bindSecurityGroup(orgName, spaceName, securityGroupName));
    }

    @Override
    public void bindStagingSecurityGroup(String securityGroupName) {
        executeWithRetry(() -> cc.bindStagingSecurityGroup(securityGroupName));
    }

    @Override
    public void createApplication(String appName, Staging staging, Integer memory, List<String> uris, List<String> serviceNames) {
        executeWithRetry(() -> cc.createApplication(appName, staging, memory, uris, serviceNames));
    }

    @Override
    public void createQuota(CloudQuota quota) {
        executeWithRetry(() -> cc.createQuota(quota));
    }

    @Override
    public void createSecurityGroup(CloudSecurityGroup securityGroup) {
        executeWithRetry(() -> cc.createSecurityGroup(securityGroup));
    }

    @Override
    public void createSecurityGroup(String name, InputStream jsonRulesFile) {
        executeWithRetry(() -> cc.createSecurityGroup(name, jsonRulesFile));
    }

    @Override
    public void createServiceKey(String serviceName, String serviceKeyName, Map<String, Object> parameters) {
        executeWithRetry(() -> cc.createServiceKey(serviceName, serviceKeyName, parameters));
    }

    @Override
    public void createSpace(String spaceName) {
        executeWithRetry(() -> cc.createSpace(spaceName));
    }

    @Override
    public void createUserProvidedService(CloudService service, Map<String, Object> credentials, String syslogDrainUrl) {
        executeWithRetry(() -> cc.createUserProvidedService(service, credentials, syslogDrainUrl));
    }

    @Override
    public void debugApplication(String appName, DebugMode mode) {
        executeWithRetry(() -> cc.debugApplication(appName, mode));
    }

    @Override
    public void deleteAllApplications() {
        executeWithRetry(() -> cc.deleteAllApplications());
    }

    @Override
    public void deleteAllServices() {
        executeWithRetry(() -> cc.deleteAllServices());
    }

    @Override
    public void deleteQuota(String quotaName) {
        executeWithRetry(() -> cc.deleteQuota(quotaName));
    }

    @Override
    public void deleteSecurityGroup(String securityGroupName) {
        executeWithRetry(() -> cc.deleteSecurityGroup(securityGroupName));
    }

    @Override
    public void deleteServiceKey(String service, String serviceKey) {
        executeWithRetry(() -> cc.deleteServiceKey(service, serviceKey));
    }

    @Override
    public void deleteSpace(String spaceName) {
        executeWithRetry(() -> cc.deleteSpace(spaceName));
    }

    @Override
    public Map<String, Object> getApplicationEnvironment(String appName) {
        return executeWithRetry(() -> cc.getApplicationEnvironment(appName));
    }

    @Override
    public Map<String, Object> getApplicationEnvironment(UUID appGuid) {
        return executeWithRetry(() -> cc.getApplicationEnvironment(appGuid));
    }

    @Override
    public List<CloudEvent> getApplicationEvents(String appName) {
        return executeWithRetry(() -> cc.getApplicationEvents(appName), HttpStatus.NOT_FOUND);
    }

    @Override
    public ApplicationStats getApplicationStats(String appName) {
        return executeWithRetry(() -> cc.getApplicationStats(appName));
    }

    @Override
    public URL getCloudControllerUrl() {
        return executeWithRetry(() -> cc.getCloudControllerUrl());
    }

    @Override
    public CloudInfo getCloudInfo() {
        return executeWithRetry(() -> cc.getCloudInfo());
    }

    @Override
    public Map<String, String> getCrashLogs(String appName) {
        return executeWithRetry(() -> cc.getCrashLogs(appName), HttpStatus.NOT_FOUND);
    }

    @Override
    public CrashesInfo getCrashes(String appName) {
        return executeWithRetry(() -> cc.getCrashes(appName), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<CloudEvent> getEvents() {
        return executeWithRetry(() -> cc.getEvents(), HttpStatus.NOT_FOUND);
    }

    @Override
    public String getFile(String appName, int instanceIndex, String filePath) {
        return executeWithRetry(() -> cc.getFile(appName, instanceIndex, filePath));
    }

    @Override
    public String getFile(String appName, int instanceIndex, String filePath, int startPosition) {
        return executeWithRetry(() -> cc.getFile(appName, instanceIndex, filePath, startPosition));
    }

    @Override
    public String getFile(String appName, int instanceIndex, String filePath, int startPosition, int endPosition) {
        return executeWithRetry(() -> cc.getFile(appName, instanceIndex, filePath, startPosition, endPosition));
    }

    @Override
    public String getFileTail(String appName, int instanceIndex, String filePath, int length) {
        return executeWithRetry(() -> cc.getFileTail(appName, instanceIndex, filePath, length));
    }

    @Override
    public Map<String, String> getLogs(String appName) {
        return executeWithRetry(() -> cc.getLogs(appName), HttpStatus.NOT_FOUND);
    }

    @Override
    public Map<String, CloudUser> getOrganizationUsers(String orgName) {
        return executeWithRetry(() -> cc.getOrganizationUsers(orgName), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<CloudOrganization> getOrganizations() {
        return executeWithRetry(() -> cc.getOrganizations(), HttpStatus.NOT_FOUND);
    }

    @Override
    public CloudQuota getQuota(String quotaName) {
        return executeWithRetry(() -> cc.getQuota(quotaName));
    }

    @Override
    public CloudQuota getQuota(String quotaName, boolean required) {
        return executeWithRetry(() -> cc.getQuota(quotaName, required));
    }

    @Override
    public List<CloudQuota> getQuotas() {
        return executeWithRetry(() -> cc.getQuotas(), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<CloudSecurityGroup> getRunningSecurityGroups() {
        return executeWithRetry(() -> cc.getRunningSecurityGroups(), HttpStatus.NOT_FOUND);
    }

    @Override
    public CloudSecurityGroup getSecurityGroup(String securityGroupName) {
        return executeWithRetry(() -> cc.getSecurityGroup(securityGroupName));
    }

    @Override
    public CloudSecurityGroup getSecurityGroup(String securityGroupName, boolean required) {
        return executeWithRetry(() -> cc.getSecurityGroup(securityGroupName, required));
    }

    @Override
    public List<CloudSecurityGroup> getSecurityGroups() {
        return executeWithRetry(() -> cc.getSecurityGroups(), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<ServiceKey> getServiceKeys(String serviceName) {
        return executeWithRetry(() -> cc.getServiceKeys(serviceName), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<CloudServiceOffering> getServiceOfferings() {
        return executeWithRetry(() -> cc.getServiceOfferings(), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<UUID> getSpaceAuditors(String spaceName) {
        return executeWithRetry(() -> cc.getSpaceAuditors(spaceName), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<UUID> getSpaceAuditors(UUID spaceGuid) {
        return executeWithRetry(() -> cc.getSpaceAuditors(spaceGuid), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<UUID> getSpaceAuditors(String orgName, String spaceName) {
        return executeWithRetry(() -> cc.getSpaceAuditors(orgName, spaceName), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<UUID> getSpaceDevelopers(String spaceName) {
        return executeWithRetry(() -> cc.getSpaceDevelopers(spaceName), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<UUID> getSpaceDevelopers(UUID spaceGuid) {
        return executeWithRetry(() -> cc.getSpaceDevelopers(spaceGuid), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<UUID> getSpaceDevelopers(String orgName, String spaceName) {
        return executeWithRetry(() -> cc.getSpaceDevelopers(orgName, spaceName), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<UUID> getSpaceManagers(String spaceName) {
        return executeWithRetry(() -> cc.getSpaceManagers(spaceName), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<UUID> getSpaceManagers(UUID spaceGuid) {
        return executeWithRetry(() -> cc.getSpaceManagers(spaceGuid), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<UUID> getSpaceManagers(String orgName, String spaceName) {
        return executeWithRetry(() -> cc.getSpaceManagers(orgName, spaceName), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<CloudSpace> getSpacesBoundToSecurityGroup(String securityGroupName) {
        return executeWithRetry(() -> cc.getSpacesBoundToSecurityGroup(securityGroupName), HttpStatus.NOT_FOUND);
    }

    @Override
    public CloudStack getStack(String name) {
        return executeWithRetry(() -> cc.getStack(name));
    }

    @Override
    public CloudStack getStack(String name, boolean required) {
        return executeWithRetry(() -> cc.getStack(name, required));
    }

    @Override
    public List<CloudStack> getStacks() {
        return executeWithRetry(() -> cc.getStacks(), HttpStatus.NOT_FOUND);
    }

    @Override
    public List<CloudSecurityGroup> getStagingSecurityGroups() {
        return executeWithRetry(() -> cc.getStagingSecurityGroups(), HttpStatus.NOT_FOUND);
    }

    @Override
    public OAuth2AccessToken login() {
        return executeWithRetry(() -> cc.login());
    }

    @Override
    public void logout() {
        executeWithRetry(() -> cc.logout());
    }

    @Override
    public void openFile(String appName, int instanceIndex, String filePath, ClientHttpResponseCallback clientHttpResponseCallback) {
        executeWithRetry(() -> cc.openFile(appName, instanceIndex, filePath, clientHttpResponseCallback));
    }

    @Override
    public void register(String email, String password) {
        executeWithRetry(() -> cc.register(email, password));
    }

    @Override
    public void registerRestLogListener(RestLogCallback callBack) {
        executeWithRetry(() -> cc.registerRestLogListener(callBack));
    }

    @Override
    public void removeDomain(String domainName) {
        executeWithRetry(() -> cc.removeDomain(domainName));
    }

    @Override
    public void setQuotaToOrg(String orgName, String quotaName) {
        executeWithRetry(() -> cc.setQuotaToOrg(orgName, quotaName));
    }

    @Override
    public void setResponseErrorHandler(ResponseErrorHandler errorHandler) {
        executeWithRetry(() -> cc.setResponseErrorHandler(errorHandler));
    }

    @Override
    public void unRegisterRestLogListener(RestLogCallback callBack) {
        executeWithRetry(() -> cc.unRegisterRestLogListener(callBack));
    }

    @Override
    public void unbindRunningSecurityGroup(String securityGroupName) {
        executeWithRetry(() -> cc.unbindRunningSecurityGroup(securityGroupName));
    }

    @Override
    public void unbindSecurityGroup(String orgName, String spaceName, String securityGroupName) {
        executeWithRetry(() -> cc.unbindSecurityGroup(orgName, spaceName, securityGroupName));
    }

    @Override
    public void unbindStagingSecurityGroup(String securityGroupName) {
        executeWithRetry(() -> cc.unbindStagingSecurityGroup(securityGroupName));
    }

    @Override
    public void unregister() {
        executeWithRetry(() -> cc.unregister());
    }

    @Override
    public void updatePassword(String newPassword) {
        executeWithRetry(() -> cc.updatePassword(newPassword));
    }

    @Override
    public void updatePassword(CloudCredentials credentials, String newPassword) {
        executeWithRetry(() -> cc.updatePassword(credentials, newPassword));
    }

    @Override
    public void updateQuota(CloudQuota quota, String name) {
        executeWithRetry(() -> cc.updateQuota(quota, name));
    }

    @Override
    public void updateSecurityGroup(CloudSecurityGroup securityGroup) {
        executeWithRetry(() -> cc.updateSecurityGroup(securityGroup));
    }

    @Override
    public void updateSecurityGroup(String name, InputStream jsonRulesFile) {
        executeWithRetry(() -> cc.updateSecurityGroup(name, jsonRulesFile));
    }

    @Override
    public void uploadApplication(String appName, String file) throws IOException {
        executeWithRetry(() -> {
            try {
                cc.uploadApplication(appName, file);
            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        });
    }

    @Override
    public void uploadApplication(String appName, File file) throws IOException {
        executeWithRetry(() -> {
            try {
                cc.uploadApplication(appName, file);
            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        });
    }

    @Override
    public void uploadApplication(String appName, InputStream inputStream) throws IOException {
        executeWithRetry(() -> {
            try {
                cc.uploadApplication(appName, inputStream);
            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        });
    }

    @Override
    public void uploadApplication(String appName, ApplicationArchive archive) throws IOException {
        executeWithRetry(() -> {
            try {
                cc.uploadApplication(appName, archive);
            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        });
    }

    @Override
    public UploadToken asyncUploadApplication(String appName, File file) throws IOException {
        return executeWithRetry(() -> {
            try {
                return cc.asyncUploadApplication(appName, file);
            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        });
    }

    @Override
    public UploadToken asyncUploadApplication(String appName, ApplicationArchive archive) throws IOException {
        return executeWithRetry(() -> {
            try {
                return cc.asyncUploadApplication(appName, archive);
            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        });
    }

    @Override
    public boolean areTasksSupported() {
        return executeWithRetry(() -> cc.areTasksSupported());
    }

    @Override
    public List<CloudTask> getTasks(String applicationName) {
        return executeWithRetry(() -> cc.getTasks(applicationName), HttpStatus.NOT_FOUND);
    }

    @Override
    public CloudTask runTask(String applicationName, CloudTask task) {
        return executeWithRetry(() -> cc.runTask(applicationName, task));
    }

    @Override
    public CloudTask cancelTask(UUID taskGuid) {
        return executeWithRetry(() -> cc.cancelTask(taskGuid));
    }

    private <T> T executeWithRetry(Supplier<T> supplier, HttpStatus... httpStatusesToIgnore) {
        return retrier.executeWithRetry(supplier, httpStatusesToIgnore);
    }

    private void executeWithRetry(Runnable runnable, HttpStatus... httpStatusesToIgnore) {
        retrier.executeWithRetry(runnable, httpStatusesToIgnore);
    }

    private List<String> toStrings(List<UUID> uuids) {
        return uuids.stream()
            .map(UUID::toString)
            .collect(Collectors.toList());
    }

    @Override
    public CloudBuild createBuild(UUID packageGuid) {
        return executeWithRetry(() -> cc.createBuild(packageGuid));
    }

    @Override
    public CloudBuild getBuild(UUID buildGuid) {
        return executeWithRetry(() -> cc.getBuild(buildGuid));
    }

    @Override
    public void bindDropletToApp(UUID dropletGuid,UUID appGuid) {
        executeWithRetry(() -> cc.bindDropletToApp(dropletGuid, appGuid));
    }
}
