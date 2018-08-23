package com.sap.cloud.lm.sl.cf.client;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.StartingInfo;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudInfoExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceOfferingExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudTask;

public interface XsCloudControllerClient extends CloudControllerClientSupportingCustomUserIds {

    void registerServiceURL(String serviceName, String serviceUrl);

    void unregisterServiceURL(String serviceName);

    void updateServiceParameters(String serviceName, Map<String, Object> parameters);

    void bindService(String appName, String serviceName, Map<String, Object> parameters);

    void updateUserProvidedServiceCredentials(String serviceName, Map<String, Object> credentials);

    StartingInfo startApplication(String appName, boolean staging);

    StartingInfo stageApplication(String appName);

    void updateServiceTags(String serviceName, List<String> serviceTags);

    int reserveTcpPort(String domain, boolean tcps);

    int reserveTcpPort(int port, String domain, boolean tcps);

    /**
     * Reserves a port for the specified domain
     * 
     * @param domain the domain to reserve a port on
     * @return the reserved port
     */
    int reservePort(String domain);

    List<CloudServiceOfferingExtended> getExtendedServiceOfferings();

    void addRoute(String host, String domainName, String path);

    void deleteRoute(String host, String domainName, String path);

    /**
     * Get the list of one-off tasks currently known for the given application. Tasks are not supported on all versions of the controller,
     * so check {@link CloudInfoExtended#hasTasksSupport()} before using this method.
     * 
     * @param appName the application to look for tasks
     * @return the list of known tasks
     * @throws UnsupportedOperationException if the targeted controller does not support tasks
     */
    List<CloudTask> getTasks(String appName);

    /**
     * Run a one-off task on an app. Tasks are not supported on all versions of the controller, so check
     * {@link CloudInfoExtended#hasTasksSupport()} before using this method.
     * 
     * @param appName the application to run the task on
     * @param taskName a name for the task to run
     * @param command the command to execute as task
     * @return the created task
     * @throws UnsupportedOperationException if the targeted controller does not support tasks
     */
    CloudTask runTask(String appName, String taskName, String command);

    /**
     * Run a one-off task on an app. Tasks are not supported on all versions of the controller, so check
     * {@link CloudInfoExtended#hasTasksSupport()} before using this method.
     * 
     * @param appName the application to run the task on
     * @param taskName a name for the task to run
     * @param command the command to execute as task
     * @param environment optional environment variables for the task (in addition to the app environment)
     * @return the created task
     * @throws UnsupportedOperationException if the targeted controller does not support tasks
     */
    CloudTask runTask(String appName, String taskName, String command, Map<String, String> environment);

    /**
     * Cancel the given task. Tasks are not supported on all versions of the controller, so check
     * {@link CloudInfoExtended#hasTasksSupport()} before using this method.
     * 
     * @param taskId the GUID of the task to cancel
     * @return the cancelled task
     */
    CloudTask cancelTask(UUID taskId);

    /**
     * Update the service plan for an existing service.
     * 
     * @param serviceName the name of the service instance to update
     * @param planName the new service plan
     * @throws CloudOperationException if there was an error
     */
    void updateServicePlan(String serviceName, String planName);

}
