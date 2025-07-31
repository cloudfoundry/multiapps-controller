package org.cloudfoundry.multiapps.controller.client.facade;

public interface ApplicationServicesUpdateCallback {

    void onError(CloudOperationException e, String applicationName, String serviceInstanceName);
}
