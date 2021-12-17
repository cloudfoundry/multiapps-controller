package org.cloudfoundry.multiapps.controller.core.security.serialization;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class SecureSerializerConfiguration {

    private static final Collection<String> DEFAULT_SENSITIVE_NAMES = List.of("user", "key", "auth", "credential", "secret", "token",
                                                                              "pass", "pwd", "certificate");

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

    public boolean isSensitive(String value) {
        return getSensitiveElementNames().stream()
                                         .anyMatch(name -> StringUtils.containsIgnoreCase(value, name));
    }

}
