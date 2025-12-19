package org.cloudfoundry.multiapps.controller.process.security;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SecretTransformationStrategyContextImpl implements SecretTransformationStrategy {

    private SecretTransformationStrategy secretTransformationStrategy;

    private Set<String> extraFieldNames;

    public SecretTransformationStrategyContextImpl(SecretTransformationStrategy secretTransformationStrategy, Set<String> extraFieldNames) {
        this.secretTransformationStrategy = secretTransformationStrategy;
        if (extraFieldNames == null) {
            this.extraFieldNames = Set.of();
        } else {
            this.extraFieldNames = extraFieldNames.stream()
                                                  .filter(Objects::nonNull)
                                                  .map(String::toLowerCase)
                                                  .collect(Collectors.toSet());
        }
    }

    @Override
    public Set<String> getJsonSecretFieldNames() {
        Set<String> out = new HashSet<>(secretTransformationStrategy.getJsonSecretFieldNames());
        out.addAll(extraFieldNames);
        return out;
    }

}
