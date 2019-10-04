package com.sap.cloud.lm.sl.cf.web.api.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

// An enum is NOT used here intentionally. This allows other projects to extend this class if they want to add another process type.
public class ProcessType {

    private static final String DEPLOY_NAME = "DEPLOY";
    private static final String BLUE_GREEN_DEPLOY_NAME = "BLUE_GREEN_DEPLOY";
    private static final String UNDEPLOY_NAME = "UNDEPLOY";
    private static final String CTS_DEPLOY_NAME = "CTS_DEPLOY";

    public static final ProcessType DEPLOY = new ProcessType(DEPLOY_NAME);
    public static final ProcessType BLUE_GREEN_DEPLOY = new ProcessType(BLUE_GREEN_DEPLOY_NAME);
    public static final ProcessType UNDEPLOY = new ProcessType(UNDEPLOY_NAME);
    public static final ProcessType CTS_DEPLOY = new ProcessType(CTS_DEPLOY_NAME);

    private String name;

    public ProcessType() {
        // Default constructor required by jersey
    }

    protected ProcessType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null) {
            return false;
        }
        if (getClass() != object.getClass()) {
            return false;
        }
        ProcessType other = (ProcessType) object;
        return Objects.equals(name, other.name);
    }

    public static ProcessType fromString(String name) {
        switch (name) {
            case DEPLOY_NAME:
                return DEPLOY;
            case BLUE_GREEN_DEPLOY_NAME:
                return BLUE_GREEN_DEPLOY;
            case UNDEPLOY_NAME:
                return UNDEPLOY;
            case CTS_DEPLOY_NAME:
                return CTS_DEPLOY;
            default:
                throw new IllegalStateException("Illegal process type: " + name);
        }
    }

}
