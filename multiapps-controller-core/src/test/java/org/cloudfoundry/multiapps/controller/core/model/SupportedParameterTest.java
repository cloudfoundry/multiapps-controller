package org.cloudfoundry.multiapps.controller.core.model;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SupportedParameterTest {

    private static final Set<String> GENERAL_PARAMETERS = Set.of(SupportedParameters.XS_TARGET_API_URL,
                                                                 SupportedParameters.APPLY_NAMESPACE_SERVICES,
                                                                 SupportedParameters.XS_AUTHORIZATION_ENDPOINT,
                                                                 SupportedParameters.APPLY_NAMESPACE_AS_SUFFIX,
                                                                 SupportedParameters.DEPLOY_TARGET,
                                                                 SupportedParameters.APPLY_NAMESPACE_APPS,
                                                                 SupportedParameters.APPLY_NAMESPACE_ROUTES,
                                                                 SupportedParameters.DEPLOY_SERVICE_URL, SupportedParameters.NAMESPACE,
                                                                 SupportedParameters.MTA_VERSION);

    private static final Set<String> CONFIGURATION_REFERENCE_PARAMETERS = Set.of(SupportedParameters.PROVIDER_NAMESPACE,
                                                                                 SupportedParameters.DEPRECATED_CONFIG_MTA_PROVIDES_DEPENDENCY,
                                                                                 SupportedParameters.MTA_ID,
                                                                                 SupportedParameters.DEPRECATED_CONFIG_MTA_MODULE);

    private static final Set<String> NESTED_PARAMETERS = Set.of(SupportedParameters.IDLE_ROUTE, SupportedParameters.VCAP_APPLICATION_ENV,
                                                                SupportedParameters.VCAP_SERVICES_ENV,
                                                                SupportedParameters.USER_PROVIDED_ENV, SupportedParameters.NO_HOSTNAME);

    @Test
    public void allFieldsShouldBeWhitelisted() throws IllegalAccessException {
        Set<String> supportedParameters = getSupportedParameters();
        Collection<String> strings = discoverAllParameterConstants();

        Set<String> missing = strings.stream()
                                     .filter(f -> !supportedParameters.contains(f))
                                     .collect(Collectors.toSet());

        assertTrue(missing.isEmpty(),
                   () -> "The following parameters are defined, but are not added to any structure: " + missing +
                       ".\nTo resolve this, add the missing parameters to the appropriate collection in the SupportedParameters class.");
    }

    private Set<String> getSupportedParameters() {
        Set<String> supportedParameters = new HashSet<>();
        fillWithSupportedParameters(supportedParameters);
        fillWithNonEntitySpecificSupportedParameters(supportedParameters);
        return supportedParameters;
    }

    private void fillWithSupportedParameters(Set<String> supportedParameters) {
        supportedParameters.addAll(SupportedParameters.MODULE_PARAMETERS);
        supportedParameters.addAll(SupportedParameters.RESOURCE_PARAMETERS);
        supportedParameters.addAll(SupportedParameters.GLOBAL_PARAMETERS);
        supportedParameters.addAll(SupportedParameters.DEPENDENCY_PARAMETERS);
        supportedParameters.addAll(SupportedParameters.MODULE_HOOK_PARAMETERS);
        supportedParameters.addAll(SupportedParameters.READ_ONLY_MODULE_PARAMETERS);
        supportedParameters.addAll(SupportedParameters.READ_ONLY_SYSTEM_PARAMETERS);
        supportedParameters.addAll(SupportedParameters.READ_ONLY_RESOURCE_PARAMETERS);
        supportedParameters.addAll(SupportedParameters.DYNAMIC_RESOLVABLE_PARAMETERS);
    }

    private void fillWithNonEntitySpecificSupportedParameters(Set<String> supportedParameters) {
        supportedParameters.addAll(GENERAL_PARAMETERS);
        supportedParameters.addAll(CONFIGURATION_REFERENCE_PARAMETERS);
        supportedParameters.addAll(NESTED_PARAMETERS);
    }

    private Collection<String> discoverAllParameterConstants() throws IllegalAccessException {
        Collection<String> strings = new ArrayList<>();
        Class<?> clazz = SupportedParameters.class;
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            Object value = field.get(null);

            if (value instanceof String) {
                strings.addAll(Arrays.asList((String) value));
            }

        }
        return strings;
    }
}
