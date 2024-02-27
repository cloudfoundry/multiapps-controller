package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.XsCloudControllerClient;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceUrl;
import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationAttributes;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Component("registerServiceUrlsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RegisterServiceUrlsStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        try {
            getStepLogger().debug(Messages.REGISTERING_SERVICE_URLS);

            XsCloudControllerClient xsClient = execution.getXsControllerClient();
            if (xsClient == null) {
                getStepLogger().debug(Messages.CLIENT_EXTENSIONS_ARE_NOT_SUPPORTED);
                return StepPhase.DONE;
            }

            List<ServiceUrl> serviceUrlsToRegister = getServiceUrlsToRegister(StepsUtil.getAppsToDeploy(execution.getContext()));
            getStepLogger().debug(Messages.SERVICE_URLS, JsonUtil.toJson(serviceUrlsToRegister, true));

            for (ServiceUrl serviceUrl : serviceUrlsToRegister) {
                registerServiceUrl(execution.getContext(), serviceUrl, xsClient);
            }

            StepsUtil.setServiceUrlsToRegister(execution.getContext(), serviceUrlsToRegister);
            getStepLogger().debug(Messages.SERVICE_URLS_REGISTERED);
            return StepPhase.DONE;
        } catch (CloudOperationException coe) {
            CloudControllerException e = new CloudControllerException(coe);
            getStepLogger().error(e, Messages.ERROR_REGISTERING_SERVICE_URLS);
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_REGISTERING_SERVICE_URLS);
            throw e;
        }
    }

    private List<ServiceUrl> getServiceUrlsToRegister(List<CloudApplicationExtended> appsToDeploy) {
        List<ServiceUrl> serviceUrlsToRegister = new ArrayList<>();
        for (CloudApplicationExtended app : appsToDeploy) {
            ServiceUrl serviceUrl = getServiceUrlToRegister(app);
            if (serviceUrl != null) {
                getStepLogger().debug(Messages.CONSTRUCTED_SERVICE_URL_FROM_APPLICATION, serviceUrl.getServiceName(), app.getName());
                serviceUrlsToRegister.add(serviceUrl);
            }
        }
        return serviceUrlsToRegister;
    }

    private ServiceUrl getServiceUrlToRegister(CloudApplicationExtended app) {
        ApplicationAttributes appAttributes = ApplicationAttributes.fromApplication(app);
        if (!appAttributes.get(SupportedParameters.REGISTER_SERVICE_URL, Boolean.class, false)) {
            return null;
        }

        String serviceName = appAttributes.get(SupportedParameters.REGISTER_SERVICE_URL_SERVICE_NAME, String.class, app.getName());
        String url = appAttributes.get(SupportedParameters.REGISTER_SERVICE_URL_SERVICE_URL, String.class);

        if (url == null) {
            throw new SLException(Messages.ERROR_NO_SERVICE_URL_SPECIFIED, serviceName, app.getName());
        }

        return new ServiceUrl(serviceName, url);
    }

    private void registerServiceUrl(DelegateExecution context, ServiceUrl serviceUrl, XsCloudControllerClient xsClient) {
        try {
            getStepLogger().info(Messages.REGISTERING_SERVICE_URL, serviceUrl.getUrl(), serviceUrl.getServiceName());
            xsClient.registerServiceURL(serviceUrl.getServiceName(), serviceUrl.getUrl());
            getStepLogger().debug(Messages.REGISTERED_SERVICE_URL, serviceUrl.getUrl(), serviceUrl.getServiceName());
        } catch (CloudOperationException e) {
            switch (e.getStatusCode()) {
                case FORBIDDEN:
                    if (shouldSucceed(context)) {
                        getStepLogger().warn(Messages.REGISTER_OF_SERVICE_URL_FAILED_403, serviceUrl.getUrl(), serviceUrl.getServiceName());
                        return;
                    }
                    throw e;
                default:
                    throw e;
            }
        }
    }

    private boolean shouldSucceed(DelegateExecution context) {
        return (Boolean) context.getVariable(Constants.PARAM_NO_FAIL_ON_MISSING_PERMISSIONS);
    }

}
