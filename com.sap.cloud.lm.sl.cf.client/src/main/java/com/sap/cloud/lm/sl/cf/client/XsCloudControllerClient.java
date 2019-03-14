package com.sap.cloud.lm.sl.cf.client;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.StartingInfo;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceOfferingExtended;

public interface XsCloudControllerClient extends CloudControllerClientSupportingCustomUserIds {

    void registerServiceURL(String serviceName, String serviceUrl);

    void unregisterServiceURL(String serviceName);

    void updateServiceParameters(String serviceName, Map<String, Object> parameters);

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
     * Update the service plan for an existing service.
     * 
     * @param serviceName the name of the service instance to update
     * @param planName the new service plan
     * @throws CloudOperationException if there was an error
     */
    void updateServicePlan(String serviceName, String planName);

}
