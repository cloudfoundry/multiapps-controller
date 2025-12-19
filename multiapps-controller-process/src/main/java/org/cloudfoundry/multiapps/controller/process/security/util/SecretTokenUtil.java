package org.cloudfoundry.multiapps.controller.process.security.util;

public class SecretTokenUtil {

    public static final String ENCRYPTED_VALUES_PREFIX = "dsc:v1:";

    private SecretTokenUtil() {

    }

    public static boolean isToken(String token) {
        if (token == null) {
            return false;
        }

        if (!token.startsWith(ENCRYPTED_VALUES_PREFIX)) {
            return false;
        }

        String tail = token.substring(ENCRYPTED_VALUES_PREFIX.length());

        if (tail.isBlank()) {
            return false;
        }

        for (int i = 0; i < tail.length(); i++) {
            if (!Character.isDigit(tail.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static long id(String token) {
        return Long.parseLong(token.substring(ENCRYPTED_VALUES_PREFIX.length()));
    }

    public static String of(long id) {
        return ENCRYPTED_VALUES_PREFIX + id;
    }

}
