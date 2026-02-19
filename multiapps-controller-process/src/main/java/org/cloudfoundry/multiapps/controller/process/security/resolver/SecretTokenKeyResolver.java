package org.cloudfoundry.multiapps.controller.process.security.resolver;

import org.flowable.engine.delegate.DelegateExecution;

public interface SecretTokenKeyResolver {

    String resolve(DelegateExecution execution);

}
