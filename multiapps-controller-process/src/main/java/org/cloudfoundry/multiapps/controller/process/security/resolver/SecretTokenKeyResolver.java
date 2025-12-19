package org.cloudfoundry.multiapps.controller.process.security.resolver;

import org.flowable.engine.delegate.DelegateExecution;

public interface SecretTokenKeyResolver {

    SecretTokenKeyContainer resolve(DelegateExecution execution);

}
