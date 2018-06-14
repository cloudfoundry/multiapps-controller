package com.sap.cloud.lm.sl.cf.core.helpers;

import javax.inject.Inject;

import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.persistence.services.AbstractFileService;

public class BeanProvider {

    private static final BeanProvider INSTANCE = new BeanProvider();

    @Inject
    private OperationDao operationDao;

    @Inject
    private CloudFoundryClientProvider clientProvider;

    @Inject
    private AbstractFileService fileService;

    private BeanProvider() {
    }

    public static BeanProvider getInstance() {
        return INSTANCE;
    }

    public OperationDao getOperationDao() {
        return operationDao;
    }

    public CloudFoundryClientProvider getCloudFoundryClientProvider() {
        return clientProvider;
    }

    public AbstractFileService getFileService() {
        return fileService;
    }

}
