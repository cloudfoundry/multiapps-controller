package com.sap.cloud.lm.sl.cf.process.util;

import static com.sap.cloud.lm.sl.cf.core.model.SupportedParameters.DEFAULT_CONTAINER_NAME;
import static com.sap.cloud.lm.sl.cf.core.model.SupportedParameters.DEFAULT_DOMAIN;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.YamlParser;
import com.sap.cloud.lm.sl.mta.handlers.v3.DescriptorParser;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;

public class ReadOnlyParametersCheckerTest {

    private ReadOnlyParametersChecker readOnlyParametersChecker = new ReadOnlyParametersChecker();

    protected static Stream<Arguments> testReadOnlyParameters() {
        return Stream.of(
// @formatter:off
                // try overwriting every global read-only parameter
                Arguments.of("mtad-00.yaml", System.lineSeparator() + MessageFormat.format(Messages.PARAMETERS_HAVE_READ_ONLY_ELEMENTS, "Global",
                        Collections.singletonList(DEFAULT_DOMAIN)), SLException.class),
                // try overwriting every module read-only parameter
                Arguments.of("mtad-01.yaml", System.lineSeparator() + MessageFormat.format(Messages.PARAMETERS_HAVE_READ_ONLY_ELEMENTS, "demo-app",
                        Collections.singletonList(DEFAULT_DOMAIN)), SLException.class),
                // try overwriting every resource read-only parameter
                Arguments.of("mtad-02.yaml", System.lineSeparator() + MessageFormat.format(Messages.PARAMETERS_HAVE_READ_ONLY_ELEMENTS, "demo-service",
                        Collections.singletonList(DEFAULT_CONTAINER_NAME)), SLException.class),
                // valid descriptor
                Arguments.of("mtad-03.yaml", null, null),
                // try overwriting every global and module read-only parameter
                Arguments.of("mtad-04.yaml", System.lineSeparator() + String.join(System.lineSeparator(), MessageFormat.format(Messages.PARAMETERS_HAVE_READ_ONLY_ELEMENTS, "Global",
                        Collections.singletonList(DEFAULT_DOMAIN)), MessageFormat.format(Messages.PARAMETERS_HAVE_READ_ONLY_ELEMENTS, "demo-app",
                        Collections.singletonList(DEFAULT_DOMAIN))), SLException.class),
                // try overwriting every global and resource read-only parameter
                Arguments.of("mtad-05.yaml", System.lineSeparator() + String.join(System.lineSeparator(), MessageFormat.format(Messages.PARAMETERS_HAVE_READ_ONLY_ELEMENTS, "Global",
                        Collections.singletonList( DEFAULT_DOMAIN)), MessageFormat.format(Messages.PARAMETERS_HAVE_READ_ONLY_ELEMENTS, "demo-service",
                        Collections.singletonList(DEFAULT_CONTAINER_NAME))), SLException.class),
                // try overwriting every module and resource read-only parameter
                Arguments.of("mtad-06.yaml", System.lineSeparator() + String.join(System.lineSeparator(), MessageFormat.format(Messages.PARAMETERS_HAVE_READ_ONLY_ELEMENTS, "demo-app",
                        Collections.singletonList(DEFAULT_DOMAIN)), MessageFormat.format(Messages.PARAMETERS_HAVE_READ_ONLY_ELEMENTS, "demo-service",
                        Collections.singletonList(DEFAULT_CONTAINER_NAME))), SLException.class)
// @formatter:on
        );
    }

    @MethodSource
    @ParameterizedTest
    public void testReadOnlyParameters(String filename, String expectedExceptionMessage, Class<? extends SLException> expectedException) {
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
