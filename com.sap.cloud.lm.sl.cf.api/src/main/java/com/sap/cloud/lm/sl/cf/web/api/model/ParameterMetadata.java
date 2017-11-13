package com.sap.cloud.lm.sl.cf.web.api.model;

import com.sap.cloud.lm.sl.common.util.CommonUtil;

public class ParameterMetadata {

    private String id;
    private Object defaultValue;
    private Boolean required;
    private ParameterType type;

    public String getId() {
        return id;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public Boolean getRequired() {
        return required;
    }

    public ParameterType getType() {
        return type;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public void setType(ParameterType type) {
        this.type = type;
    }

    public static ParameterMetadataBuilder builder() {
        return new ParameterMetadataBuilder();
    }

    public static class ParameterMetadataBuilder {

        private String id;
        private Boolean required;
        private Object defaultValue;

        private ParameterType type;

        public ParameterMetadata build() {
            ParameterMetadata result = new ParameterMetadata();
            result.setId(id);
            result.setRequired(CommonUtil.getOrDefault(required, false));
            result.setDefaultValue(defaultValue);
            result.setType(type);
            return result;
        }

        public ParameterMetadataBuilder id(String id) {
            this.id = id;
            return this;
        }

        public ParameterMetadataBuilder required(boolean isRequired) {
            this.required = isRequired;
            return this;
        }

        public ParameterMetadataBuilder defaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public ParameterMetadataBuilder type(ParameterType type) {
            this.type = type;
            return this;
        }
    }

    public enum ParameterType {
        STRING, INTEGER, BOOLEAN, TABLE
    }

}
