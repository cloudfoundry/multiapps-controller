package com.sap.cloud.lm.sl.cf.web.configuration;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sap.cloud.lm.sl.cf.persistence.DataSourceWithDialect;
import com.sap.cloud.lm.sl.cf.persistence.services.FileSystemFileStorage;
import com.sap.cloud.lm.sl.cf.persistence.services.ObjectStoreFileStorage;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLoggerProvider;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLogsPersistenceService;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLogsPersister;
import com.sap.cloud.lm.sl.cf.persistence.services.ProgressMessageService;
import com.sap.cloud.lm.sl.cf.web.configuration.bean.factory.FileServiceFactoryBean;

@Configuration
public class ServicesConfiguration {

    @Autowired(required = false)
    private FileSystemFileStorage fileSystemFileStorage;
    @Autowired(required = false)
    private ObjectStoreFileStorage objectStoreFileStorage;

    @Bean
    public ProcessLoggerProvider processLoggerProvider() {
        return new ProcessLoggerProvider();
    }

    @Bean
    public ProcessLogsPersister processLogsPersister() {
        return new ProcessLogsPersister();
    }

    @Inject
    @Bean
    public ProcessLogsPersistenceService processLogsPersistenceService(DataSourceWithDialect dataSourceWithDialect) {
        return new ProcessLogsPersistenceService(dataSourceWithDialect, false);
    }

    @Inject
    @Bean
    public ProgressMessageService progressMessageService(DataSourceWithDialect dateSourceWithDialect) {
        return new ProgressMessageService(dateSourceWithDialect);
    }

    @Inject
    @Bean
    public FileServiceFactoryBean fileService(DataSourceWithDialect dataSourceWithDialect) {
        FileServiceFactoryBean fileServiceFactoryBean = new FileServiceFactoryBean();
        fileServiceFactoryBean.setDataSourceWithDialect(dataSourceWithDialect);
        fileServiceFactoryBean.setFileSystemFileStorage(fileSystemFileStorage);
        fileServiceFactoryBean.setObjectStoreFileStorage(objectStoreFileStorage);
        return fileServiceFactoryBean;
    }
}
