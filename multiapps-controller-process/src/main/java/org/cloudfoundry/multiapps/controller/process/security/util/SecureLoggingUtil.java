package org.cloudfoundry.multiapps.controller.process.security.util;

import java.util.Collection;

import org.cloudfoundry.multiapps.controller.core.security.serialization.DynamicSecureSerialization;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerializationFactory;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

public class SecureLoggingUtil {

    public static DynamicSecureSerialization getDynamicSecureSerialization(ProcessContext context) {
        Collection<String> parametersToHide = context.getVariable(Variables.SECURE_EXTENSION_DESCRIPTOR_PARAMETER_NAMES);
        return SecureSerializationFactory.ofAdditionalValues(parametersToHide);
    }

}
