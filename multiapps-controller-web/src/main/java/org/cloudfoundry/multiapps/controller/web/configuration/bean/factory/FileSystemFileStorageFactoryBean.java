package org.cloudfoundry.multiapps.controller.web.configuration.bean.factory;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.persistence.services.FileSystemFileStorage;
import org.cloudfoundry.multiapps.controller.persistence.util.EnvironmentServicesFinder;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import io.pivotal.cfenv.core.CfService;

public class FileSystemFileStorageFactoryBean implements FactoryBean<FileSystemFileStorage>, InitializingBean {

    private final String serviceName;
    private final EnvironmentServicesFinder environmentServicesFinder;
    private FileSystemFileStorage fileSystemFileStorage;

    public FileSystemFileStorageFactoryBean(String serviceName, EnvironmentServicesFinder environmentServicesFinder) {
        this.serviceName = serviceName;
        this.environmentServicesFinder = environmentServicesFinder;
    }

    @Override
    public void afterPropertiesSet() {
        String storagePath = getStoragePath(serviceName);
        if (storagePath == null) {
            return;
        }
        this.fileSystemFileStorage = new FileSystemFileStorage(storagePath);
    }

    @Override
    public Class<FileSystemFileStorage> getObjectType() {
        return FileSystemFileStorage.class;
    }

    @Override
    public FileSystemFileStorage getObject() {
        return fileSystemFileStorage;
    }

    private String getStoragePath(String serviceName) {
        CfService service = environmentServicesFinder.findService(serviceName);
        if (service == null) {
            return null;
        }
        return getStoragePath(service);
    }

    @SuppressWarnings("unchecked")
    private String getStoragePath(CfService service) {
        Map<String, Object> credentials = service.getCredentials()
                                                 .getMap();
        List<Object> volumeMounts = (List<Object>) credentials.get("volume_mounts");
        Map<String, Object> volumeMount = (Map<String, Object>) volumeMounts.get(0);
        return (String) volumeMount.get("container_dir");
    }

}
