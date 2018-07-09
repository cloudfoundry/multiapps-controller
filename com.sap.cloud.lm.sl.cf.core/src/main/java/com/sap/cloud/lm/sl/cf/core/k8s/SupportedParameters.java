package com.sap.cloud.lm.sl.cf.core.k8s;

public class SupportedParameters {

    public static final String CONTAINER_IMAGE = "container-image";
    public static final String CONTAINER_PORT = "container-port";
    public static final String ROUTE = "route";
    public static final String DATA = "data";

    public static class ContainerImageCredentialsSchema {

        public static final String REGISTRY = "registry";
        public static final String USERNAME = "username";
        public static final String PASSWORD = "password";

    }

}
