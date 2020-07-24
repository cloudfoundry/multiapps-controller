package org.cloudfoundry.multiapps.controller.core.http;

import java.text.MessageFormat;

import org.apache.http.client.config.CookieSpecs;

public enum CookieSpecification {

    BROWSER_COMPATIBILITY(CookieSpecs.BROWSER_COMPATIBILITY), NETSCAPE(CookieSpecs.NETSCAPE), STANDARD(
        CookieSpecs.STANDARD), STANDARD_STRICT(CookieSpecs.STANDARD_STRICT), BEST_MATCH(
            CookieSpecs.BEST_MATCH), DEFAULT(CookieSpecs.DEFAULT), IGNORE_COOKIES(CookieSpecs.IGNORE_COOKIES);

    private final String value;

    CookieSpecification(String value) {
        this.value = value;
    }

    public static CookieSpecification fromString(String value) {
        switch (value) {
            case CookieSpecs.BROWSER_COMPATIBILITY:
                return CookieSpecification.BROWSER_COMPATIBILITY;
            case CookieSpecs.NETSCAPE:
                return CookieSpecification.NETSCAPE;
            case CookieSpecs.STANDARD:
                return CookieSpecification.STANDARD;
            case CookieSpecs.STANDARD_STRICT:
                return CookieSpecification.STANDARD_STRICT;
            case CookieSpecs.BEST_MATCH:
                return CookieSpecification.BEST_MATCH;
            case CookieSpecs.DEFAULT:
                return CookieSpecification.DEFAULT;
            case CookieSpecs.IGNORE_COOKIES:
                return CookieSpecification.IGNORE_COOKIES;
            default:
                throw new IllegalStateException(MessageFormat.format("{0} is not a valid cookie specification.", value));
        }
    }

    @Override
    public String toString() {
        return value;
    }

}
