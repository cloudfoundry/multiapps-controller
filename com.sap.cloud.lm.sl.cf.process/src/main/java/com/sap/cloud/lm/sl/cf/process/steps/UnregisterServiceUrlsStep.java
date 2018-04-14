package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;
import java.util.stream.Collectors;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceUrl;
import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationAttributesGetter;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("unregisterServiceUrlsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UnregisterServiceUrlsStep extends SyncActivitiStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws SLException {
        try {
            getStepLogger().info(Messages.UNREGISTERING_SERVICE_URLS);

            ClientExtensions clientExtensions = execution.getClientExtensions();
            if (clientExtensions == null) {
                getStepLogger().warn(Messages.CLIENT_DOES_NOT_SUPPORT_EXTENSIONS);
                return StepPhase.DONE;
            }
            List<String> serviceUrlToRegisterNames = getServiceNames(StepsUtil.getServiceUrlsToRegister(execution.getContext()));

            for (CloudApplication app : StepsUtil.getAppsToUndeploy(execution.getContext())) {
                unregisterServiceUrlIfNecessary(execution.getContext(), app, serviceUrlToRegisterNames, clientExtensions);
            }

            getStepLogger().debug(Messages.SERVICE_URLS_UNREGISTERED);
            return StepPhase.DONE;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_UNREGISTERING_SERVICE_URLS);
            throw e;
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            getStepLogger().error(e, Messages.ERROR_UNREGISTERING_SERVICE_URLS);
            throw e;
        }
    }

    private List<String> getServiceNames(List<ServiceUrl> serviceUrls) {
        return serviceUrls.stream()
            .map((serviceUrl) -> serviceUrl.getServiceName())
            .collect(Collectors.toList());
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
                getStepLogger().info(Messages.UNREGISTERING_SERVICE_URL, serviceName, app.getName());
                clientExtensions.unregisterServiceURL(serviceName);
                getStepLogger().debug(Messages.UNREGISTERED_SERVICE_URL, serviceName, app.getName());
            } catch (CloudFoundryException e) {
                switch (e.getStatusCode()) {
                    case FORBIDDEN:
                        if (shouldSucceed(context)) {
                            getStepLogger().warn(Messages.UNREGISTER_OF_SERVICE_URL_FAILED_403, serviceName);
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
