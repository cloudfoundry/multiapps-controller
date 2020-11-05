package org.cloudfoundry.multiapps.controller.core.validators.parameters.v2;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.test.Tester;
import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.common.util.YamlParser;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.DomainValidator;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.HostValidator;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.ParameterValidator;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.RouteValidator;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorParser;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DescriptorParametersValidatorTest {

    protected static final List<ParameterValidator> PARAMETER_VALIDATORS = List.of(new HostValidator(), new DomainValidator(),
                                                                                   new TestValidator(), new RouteValidator(null, false));

    private final Tester tester = Tester.forClass(getClass());

    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

    protected DescriptorParametersValidator createDescriptorParametersValidator(DeploymentDescriptor descriptor) {
        return new DescriptorParametersValidator(descriptor, PARAMETER_VALIDATORS);
    }

    public static Stream<Arguments> testValidate() {
        return Stream.of(
        // @formatter:off
            // (0) All parameters are valid:
            Arguments.of("mtad-01.yaml", new Expectation(Expectation.Type.JSON, "mtad-01.yaml.json")),
            // (1) Invalid host in a descriptor module:
            Arguments.of("mtad-03.yaml", new Expectation(Expectation.Type.JSON, "mtad-03.yaml.json")),
            // (2) Invalid parameter in a descriptor resource:
            Arguments.of("mtad-04.yaml", new Expectation(Expectation.Type.JSON, "mtad-04.yaml.json")),
            // (3) Invalid parameter value in property:
            Arguments.of("mtad-06.yaml", new Expectation(Expectation.Type.JSON, "mtad-06.yaml.json")),
            // (4) Invalid parameter value in provided dependency property:
            Arguments.of("mtad-07.yaml", new Expectation(Expectation.Type.JSON, "mtad-07.yaml.json"))
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testValidate(String descriptorLocation, Expectation expectation) {
        String descriptorYaml = TestUtil.getResourceAsString(descriptorLocation, getClass());
        Map<String, Object> deploymentDescriptor = new YamlParser().convertYamlToMap(descriptorYaml);
        DescriptorParametersValidator validator = createDescriptorParametersValidator(getDescriptorParser().parseDeploymentDescriptor(deploymentDescriptor));
        tester.test(() -> validator.validate(), expectation);
    }

    protected static class TestValidator implements ParameterValidator {

        @Override
        public boolean isValid(Object parameter, final Map<String, Object> context) {
            return parameter.equals("test");
        }

        @Override
        public Class<?> getContainerType() {
            return Resource.class;
        }

        @Override
        public String getParameterName() {
            return "test";
        }

        @Override
        public Object attemptToCorrect(Object parameter, final Map<String, Object> context) {
            return "test";
        }

        @Override
        public boolean canCorrect() {
            return true;
        }

    }

}
