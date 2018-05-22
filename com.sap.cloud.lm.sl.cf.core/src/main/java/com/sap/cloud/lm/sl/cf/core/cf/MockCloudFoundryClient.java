package com.sap.cloud.lm.sl.cf.core.cf;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.ApplicationLogListener;
import org.cloudfoundry.client.lib.ClientHttpResponseCallback;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.RestLogCallback;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.StreamingLogToken;
import org.cloudfoundry.client.lib.UploadStatusCallback;
import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.ApplicationLog.MessageType;
import org.cloudfoundry.client.lib.domain.ApplicationStats;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.client.lib.domain.CloudApplication.DebugMode;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudEntity.Meta;
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
import org.cloudfoundry.client.lib.domain.CloudUser;
import org.cloudfoundry.client.lib.domain.CrashesInfo;
import org.cloudfoundry.client.lib.domain.InstanceState;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.cloudfoundry.client.lib.domain.ServiceKey;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.client.lib.domain.Upload;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.client.ResponseErrorHandler;

import com.sap.cloud.lm.sl.cf.client.util.TokenFactory;

public class MockCloudFoundryClient implements CloudFoundryOperations {

    private Map<String, CloudApplication> apps = new HashMap<String, CloudApplication>();
    private Map<String, Map<String, String>> appsEnv = new HashMap<String, Map<String, String>>();
    private CloudOrganization org = new CloudOrganization(createMeta(), "dummyOrg");

    @Override
    public void setResponseErrorHandler(ResponseErrorHandler errorHandler) {
    }

    @Override
    public URL getCloudControllerUrl() {
        return null;
    }

    @Override
    public CloudInfo getCloudInfo() {
        return null;
    }

    @Override
    public List<CloudSpace> getSpaces() {
        return null;
    }

    @Override
    public List<UUID> getSpaceManagers(String spaceName) {
        return null;
    }

    @Override
    public List<UUID> getSpaceDevelopers(String spaceName) {
        return null;
    }

    @Override
    public List<UUID> getSpaceAuditors(String spaceName) {
        return null;
    }

    @Override
    public List<UUID> getSpaceManagers(String orgName, String spaceName) {
        return Arrays.asList(TokenFactory.DUMMY_UUID);
    }

    @Override
    public List<UUID> getSpaceDevelopers(String orgName, String spaceName) {
        return Arrays.asList(TokenFactory.DUMMY_UUID);
    }

    @Override
    public List<UUID> getSpaceAuditors(String orgName, String spaceName) {
        return Arrays.asList(TokenFactory.DUMMY_UUID);
    }

    @Override
    public void associateAuditorWithSpace(String spaceName) {
    }

    @Override
    public void associateDeveloperWithSpace(String spaceName) {
    }

    @Override
    public void associateManagerWithSpace(String spaceName) {
    }

    @Override
    public void associateAuditorWithSpace(String orgName, String spaceName) {
    }

    @Override
    public void associateDeveloperWithSpace(String orgName, String spaceName) {
    }

    @Override
    public void associateManagerWithSpace(String orgName, String spaceName) {
    }

    @Override
    public void createSpace(String spaceName) {
    }

    @Override
    public CloudSpace getSpace(String spaceName) {
        return new CloudSpace(createMeta(), spaceName, org);
    }

    @Override
    public CloudSpace getSpace(String spaceName, boolean required) {
        return new CloudSpace(createMeta(), spaceName, org);
    }

    @Override
    public void deleteSpace(String spaceName) {
    }

    @Override
    public List<CloudOrganization> getOrganizations() {
        return null;
    }

    @Override
    public CloudOrganization getOrganization(String orgName) {
        return null;
    }

    @Override
    public CloudOrganization getOrganization(String orgName, boolean required) {
        return null;
    }

    @Override
    public void register(String email, String password) {
    }

    @Override
    public void updatePassword(String newPassword) {
    }

    @Override
    public void updatePassword(CloudCredentials credentials, String newPassword) {
    }

    @Override
    public void unregister() {
    }

    @Override
    public OAuth2AccessToken login() {
        return null;
    }

    @Override
    public void logout() {
    }

    @Override
    public List<CloudApplication> getApplications() {
        return new ArrayList<CloudApplication>(apps.values());
    }

    @Override
    public List<CloudApplication> getApplications(String inlineDepth) {
        return new ArrayList<CloudApplication>(apps.values());
    }

    @Override
    public CloudApplication getApplication(String appName) {
        return apps.get(appName);
    }

    @Override
    public CloudApplication getApplication(String appName, boolean required) {
        return apps.get(appName);
    }

    @Override
    public CloudApplication getApplication(UUID guid) {
        return null;
    }

    @Override
    public CloudApplication getApplication(UUID guid, boolean required) {
        return null;
    }

    @Override
    public ApplicationStats getApplicationStats(String appName) {
        return null;
    }

    @Override
    public Map<String, Object> getApplicationEnvironment(String appName) {
        return null;
    }

    @Override
    public Map<String, Object> getApplicationEnvironment(UUID appGuid) {
        return null;
    }

    @Override
    public void createApplication(String appName, Staging staging, Integer memory, List<String> uris, List<String> serviceNames) {
    }

    @Override
    public void createApplication(String appName, Staging staging, Integer disk, Integer memory, List<String> uris,
        List<String> serviceNames) {
        apps.put(appName, new CloudApplication(appName, null, null, memory, 1, uris, serviceNames, AppState.STOPPED));
    }

    @Override
    public void createService(CloudService service) {
        // Do nothing
    }

    @Override
    public void createUserProvidedService(CloudService service, Map<String, Object> credentials) {
        // Do nothing
    }

    @Override
    public void createUserProvidedService(CloudService service, Map<String, Object> credentials, String syslogDrainUrl) {
    }

    @Override
    public List<CloudRoute> deleteOrphanedRoutes() {
        return null;
    }

    @Override
    public void uploadApplication(String appName, String file) throws IOException {
    }

    @Override
    public void uploadApplication(String appName, File file) throws IOException {
    }

    @Override
    public void uploadApplication(String appName, File file, UploadStatusCallback callback) throws IOException {
    }

    @Override
    public void uploadApplication(String appName, InputStream inputStream) throws IOException {
    }

    @Override
    public void uploadApplication(String appName, InputStream inputStream, UploadStatusCallback callback) throws IOException {
    }

    @Override
    public void uploadApplication(String appName, ApplicationArchive archive) throws IOException {
    }

    @Override
    public void uploadApplication(String appName, ApplicationArchive archive, UploadStatusCallback callback) throws IOException {
    }

    @Override
    public String asyncUploadApplication(String appName, File file) throws IOException {
        return null;
    }

    @Override
    public String asyncUploadApplication(String appName, File file, UploadStatusCallback callback) throws IOException {
        return null;
    }

    @Override
    public String asyncUploadApplication(String appName, ApplicationArchive archive) throws IOException {
        return null;
    }

    @Override
    public String asyncUploadApplication(String appName, ApplicationArchive archive, UploadStatusCallback callback) throws IOException {
        return null;
    }

    @Override
    public Upload getUploadStatus(String uploadToken) {
        return null;
    }

    @Override
    public StartingInfo startApplication(String appName) {
        apps.get(appName)
            .setState(AppState.STARTED);
        return null;
    }

    @Override
    public void debugApplication(String appName, DebugMode mode) {
    }

    @Override
    public void stopApplication(String appName) {
    }

    @Override
    public StartingInfo restartApplication(String appName) {
        return null;
    }

    @Override
    public void deleteApplication(String appName) {
    }

    @Override
    public void deleteAllApplications() {
    }

    @Override
    public void deleteAllServices() {
    }

    @Override
    public void updateApplicationDiskQuota(String appName, int disk) {
    }

    @Override
    public void updateApplicationMemory(String appName, int memory) {
    }

    @Override
    public void updateApplicationInstances(String appName, int instances) {
        apps.get(appName)
            .setInstances(instances);
    }

    @Override
    public void updateApplicationServices(String appName, List<String> services) {
    }

    @Override
    public void updateApplicationStaging(String appName, Staging staging) {
    }

    @Override
    public void updateApplicationUris(String appName, List<String> uris) {
    }

    @Override
    public void updateApplicationEnv(String appName, Map<String, String> env) {
        appsEnv.put(appName, env);
    }

    @Override
    public void updateApplicationEnv(String appName, List<String> env) {
    }

    @Override
    public List<CloudEvent> getEvents() {
        return null;
    }

    @Override
    public List<CloudEvent> getApplicationEvents(String appName) {
        return null;
    }

    @Override
    public Map<String, String> getLogs(String appName) {
        return null;
    }

    @Override
    public StreamingLogToken streamLogs(String appName, ApplicationLogListener listener) {
        return null;
    }

    @Override
    public List<ApplicationLog> getRecentLogs(String appName) {
        return Arrays.asList(new ApplicationLog("0", "Deployment done", new Date(), MessageType.STDOUT, "App", "0"));
    }

    @Override
    public Map<String, String> getCrashLogs(String appName) {
        return null;
    }

    @Override
    public String getStagingLogs(StartingInfo info, int offset) {
        return null;
    }

    @Override
    public List<CloudStack> getStacks() {
        return null;
    }

    @Override
    public CloudStack getStack(String name) {
        return null;
    }

    @Override
    public CloudStack getStack(String name, boolean required) {
        return null;
    }

    @Override
    public String getFile(String appName, int instanceIndex, String filePath) {
        return null;
    }

    @Override
    public String getFile(String appName, int instanceIndex, String filePath, int startPosition) {
        return null;
    }

    @Override
    public String getFile(String appName, int instanceIndex, String filePath, int startPosition, int endPosition) {
        return null;
    }

    @Override
    public String getFileTail(String appName, int instanceIndex, String filePath, int length) {
        return null;
    }

    @Override
    public void openFile(String appName, int instanceIndex, String filePath, ClientHttpResponseCallback clientHttpResponseCallback) {
    }

    @Override
    public List<CloudService> getServices() {
        return Collections.emptyList();
    }

    @Override
    public CloudService getService(String service) {
        return null;
    }

    @Override
    public CloudService getService(String service, boolean required) {
        return null;
    }

    @Override
    public CloudServiceInstance getServiceInstance(String service) {
        return null;
    }

    @Override
    public CloudServiceInstance getServiceInstance(String service, boolean required) {
        return null;
    }

    @Override
    public void deleteService(String service) {
    }

    @Override
    public List<CloudServiceOffering> getServiceOfferings() {
        return null;
    }

    @Override
    public List<CloudServiceBroker> getServiceBrokers() {
        return null;
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name) {
        return null;
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name, boolean required) {
        return null;
    }

    @Override
    public void createServiceBroker(CloudServiceBroker serviceBroker) {
    }

    @Override
    public void updateServiceBroker(CloudServiceBroker serviceBroker) {
    }

    @Override
    public void deleteServiceBroker(String name) {
    }

    @Override
    public void updateServicePlanVisibilityForBroker(String name, boolean visibility) {
    }

    @Override
    public void bindService(String appName, String serviceName) {
    }

    @Override
    public void unbindService(String appName, String serviceName) {
    }

    @Override
    public InstancesInfo getApplicationInstances(String appName) {
        return null;
    }

    @Override
    public InstancesInfo getApplicationInstances(CloudApplication app) {
        if (app == null) {
            return null;
        }
        List<Map<String, Object>> attributes = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < apps.get(app.getName())
            .getInstances(); i++) {
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("state", InstanceState.RUNNING.toString());
            attributes.add(data);
        }
        return new InstancesInfo(attributes);
    }

    @Override
    public CrashesInfo getCrashes(String appName) {
        return null;
    }

    @Override
    public void rename(String appName, String newName) {
    }

    @Override
    public List<CloudDomain> getDomainsForOrg() {
        return Collections.emptyList();
    }

    @Override
    public List<CloudDomain> getPrivateDomains() {
        return null;
    }

    @Override
    public List<CloudDomain> getSharedDomains() {
        return null;
    }

    @Override
    public List<CloudDomain> getDomains() {
        return null;
    }

    @Override
    public CloudDomain getDefaultDomain() {
        return new CloudDomain(createMeta(), "mock", new CloudOrganization(createMeta(), "mockOrganization"));
    }

    private Meta createMeta() {
        return new Meta(new UUID(0, 0), null, null);
    }

    @Override
    public void addDomain(String domainName) {
        // Do nothing
    }

    @Override
    public void removeDomain(String domainName) {
    }

    @Override
    public void deleteDomain(String domainName) {
    }

    @Override
    public List<CloudRoute> getRoutes(String domainName) {
        return null;
    }

    @Override
    public void addRoute(String host, String domainName) {
    }

    @Override
    public void deleteRoute(String host, String domainName) {
    }

    @Override
    public void registerRestLogListener(RestLogCallback callBack) {
    }

    @Override
    public void unRegisterRestLogListener(RestLogCallback callBack) {
    }

    @Override
    public CloudQuota getQuota(String quotaName) {
        return null;
    }

    @Override
    public CloudQuota getQuota(String quotaName, boolean required) {
        return null;
    }

    @Override
    public void setQuotaToOrg(String orgName, String quotaName) {
    }

    @Override
    public void createQuota(CloudQuota quota) {
    }

    @Override
    public void deleteQuota(String quotaName) {
    }

    @Override
    public List<CloudQuota> getQuotas() {
        return null;
    }

    @Override
    public void updateQuota(CloudQuota quota, String name) {
    }

    @Override
    public List<CloudSecurityGroup> getSecurityGroups() {
        return null;
    }

    @Override
    public CloudSecurityGroup getSecurityGroup(String securityGroupName) {
        return null;
    }

    @Override
    public CloudSecurityGroup getSecurityGroup(String securityGroupName, boolean required) {
        return null;
    }

    @Override
    public void createSecurityGroup(CloudSecurityGroup securityGroup) {
    }

    @Override
    public void createSecurityGroup(String name, InputStream jsonRulesFile) {
    }

    @Override
    public void updateSecurityGroup(CloudSecurityGroup securityGroup) {
    }

    @Override
    public void updateSecurityGroup(String name, InputStream jsonRulesFile) {
    }

    @Override
    public void deleteSecurityGroup(String securityGroupName) {
    }

    @Override
    public List<CloudSecurityGroup> getStagingSecurityGroups() {
        return null;
    }

    @Override
    public void bindStagingSecurityGroup(String securityGroupName) {
    }

    @Override
    public void unbindStagingSecurityGroup(String securityGroupName) {
    }

    @Override
    public List<CloudSecurityGroup> getRunningSecurityGroups() {
        return null;
    }

    @Override
    public void bindRunningSecurityGroup(String securityGroupName) {
    }

    @Override
    public void unbindRunningSecurityGroup(String securityGroupName) {
    }

    @Override
    public List<CloudSpace> getSpacesBoundToSecurityGroup(String securityGroupName) {
        return null;
    }

    @Override
    public void bindSecurityGroup(String orgName, String spaceName, String securityGroupName) {
    }

    @Override
    public void unbindSecurityGroup(String orgName, String spaceName, String securityGroupName) {
    }

    @Override
    public void associateAuditorWithSpace(String orgName, String spaceName, String securityGroupName) {
    }

    @Override
    public void associateDeveloperWithSpace(String orgName, String spaceName, String securityGroupName) {
    }

    @Override
    public void associateManagerWithSpace(String orgName, String spaceName, String securityGroupName) {
    }

    @Override
    public Map<String, CloudUser> getOrganizationUsers(String arg0) {
        return null;
    }

    @Override
    public List<ServiceKey> getServiceKeys(String serviceName) {
        return null;
    }

}
