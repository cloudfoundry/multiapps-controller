package org.cloudfoundry.multiapps.controller.process.steps;

import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.process.security.store.SecretTokenStore;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.flowable.engine.delegate.DelegateExecution;
import org.immutables.value.Value;

@Value.Immutable
public abstract class SecureProcessContextFactory {

    abstract DelegateExecution getDelegateExecution();

    abstract StepLogger getStepLogger();

    abstract CloudControllerClientProvider getClientProvider();

    abstract SecretTokenStore getSecretTokenStore();

    public SecureProcessContext ofSecureProcessContext() {
        return new SecureProcessContext(getDelegateExecution(), getStepLogger(), getClientProvider(), getSecretTokenStore());
    }

}
