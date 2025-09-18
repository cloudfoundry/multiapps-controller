package org.cloudfoundry.multiapps.controller.web.configuration.objectstore;

import java.util.Map;

import org.cloudfoundry.multiapps.controller.web.configuration.service.ObjectStoreServiceInfo;

public interface ObjectStoreProvider {

    public void connectToObjectStore();

    public ObjectStoreServiceInfo getObjectStoreServiceInfo(Map<String, Object> credentials);
}
