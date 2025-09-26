package org.cloudfoundry.multiapps.controller.web.configuration;

import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.service.impl.types.LongStringType;

public class CustomLongStringType extends LongStringType {

    private static final String CUSTOM_TYPE_NAME = "deployServiceLongString";

    public CustomLongStringType() {
        super(4000);
    }

    @Override
    public String getTypeName() {
        return CUSTOM_TYPE_NAME;
    }

    @Override
    public void setValue(Object value, ValueFields valueFields) {
        super.setValue(value, valueFields);
        valueFields.setCachedValue(value);
    }
}
