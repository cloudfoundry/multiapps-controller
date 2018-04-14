package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.ArrayList;
import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceUrl;
import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationAttributesGetter;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Component("registerServiceUrlsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RegisterServiceUrlsStep extends SyncActivitiStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws SLException {
        try {
            getStepLogger().info(Messages.REGISTERING_SERVICE_URLS);

            ClientExtensions clientExtensions = execution.getClientExtensions();
            if (clientExtensions == null) {
                getStepLogger().warn(Messages.CLIENT_DOES_NOT_SUPPORT_EXTENSIONS);
                return StepPhase.DONE;
            }

            List<ServiceUrl> serviceUrlsToRegister = getServiceUrlsToRegister(StepsUtil.getAppsToDeploy(execution.getContext()));
            getStepLogger().debug(Messages.SERVICE_URLS, JsonUtil.toJson(serviceUrlsToRegister, true));

            for (ServiceUrl serviceUrl : serviceUrlsToRegister) {
                registerServiceUrl(execution.getContext(), serviceUrl, clientExtensions);
            }

            StepsUtil.setServiceUrlsToRegister(execution.getContext(), serviceUrlsToRegister);
            getStepLogger().debug(Messages.SERVICE_URLS_REGISTERED);
            return StepPhase.DONE;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_REGISTERING_SERVICE_URLS);
            throw e;
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            getStepLogger().error(e, Messages.ERROR_REGISTERING_SERVICE_URLS);
            throw e;
        }
    }

    private List<ServiceUrl> getServiceUrlsToRegister(List<CloudApplicationExtended> appsToDeploy) throws SLException {
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

    private ServiceUrl getServiceUrlToRegister(CloudApplicationExtended app) throws SLException {
        ApplicationAttributesGetter attributesGetter = ApplicationAttributesGetter.forApplication(app);
        if (!attributesGetter.getAttribute(SupportedParameters.REGISTER_SERVICE_URL, Boolean.class, false)) {
            return null;
        }

        String serviceName = attributesGetter.getAttribute(SupportedParameters.REGISTER_SERVICE_URL_SERVICE_NAME, String.class,
            app.getName());
        String url = attributesGetter.getAttribute(SupportedParameters.REGISTER_SERVICE_URL_SERVICE_URL, String.class);

        if (url == null) {
            throw new SLException(Messages.ERROR_NO_SERVICE_URL_SPECIFIED, serviceName, app.getName());
        }

        return new ServiceUrl(serviceName, url);
    }

    private void registerServiceUrl(DelegateExecution context, ServiceUrl serviceUrl, ClientExtensions clientExtensions) {
        try {
            getStepLogger().info(Messages.REGISTERING_SERVICE_URL, serviceUrl.getUrl(), serviceUrl.getServiceName());
            clientExtensions.registerServiceURL(serviceUrl.getServiceName(), serviceUrl.getUrl());
            getStepLogger().debug(Messages.REGISTERED_SERVICE_URL, serviceUrl.getUrl(), serviceUrl.getServiceName());
        } catch (CloudFoundryException e) {
            switch (e.getStatusCode()) {
                case FORBIDDEN:
                    if (shouldSucceed(context)) {
                        getStepLogger().warn(Messages.REGISTER_OF_SERVICE_URL_FAILED_403, serviceUrl.getUrl(), serviceUrl.getServiceName());
                        return;
                    }
                default:
                    throw e;
            }
        }
    }

    private boolean shouldSucceed(DelegateExecution context) {
        return (Boolean) context.getVariable(Constants.PARAM_NO_FAIL_ON_MISSING_PERMISSIONS);
    }

}
