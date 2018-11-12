package com.sap.cloud.lm.sl.cf.core.helpers;

import javax.inject.Inject;

import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.persistence.services.FileService;

public class BeanProvider {

    private static final BeanProvider INSTANCE = new BeanProvider();

    @Inject
    private OperationDao operationDao;

    @Inject
    private CloudControllerClientProvider clientProvider;

    @Inject
    private FileService fileService;

    private BeanProvider() {
    }

    public static BeanProvider getInstance() {
        return INSTANCE;
    }

    public OperationDao getOperationDao() {
        return operationDao;
    }

    public CloudControllerClientProvider getCloudFoundryClientProvider() {
        return clientProvider;
    }

    public FileService getFileService() {
        return fileService;
    }

}
