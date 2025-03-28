package org.cloudfoundry.multiapps.controller.process.util;

import static org.cloudfoundry.multiapps.controller.core.model.SupportedParameters.DEFAULT_CONTAINER_NAME;
import static org.cloudfoundry.multiapps.controller.core.model.SupportedParameters.DEFAULT_DOMAIN;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.util.YamlParser;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorParser;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ReadOnlyParametersCheckerTest {

    private final ReadOnlyParametersChecker readOnlyParametersChecker = new ReadOnlyParametersChecker();

    protected static Stream<Arguments> testReadOnlyParameters() {
        return Stream.of(
            // try overwriting every global read-only parameter
            Arguments.of("mtad-00.yaml",
                         System.lineSeparator() + MessageFormat.format(Messages.PARAMETERS_HAVE_READ_ONLY_ELEMENTS, "Global",
                                                                       List.of(DEFAULT_DOMAIN)),
                         SLException.class),
            // try overwriting every module read-only parameter
            Arguments.of("mtad-01.yaml",
                         System.lineSeparator() + MessageFormat.format(Messages.PARAMETERS_HAVE_READ_ONLY_ELEMENTS, "demo-app",
                                                                       List.of(DEFAULT_DOMAIN)),
                         SLException.class),
            // try overwriting every resource read-only parameter
            Arguments.of("mtad-02.yaml",
                         System.lineSeparator() + MessageFormat.format(Messages.PARAMETERS_HAVE_READ_ONLY_ELEMENTS,
                                                                       "demo-service", List.of(DEFAULT_CONTAINER_NAME)),
                         SLException.class),
            // valid descriptor
            Arguments.of("mtad-03.yaml", null, null),
            // try overwriting every global and module read-only parameter
            Arguments.of("mtad-04.yaml",
                         System.lineSeparator() + String.join(System.lineSeparator(),
                                                              MessageFormat.format(Messages.PARAMETERS_HAVE_READ_ONLY_ELEMENTS,
                                                                                   "Global", List.of(DEFAULT_DOMAIN)),
                                                              MessageFormat.format(Messages.PARAMETERS_HAVE_READ_ONLY_ELEMENTS,
                                                                                   "demo-app", List.of(DEFAULT_DOMAIN))),
                         SLException.class),
            // try overwriting every global and resource read-only parameter
            Arguments.of("mtad-05.yaml",
                         System.lineSeparator()
                             + String.join(System.lineSeparator(),
                                           MessageFormat.format(Messages.PARAMETERS_HAVE_READ_ONLY_ELEMENTS, "Global",
                                                                List.of(DEFAULT_DOMAIN)),
                                           MessageFormat.format(Messages.PARAMETERS_HAVE_READ_ONLY_ELEMENTS, "demo-service",
                                                                List.of(DEFAULT_CONTAINER_NAME))),
                         SLException.class),
            // try overwriting every module and resource read-only parameter
            Arguments.of("mtad-06.yaml",
                         System.lineSeparator()
                             + String.join(System.lineSeparator(),
                                           MessageFormat.format(Messages.PARAMETERS_HAVE_READ_ONLY_ELEMENTS, "demo-app",
                                                                List.of(DEFAULT_DOMAIN)),
                                           MessageFormat.format(Messages.PARAMETERS_HAVE_READ_ONLY_ELEMENTS, "demo-service",
                                                                List.of(DEFAULT_CONTAINER_NAME))),
                         SLException.class));
    }

    @MethodSource
    @ParameterizedTest
    void testReadOnlyParameters(String filename, String expectedExceptionMessage, Class<? extends SLException> expectedException) {
        Map<String, Object> deploymentDescriptorMap = new YamlParser().convertYamlToMap(getClass().getResourceAsStream(filename));
        DeploymentDescriptor descriptor = getDescriptorParser().parseDeploymentDescriptor(deploymentDescriptorMap);
        if (expectedException != null) {
            Exception exception = Assertions.assertThrows(expectedException, () -> readOnlyParametersChecker.check(descriptor));
            Assertions.assertEquals(expectedExceptionMessage, exception.getMessage());
            return;
        }
        readOnlyParametersChecker.check(descriptor);
    }

    private DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

}
