package org.cloudfoundry.multiapps.controller.process.metadata.parameters;

import java.text.MessageFormat;

import org.cloudfoundry.multiapps.controller.api.model.parameters.ParameterConverter;
import org.cloudfoundry.multiapps.controller.process.Messages;

public class NamespaceConverter implements ParameterConverter {

    private static final int MAX_NAMESPACE_LENGTH = 36;

    @Override
    public Object convert(Object value) {
        String namespace = String.valueOf(value);
        validate(namespace);
        return namespace;
    }

    private void validate(String namespace) {
        if (namespace.length() > MAX_NAMESPACE_LENGTH) {
            throw new IllegalArgumentException(MessageFormat.format(Messages.INVALID_NAMESPACE_LENGTH, MAX_NAMESPACE_LENGTH));
        }
    }
}
