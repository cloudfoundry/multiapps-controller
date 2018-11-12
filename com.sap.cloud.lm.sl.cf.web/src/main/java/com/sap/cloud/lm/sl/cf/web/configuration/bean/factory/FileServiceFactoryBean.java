package com.sap.cloud.lm.sl.cf.web.configuration.bean.factory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import com.sap.cloud.lm.sl.cf.persistence.DataSourceWithDialect;
import com.sap.cloud.lm.sl.cf.persistence.services.DatabaseFileService;
import com.sap.cloud.lm.sl.cf.persistence.services.FileService;
import com.sap.cloud.lm.sl.cf.persistence.services.FileSystemFileStorage;

public class FileServiceFactoryBean implements FactoryBean<FileService>, InitializingBean {

    private DataSourceWithDialect dataSourceWithDialect;
    private FileSystemFileStorage fileSystemFileStorage;
    private FileService fileService;

    public void setDataSourceWithDialect(DataSourceWithDialect dataSourceWithDialect) {
        this.dataSourceWithDialect = dataSourceWithDialect;
    }

    public void setFileSystemFileStorage(FileSystemFileStorage fileSystemFileStorage) {
        this.fileSystemFileStorage = fileSystemFileStorage;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (fileSystemFileStorage != null) {
            this.fileService = new FileService(dataSourceWithDialect, fileSystemFileStorage);
        } else {
            this.fileService = new DatabaseFileService(dataSourceWithDialect);
        }
    }

    @Override
    public FileService getObject() throws Exception {
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
