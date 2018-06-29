package com.sap.cloud.lm.sl.cf.core.k8s;

public class Labels {

    public static final String RELEASE = "release";
    public static final String RELEASE_VALUE = "{{ .Release.Name }}";
    public static final String APP = "app";
    public static final String RUN = "run";
    public static final String RUN_SUFFIX = "-pod";
    
}
