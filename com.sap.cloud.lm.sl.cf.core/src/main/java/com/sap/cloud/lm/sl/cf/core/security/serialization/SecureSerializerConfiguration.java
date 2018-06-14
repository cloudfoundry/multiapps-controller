package com.sap.cloud.lm.sl.cf.core.security.serialization;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang.StringUtils;

public class SecureSerializerConfiguration {

    private static final Collection<String> DEFAULT_SENSITIVE_NAMES = Arrays.asList("pass", "pwd");

    public static final Object SECURE_SERIALIZATION_MASK = "********";

    private boolean formattedOutput = true;
    private Collection<String> sensitiveElementNames = DEFAULT_SENSITIVE_NAMES;
    private Collection<String> sensitiveElementPaths = Collections.emptyList();

    public Collection<String> getSensitiveElementNames() {
        return sensitiveElementNames;
    }

    public Collection<String> getSensitiveElementPaths() {
        return sensitiveElementPaths;
    }

    public boolean formattedOutputIsEnabled() {
        return formattedOutput;
    }

    public void setSensitiveElementNames(Collection<String> sensitiveElementNames) {
        this.sensitiveElementNames = sensitiveElementNames;
    }

    public void setSensitiveElementPaths(Collection<String> sensitiveElementPaths) {
        this.sensitiveElementPaths = sensitiveElementPaths;
    }

    public void setFormattedOutput(boolean formattedOutput) {
        this.formattedOutput = formattedOutput;
    }

    public boolean apply(String value) {
        return getSensitiveElementNames().stream()
            .anyMatch(name -> StringUtils.containsIgnoreCase(value, name));
    }

}
