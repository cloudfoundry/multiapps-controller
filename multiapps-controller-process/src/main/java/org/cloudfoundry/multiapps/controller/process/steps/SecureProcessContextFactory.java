package org.cloudfoundry.multiapps.controller.process.steps;

import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.process.security.store.SecretTokenStore;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.flowable.engine.delegate.DelegateExecution;
import org.immutables.value.Value;

@Value.Immutable
public interface SecureProcessContextFactory {

    DelegateExecution getDelegateExecution();

    StepLogger getStepLogger();

    CloudControllerClientProvider getClientProvider();

    SecretTokenStore getSecretTokenStore();

    default SecureProcessContext ofSecureProcessContext() {
        return new SecureProcessContext(getDelegateExecution(), getStepLogger(), getClientProvider(), getSecretTokenStore());
    }

}
