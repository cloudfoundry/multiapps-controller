package org.cloudfoundry.multiapps.controller.process.steps;

import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.process.security.SecretTransformationStrategy;
import org.cloudfoundry.multiapps.controller.process.security.store.SecretTokenStore;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.flowable.engine.delegate.DelegateExecution;

public final class SecureProcessContextFactory {

    private SecureProcessContextFactory() {

    }

    public static SecureProcessContext ofSecureProcessContext(DelegateExecution execution, StepLogger stepLogger,
                                                              CloudControllerClientProvider clientProvider,
                                                              SecretTokenStore secretTokenStore,
                                                              SecretTransformationStrategy secretTransformationStrategy) {
        return new SecureProcessContext(execution, stepLogger, clientProvider, secretTokenStore, secretTransformationStrategy);
    }

}
