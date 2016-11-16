package com.sap.cloud.lm.sl.cf.client;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.StartingInfo;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceOfferingExtended;
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
     * @param uploadToken the token returned by
     *        {@link #asynchUploadApplication(String, File, UploadStatusCallbackExtended)}
     * @return info object used to get the upload progress
     */
    UploadInfo getUploadProgress(String uploadToken);

    List<ServiceKey> getServiceKeys(String serviceName);

    ServiceKey createServiceKey(String serviceName, String serviceKey, String parameters);

    void deleteServiceKey(String serviceName, String serviceKey);

    List<CloudServiceOfferingExtended> getExtendedServiceOfferings();

    void addRoute(String host, String domainName, String path);

    void deleteRoute(String host, String domainName, String path);

}
