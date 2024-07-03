package org.cloudfoundry.multiapps.controller.process.metadata.parameters;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.api.model.parameters.ParameterConverter;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variable;

import java.text.MessageFormat;

public class ApplyNamespaceParameterConverter implements ParameterConverter {
    private final Variable<Boolean> applyNamespace;

    public ApplyNamespaceParameterConverter(Variable<Boolean> applyNamespace) {
        this.applyNamespace = applyNamespace;
    }

    @Override
    public Boolean convert(Object value) {
        try {
            return parseBoolean(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new ContentException(e, MessageFormat.format(Messages.NOT_BOOLEAN_PARAMETER_VALUE, value, applyNamespace.getName()));
        }
    }

    private boolean parseBoolean(String value) {
        if (value == null) {
            throw new IllegalArgumentException(Messages.PARSE_NULL_STRING_ERROR);
        }

        String lowerValue = value.trim()
                                 .toLowerCase();

        return switch (lowerValue) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException(Messages.INVALID_BOOLEAN_VALUE);
        };
    }
}
