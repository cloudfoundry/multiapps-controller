package org.cloudfoundry.multiapps.controller.web.configuration;

import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.service.impl.types.LongStringType;

public class CustomLongStringType extends LongStringType {

    public CustomLongStringType(int minLength) {
        super(minLength);
    }

    @Override
    public void setValue(Object value, ValueFields valueFields) {
        super.setValue(value, valueFields);
        valueFields.setCachedValue(value);
    }
}
