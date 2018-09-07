package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.XsCloudControllerClient;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceUrl;
import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationAttributes;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("unregisterServiceUrlsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UnregisterServiceUrlsStep extends SyncActivitiStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        try {
            getStepLogger().debug(Messages.UNREGISTERING_SERVICE_URLS);

            XsCloudControllerClient xsClient = execution.getXsControllerClient();
            if (xsClient == null) {
                getStepLogger().debug(Messages.CLIENT_EXTENSIONS_ARE_NOT_SUPPORTED);
                return StepPhase.DONE;
            }
            List<String> serviceUrlToRegisterNames = getServiceNames(StepsUtil.getServiceUrlsToRegister(execution.getContext()));

            for (CloudApplication app : StepsUtil.getAppsToUndeploy(execution.getContext())) {
                unregisterServiceUrlIfNecessary(execution.getContext(), app, serviceUrlToRegisterNames, xsClient);
            }

            getStepLogger().debug(Messages.SERVICE_URLS_UNREGISTERED);
            return StepPhase.DONE;
        } catch (CloudOperationException coe) {
            CloudControllerException e = new CloudControllerException(coe);
            getStepLogger().error(e, Messages.ERROR_UNREGISTERING_SERVICE_URLS);
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_UNREGISTERING_SERVICE_URLS);
            throw e;
        }
    }

    private List<String> getServiceNames(List<ServiceUrl> serviceUrls) {
        return serviceUrls.stream()
            .map(ServiceUrl::getServiceName)
            .collect(Collectors.toList());
    }

    private void unregisterServiceUrlIfNecessary(DelegateExecution context, CloudApplication app, List<String> serviceUrlsToRegister,
        XsCloudControllerClient xsClient) {
        ApplicationAttributes appAttributes = ApplicationAttributes.fromApplication(app);
        if (!appAttributes.get(SupportedParameters.REGISTER_SERVICE_URL, Boolean.class, false)) {
            return;
        }
        String serviceName = appAttributes.get(SupportedParameters.REGISTER_SERVICE_URL_SERVICE_NAME, String.class);
        if (serviceName != null && !serviceUrlsToRegister.contains(serviceName)) {
            try {
                getStepLogger().info(Messages.UNREGISTERING_SERVICE_URL, serviceName, app.getName());
                xsClient.unregisterServiceURL(serviceName);
                getStepLogger().debug(Messages.UNREGISTERED_SERVICE_URL, serviceName, app.getName());
            } catch (CloudOperationException e) {
                switch (e.getStatusCode()) {
                    case FORBIDDEN:
                        if (shouldSucceed(context)) {
                            getStepLogger().warn(Messages.UNREGISTER_OF_SERVICE_URL_FAILED_403, serviceName);
                            return;
                        }
                        throw e;
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
