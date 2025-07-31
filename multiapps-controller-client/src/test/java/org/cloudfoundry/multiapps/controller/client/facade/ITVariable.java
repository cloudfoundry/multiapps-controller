package org.cloudfoundry.multiapps.controller.client.facade;

public enum ITVariable {

  // @formatter:off
    CF_API(Constants.CF_API_ENV, Constants.CF_API_PROPERTY),
    USER_EMAIL(Constants.USER_EMAIL_ENV, Constants.USER_EMAIL_PROPERTY),
    USER_PASSWORD(Constants.USER_PASSWORD_ENV, Constants.USER_PASSWORD_PROPERTY),
    USER_ORIGIN(Constants.USER_ORIGIN_ENV, Constants.USER_ORIGIN_PROPERTY, false),
    ORG(Constants.ORG_ENV, Constants.ORG_PROPERTY),
    SPACE(Constants.SPACE_ENV, Constants.SPACE_PROPERTY),
    DOMAIN_NAME(Constants.DOMAIN_NAME_ENV, Constants.DOMAIN_NAME_PROPERTY),
    PATH_TO_SERVICE_BROKER_APPLICATION(Constants.PATH_TO_SERVICE_BROKER_ENV, Constants.PATH_TO_SERVICE_BROKER_PROPERTY, false);
  // @formatter:on

    private final String envVariable;
    private final String property;
    private final boolean required;

    ITVariable(String envVariable, String property, boolean required) {
        this.envVariable = envVariable;
        this.property = property;
        this.required = required;
    }

    ITVariable(String envVariable, String property) {
        this.envVariable = envVariable;
        this.property = property;
        this.required = true;
    }

    public String getEnvVariable() {
        return envVariable;
    }

    public String getProperty() {
        return property;
    }

    public boolean isRequired() {
        return required;
    }

    public String getValue() {
        String envVariable = System.getenv(this.getEnvVariable());
        String property = System.getProperty(this.getProperty());
        return property != null ? property : envVariable;
    }

    private static class Constants {
        public static final String CF_API_ENV = "CF_API";
        public static final String USER_EMAIL_ENV = "CF_USER_EMAIL";
        public static final String USER_PASSWORD_ENV = "CF_USER_PASSWORD";
        public static final String USER_ORIGIN_ENV = "CF_USER_ORIGIN";
        public static final String ORG_ENV = "CF_ORG";
        public static final String SPACE_ENV = "CF_SPACE";
        public static final String DOMAIN_NAME_ENV = "DOMAIN_NAME";
        public static final String PATH_TO_SERVICE_BROKER_ENV = "PATH_TO_SERVICE_BROKER";

        public static final String CF_API_PROPERTY = "api";
        public static final String USER_EMAIL_PROPERTY = "user.email";
        public static final String USER_PASSWORD_PROPERTY = "user.password";
        public static final String USER_ORIGIN_PROPERTY = "user.origin";
        public static final String ORG_PROPERTY = "org";
        public static final String SPACE_PROPERTY = "space";
        public static final String DOMAIN_NAME_PROPERTY = "domain.name";
        public static final String PATH_TO_SERVICE_BROKER_PROPERTY = "path.servicebroker";
    }
}
