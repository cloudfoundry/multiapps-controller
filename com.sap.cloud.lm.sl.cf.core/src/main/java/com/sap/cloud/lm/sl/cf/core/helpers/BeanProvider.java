package com.sap.cloud.lm.sl.cf.core.helpers;

import javax.inject.Inject;

import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.core.dao.OngoingOperationDao;
import com.sap.cloud.lm.sl.persistence.services.AbstractFileService;

public class BeanProvider {
    private static BeanProvider INSTANCE = new BeanProvider();

    @Inject
    private OngoingOperationDao ongoingOperationDao;

    @Inject
    private CloudFoundryClientProvider clientProvider;

    @Inject
    private AbstractFileService fileService;

    private BeanProvider() {
    }

    public static BeanProvider getInstance() {
        return INSTANCE;
    }

    public OngoingOperationDao getOngoingOperationDao() {
        return ongoingOperationDao;
    }

    public CloudFoundryClientProvider getCloudFoundryClientProvider() {
        return clientProvider;
    }

    public AbstractFileService getFileService() {
        return fileService;
    }
}
