package org.cloudfoundry.multiapps.controller.process.security;

import java.util.Set;

public interface SecretTransformationStrategy {

    Set<String> getJsonSecretFieldNames();

}
