package com.sap.cloud.lm.sl.cf.web.configuration.bean.factory;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.sap.cloud.lm.sl.cf.persistence.DataSourceWithDialect;
import com.sap.cloud.lm.sl.cf.persistence.services.DatabaseFileService;
import com.sap.cloud.lm.sl.cf.persistence.services.FileService;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorage;
import com.sap.cloud.lm.sl.cf.persistence.services.FileSystemFileStorage;
import com.sap.cloud.lm.sl.cf.persistence.services.ObjectStoreFileStorage;

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
