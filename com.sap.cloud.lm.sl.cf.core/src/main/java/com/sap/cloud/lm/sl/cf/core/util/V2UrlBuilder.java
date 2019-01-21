package com.sap.cloud.lm.sl.cf.core.util;

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class V2UrlBuilder {

    private static final CharSequence V2_QUERY_PREFIX = "q=";
    
    private static String buildQueryString(String separator, Set<String> fields) {
        String queryValue = fields.stream()
            .map(e -> String.join("", e, ":{", e, "}"))
            .collect(Collectors.joining(separator));
        return String.join("", V2_QUERY_PREFIX, queryValue);
    }

    public static String build(Supplier<String> urlSupplier, String v2QuerySeparator, Set<String> fields) {
        return String.join("", urlSupplier.get(), buildQueryString(v2QuerySeparator, fields));
    }
    
}
