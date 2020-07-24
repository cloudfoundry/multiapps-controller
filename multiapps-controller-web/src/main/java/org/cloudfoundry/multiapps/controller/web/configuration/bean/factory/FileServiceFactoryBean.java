package org.cloudfoundry.multiapps.controller.web.configuration.bean.factory;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.persistence.DataSourceWithDialect;
import org.cloudfoundry.multiapps.controller.persistence.services.DatabaseFileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorage;
import org.cloudfoundry.multiapps.controller.persistence.services.FileSystemFileStorage;
import org.cloudfoundry.multiapps.controller.persistence.services.ObjectStoreFileStorage;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

@Named("fileService")
public class FileServiceFactoryBean implements FactoryBean<FileService>, InitializingBean {

    @Inject
    private DataSourceWithDialect dataSourceWithDialect;
    @Autowired(required = false)
    private FileSystemFileStorage fileSystemFileStorage;
    @Autowired(required = false)
    private ObjectStoreFileStorage objectStoreFileStorage;
    private FileService fileService;

    @Override
    public void afterPropertiesSet() {
        FileStorage fileStorage = objectStoreFileStorage != null ? objectStoreFileStorage : fileSystemFileStorage;
        if (fileStorage != null) {
            this.fileService = new FileService(dataSourceWithDialect, fileStorage);
        } else {
            this.fileService = new DatabaseFileService(dataSourceWithDialect);
        }
    }

    @Override
    public FileService getObject() {
        return fileService;
    }

    @Override
    public Class<?> getObjectType() {
        return FileService.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
