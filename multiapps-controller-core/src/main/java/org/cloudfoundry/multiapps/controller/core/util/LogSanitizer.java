package org.cloudfoundry.multiapps.controller.core.util;

public final class LogSanitizer {

    private LogSanitizer() {
    }

    public static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\r", "\\r")
                    .replace("\n", "\\n")
                    .replace("\t", "\\t");
    }

    public static String sanitize(Object value) {
        return value == null ? null : sanitize(value.toString());
    }
}
