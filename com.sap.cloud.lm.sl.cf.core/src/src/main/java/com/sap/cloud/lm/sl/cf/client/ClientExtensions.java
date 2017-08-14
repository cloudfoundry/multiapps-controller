package com.sap.cloud.lm.sl.cf.client;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.StartingInfo;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudInfoExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceOfferingExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudTask;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceKey;
import com.sap.cloud.lm.sl.cf.client.lib.domain.UploadInfo;
import com.sap.cloud.lm.sl.cf.client.lib.domain.UploadStatusCallbackExtended;

public interface ClientExtensions {

    void registerServiceURL(String serviceName, String serviceUrl);

    void unregisterServiceURL(String serviceName);

    void updateServiceParameters(String serviceName, Map<String, Object> parameters);

    void bindService(String appName, String serviceName, Map<String, Object> parameters);

    void updateUserProvidedServiceCredentials(String serviceName, Map<String, Object> credentials);

    StartingInfo startApplication(String appName, boolean staging);

    StartingInfo stageApplication(String appName);

    void updateServiceTags(String serviceName, List<String> serviceTags);

    /**
     * Reserves a port for the specified domain
     * 
     * @param domain the domain to reserve a port on
     * @return the reserved port
     */
    int reservePort(String domain);

    /**
     * Asynchronously upload an application
     *
     * @param appName the application name
     * @param file the application archive or folder
     * @param callback a callback interface used to provide progress information or <tt>null</tt>
     * @return a token used to track the progress of the asynchronous upload
     * @throws java.io.IOException if there was an IO error accessing the archive or folder
     */
    String asynchUploadApplication(String appName, File file, UploadStatusCallbackExtended callback) throws IOException;

    /**
     * Get progress information about a file upload process triggered by
     * {@link #asynchUploadApplication(String, File, UploadStatusCallbackExtended)}.
     *
     * @param uploadToken the token returned by {@link #asynchUploadApplication(String, File, UploadStatusCallbackExtended)}
     * @return info object used to get the upload progress
     */
    UploadInfo getUploadProgress(String uploadToken);

    List<ServiceKey> getServiceKeys(String serviceName);

    ServiceKey createServiceKey(String serviceName, String serviceKey, String parameters);

    void deleteServiceKey(String serviceName, String serviceKey);

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
    List<CloudTask> getTasks(String appName) throws UnsupportedOperationException;

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
    CloudTask runTask(String appName, String taskName, String command) throws UnsupportedOperationException;

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
    CloudTask runTask(String appName, String taskName, String command, Map<String, String> environment)
        throws UnsupportedOperationException;

    /**
     * Cancel the given task. Tasks are not supported on all versions of the controller, so check
     * {@link CloudInfoExtended#hasTasksSupport()} before using this method.
     * 
     * @param taskId the GUID of the task to cancel
     * @return the cancelled task
     */
    CloudTask cancelTask(UUID taskId) throws UnsupportedOperationException;

}
