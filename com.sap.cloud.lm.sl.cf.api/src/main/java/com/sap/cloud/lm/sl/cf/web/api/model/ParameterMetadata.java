package com.sap.cloud.lm.sl.cf.web.api.model;

public class ParameterMetadata {

    private String id;
    private Object defaultValue;
    private Boolean required;

    public String getId() {
        return id;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public Boolean getRequired() {
        return required;
    }

    public static ParameterMetadataBuilder builder() {
        return new ParameterMetadataBuilder();
    }

    public static class ParameterMetadataBuilder {

        private String id;
        private Boolean required;
        private Object defaultValue;

        public ParameterMetadata build() {
            ParameterMetadata result = new ParameterMetadata();
            result.id = id;
            result.required = required != null ? required : false;
            result.defaultValue = defaultValue;
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
    }

}
