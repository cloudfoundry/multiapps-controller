package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.List;
import java.util.stream.Collectors;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceUrl;
import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationAttributesGetter;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("unregisterServiceUrlsStep")
public class UnregisterServiceUrlsStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnregisterServiceUrlsStep.class);

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("unregisterServiceUrlsTask").displayName("Unregister Service URLs").description(
            "Unregister Service URLs").build();
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {

        logActivitiTask(context, LOGGER);
        try {
            info(context, Messages.UNREGISTERING_SERVICE_URLS, LOGGER);

            ClientExtensions clientExtensions = getClientExtensions(context, LOGGER);
            if (clientExtensions == null) {
                warn(context, Messages.CLIENT_DOES_NOT_SUPPORT_EXTENSIONS, LOGGER);
                return ExecutionStatus.SUCCESS;
            }
            List<String> serviceUrlToRegisterNames = getServiceNames(StepsUtil.getServiceUrlsToRegister(context));

            for (CloudApplication app : StepsUtil.getAppsToUndeploy(context)) {
                unregisterServiceUrlIfNecessary(context, app, serviceUrlToRegisterNames, clientExtensions);
            }

            debug(context, Messages.SERVICE_URLS_UNREGISTERED, LOGGER);
            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            error(context, Messages.ERROR_UNREGISTERING_SERVICE_URLS, e, LOGGER);
            throw e;
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            error(context, Messages.ERROR_UNREGISTERING_SERVICE_URLS, e, LOGGER);
            throw e;
        }
    }

    private List<String> getServiceNames(List<ServiceUrl> serviceUrls) {
        return serviceUrls.stream().map((serviceUrl) -> serviceUrl.getServiceName()).collect(Collectors.toList());
    }

    private void unregisterServiceUrlIfNecessary(DelegateExecution context, CloudApplication app, List<String> serviceUrlsToRegister,
        ClientExtensions clientExtensions) throws SLException {
        ApplicationAttributesGetter attributesGetter = ApplicationAttributesGetter.forApplication(app);
        if (!attributesGetter.getAttribute(SupportedParameters.REGISTER_SERVICE_URL, Boolean.class, false)) {
            return;
        }
        String serviceName = attributesGetter.getAttribute(SupportedParameters.REGISTER_SERVICE_URL_SERVICE_NAME, String.class);
        if (serviceName != null && !serviceUrlsToRegister.contains(serviceName)) {
            try {
                info(context, format(Messages.UNREGISTERING_SERVICE_URL, serviceName, app.getName()), LOGGER);
                clientExtensions.unregisterServiceURL(serviceName);
                debug(context, format(Messages.UNREGISTERED_SERVICE_URL, serviceName, app.getName()), LOGGER);
            } catch (CloudFoundryException e) {
                switch (e.getStatusCode()) {
                    case FORBIDDEN:
                        if (shouldSucceed(context)) {
                            warn(context, format(Messages.UNREGISTER_OF_SERVICE_URL_FAILED_403, serviceName), LOGGER);
                            return;
                        }
                    default:
                        throw e;
                }
            }
        }
    }

    private boolean shouldSucceed(DelegateExecution context) {
        return (Boolean) context.getVariable(Constants.PARAM_NO_FAIL_ON_MISSING_PERMISSIONS);
    }

}
