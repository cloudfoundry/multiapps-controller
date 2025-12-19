package org.cloudfoundry.multiapps.controller.process.security;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SecretTransformationStrategyImpl implements SecretTransformationStrategy {

    private Set<String> secretFields;

    public SecretTransformationStrategyImpl(Set<String> secretFields) {
        if (secretFields == null) {
            this.secretFields = Set.of();
        } else {
            this.secretFields = secretFields.stream()
                                            .filter(Objects::nonNull)
                                            .map(String::toLowerCase)
                                            .collect(Collectors.toSet());
        }
    }

    @Override
    public Set<String> getJsonSecretFieldNames() {
        return this.secretFields;
    }

}
