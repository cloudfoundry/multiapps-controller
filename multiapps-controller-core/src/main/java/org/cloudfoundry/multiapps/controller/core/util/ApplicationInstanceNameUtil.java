package org.cloudfoundry.multiapps.controller.core.util;

public class ApplicationInstanceNameUtil {

    private static final String APP_INSTANCE_TEMPLATE = "ds-%s/%d";

    private ApplicationInstanceNameUtil() {

    }

    public static String buildApplicationInstanceTemplate(ApplicationConfiguration applicationConfiguration) {
        String applicationId = applicationConfiguration.getApplicationGuid();
        Integer applicationInstanceIndex = applicationConfiguration.getApplicationInstanceIndex();
        return String.format(APP_INSTANCE_TEMPLATE, applicationId, applicationInstanceIndex);
    }
}
