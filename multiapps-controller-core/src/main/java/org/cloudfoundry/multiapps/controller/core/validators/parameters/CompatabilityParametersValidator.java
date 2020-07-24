package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import org.cloudfoundry.multiapps.controller.core.util.UserMessageLogger;

public abstract class CompatabilityParametersValidator<T> {

    protected final UserMessageLogger userMessageLogger;

    protected CompatabilityParametersValidator(UserMessageLogger userMessageLoger) {
        this.userMessageLogger = userMessageLoger;
    }

    public abstract T validate();

}
