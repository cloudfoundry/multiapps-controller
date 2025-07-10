package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import org.cloudfoundry.multiapps.controller.core.util.UserMessageLogger;

public abstract class CompatibilityParametersValidator<T> {

    protected final UserMessageLogger userMessageLogger;

    protected CompatibilityParametersValidator(UserMessageLogger userMessageLoger) {
        this.userMessageLogger = userMessageLoger;
    }

    public abstract T validate();

}
