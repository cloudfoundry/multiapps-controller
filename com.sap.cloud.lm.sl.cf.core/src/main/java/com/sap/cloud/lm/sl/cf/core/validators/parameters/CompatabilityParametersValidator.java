package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;

public abstract class CompatabilityParametersValidator<T> {

    protected final UserMessageLogger userMessageLogger;

    protected CompatabilityParametersValidator(UserMessageLogger userMessageLoger) {
        this.userMessageLogger = userMessageLoger;
    }

    public abstract T validate();

}
