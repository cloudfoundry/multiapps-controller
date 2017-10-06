package com.sap.cloud.lm.sl.cf.web.activiti.rest;

import org.activiti.rest.service.application.ActivitiRestServicesApplication;

public class CustomActivitiRestServicesApplication extends ActivitiRestServicesApplication {
    public CustomActivitiRestServicesApplication() {
        setRestAuthenticator(new CustomActivitiRestAuthenticator());
    }
}
