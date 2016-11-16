package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceUrl;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("registerServiceUrlsStep")
public class RegisterServiceUrlsStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterServiceUrlsStep.class);

    public static StepMetadata getMetadata() {
        return new StepMetadata("registerServiceUrlsTask", "Register Service URLs", "Register Service URLs");
    }

    protected Function<DelegateExecution, ClientExtensions> extensionsSupplier = (context) -> getClientExtensions(context, LOGGER);

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {

        logActivitiTask(context, LOGGER);
        try {
            info(context, Messages.REGISTERING_SERVICE_URLS, LOGGER);

            ClientExtensions clientExtensionss = extensionsSupplier.apply(context);
            if (clientExtensionss == null) {
                warn(context, Messages.CLIENT_DOES_NOT_SUPPORT_EXTENSIONS, LOGGER);
                return ExecutionStatus.SUCCESS;
            }

            List<ServiceUrl> serviceUrlsToRegister = getServiceUrlsToRegister(StepsUtil.getAppsToDeploy(context), context);
            debug(context, format(Messages.SERVICE_URLS, JsonUtil.toJson(serviceUrlsToRegister, true)), LOGGER);

            for (ServiceUrl serviceUrl : serviceUrlsToRegister) {
                registerServiceUrl(context, serviceUrl, clientExtensionss);
            }

            StepsUtil.setServiceUrlsToRegister(context, serviceUrlsToRegister);
            debug(context, Messages.SERVICE_URLS_REGISTERED, LOGGER);
            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            error(context, Messages.ERROR_REGISTERING_SERVICE_URLS, e, LOGGER);
            throw e;
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            error(context, Messages.ERROR_REGISTERING_SERVICE_URLS, e, LOGGER);
            throw e;
        }
    }

    private List<ServiceUrl> getServiceUrlsToRegister(List<CloudApplicationExtended> appsToDeploy, DelegateExecution context)
        throws SLException {
        List<ServiceUrl> serviceUrlsToRegister = new ArrayList<>();
        for (CloudApplicationExtended app : appsToDeploy) {
            ServiceUrl serviceUrl = getServiceUrlToRegister(app, context);
            if (serviceUrl != null) {
                debug(context, format(Messages.CONSTRUCTED_SERVICE_URL_FROM_APPLICATION, serviceUrl.getServiceName(), app.getName()),
                    LOGGER);
                serviceUrlsToRegister.add(serviceUrl);
            }
        }
        return serviceUrlsToRegister;
    }

    private ServiceUrl getServiceUrlToRegister(CloudApplicationExtended app, DelegateExecution context) throws SLException {
        if (!StepsUtil.getAppAttribute(app, SupportedParameters.REGISTER_SERVICE_URL, false)) {
            return null;
        }

        String serviceName = StepsUtil.getAppAttribute(app, SupportedParameters.REGISTER_SERVICE_URL_SERVICE_NAME, app.getName());
        String url = StepsUtil.getAppAttribute(app, SupportedParameters.REGISTER_SERVICE_URL_SERVICE_URL, null);

        if (url == null) {
            throw new SLException(Messages.ERROR_NO_SERVICE_URL_SPECIFIED, serviceName, app.getName());
        }

        return new ServiceUrl(serviceName, url);
    }

    private void registerServiceUrl(DelegateExecution context, ServiceUrl serviceUrl, ClientExtensions clientExtensions) {
        try {
            info(context, format(Messages.REGISTERING_SERVICE_URL, serviceUrl.getUrl(), serviceUrl.getServiceName()), LOGGER);
            clientExtensions.registerServiceURL(serviceUrl.getServiceName(), serviceUrl.getUrl());
            debug(context, format(Messages.REGISTERED_SERVICE_URL, serviceUrl.getUrl(), serviceUrl.getServiceName()), LOGGER);
        } catch (CloudFoundryException e) {
            switch (e.getStatusCode()) {
                case FORBIDDEN:
                    warn(context, format(Messages.REGISTER_OF_SERVICE_URL_FAILED_403, serviceUrl.getUrl(), serviceUrl.getServiceName()),
                        LOGGER);
                    break;
                default:
                    throw e;
            }
        }
    }

}
