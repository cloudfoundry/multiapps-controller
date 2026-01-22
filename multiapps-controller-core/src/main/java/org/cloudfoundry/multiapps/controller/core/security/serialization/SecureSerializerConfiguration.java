package org.cloudfoundry.multiapps.controller.core.security.serialization;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

public class SecureSerializerConfiguration {

    private static final Collection<String> DEFAULT_SENSITIVE_NAMES = List.of("user", "key", "auth", "credential", "secret", "token",
                                                                              "pass", "pwd", "certificate");

    public static final Object SECURE_SERIALIZATION_MASK = "********";

    private boolean formattedOutput = true;
    private Collection<String> sensitiveElementNames = DEFAULT_SENSITIVE_NAMES;
    private Collection<String> sensitiveElementPaths = Collections.emptyList();

    private Collection<String> additionalSensitiveElementNames = Collections.emptyList();

    public Collection<String> getSensitiveElementNames() {
        if (CollectionUtils.isEmpty(additionalSensitiveElementNames)) {
            return sensitiveElementNames;
        }

        Set<String> mergedSensitiveElementNames = new HashSet<>(sensitiveElementNames);

        for (String additionalSensitiveElement : additionalSensitiveElementNames) {
            boolean isNotExistent = mergedSensitiveElementNames.stream()
                                                               .noneMatch(sensitiveElement -> sensitiveElement.equalsIgnoreCase(
                                                                   additionalSensitiveElement));
            if (isNotExistent) {
                mergedSensitiveElementNames.add(additionalSensitiveElement);
            }
        }

        return mergedSensitiveElementNames;
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

    public void setAdditionalSensitiveElementNames(Collection<String> additionalSensitiveElementNames) {
        this.additionalSensitiveElementNames = additionalSensitiveElementNames;
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
