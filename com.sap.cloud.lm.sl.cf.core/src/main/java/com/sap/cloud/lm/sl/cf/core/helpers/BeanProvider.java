package com.sap.cloud.lm.sl.cf.core.helpers;

import javax.inject.Inject;

import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.core.dao.OngoingOperationDao;
import com.sap.cloud.lm.sl.persistence.services.FileService;

public class BeanProvider {
    private static BeanProvider INSTANCE = new BeanProvider();

    @Inject
    private OngoingOperationDao ongoingOperationDao;

    @Inject
    private CloudFoundryClientProvider clientProvider;

    @Inject
    private FileService fileService;

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

    public FileService getFileService() {
        return fileService;
    }
}
