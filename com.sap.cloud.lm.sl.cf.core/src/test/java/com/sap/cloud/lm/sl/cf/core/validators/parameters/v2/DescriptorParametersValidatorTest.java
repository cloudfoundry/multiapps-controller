package com.sap.cloud.lm.sl.cf.core.validators.parameters.v2;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.validators.parameters.DomainValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.HostValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParameterValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.RouteValidator;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.Tester;
import com.sap.cloud.lm.sl.common.util.Tester.Expectation;
import com.sap.cloud.lm.sl.common.util.YamlParser;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorParser;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Resource;

@RunWith(Parameterized.class)
public class DescriptorParametersValidatorTest {

    protected static final List<ParameterValidator> PARAMETER_VALIDATORS = Arrays.asList(new HostValidator(), new DomainValidator(),
                                                                                         new TestValidator(), new RouteValidator());

    private final Tester tester = Tester.forClass(getClass());

    private final String descriptorLocation;
    private final Expectation expectation;

    private DescriptorParametersValidator validator;

    public DescriptorParametersValidatorTest(String descriptorLocation, Expectation expectation) {
        this.descriptorLocation = descriptorLocation;
        this.expectation = expectation;
    }

    @Before
    public void setUp() {
        String descriptorYaml = TestUtil.getResourceAsString(descriptorLocation, getClass());
        Map<String, Object> deploymentDescriptor = new YamlParser().convertYamlToMap(descriptorYaml);
        validator = createDescriptorParametersValidator(getDescriptorParser().parseDeploymentDescriptor(deploymentDescriptor));
    }

    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

    protected DescriptorParametersValidator createDescriptorParametersValidator(DeploymentDescriptor descriptor) {
        return new DescriptorParametersValidator(descriptor, PARAMETER_VALIDATORS);
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
            // TODO
            // @formatter:off
            // (0) All parameters are valid:
            {
                "mtad-01.yaml", new Expectation(Expectation.Type.JSON, "mtad-01.yaml.json"),
            },
            // (1) Invalid host in a descriptor module:
            {
                "mtad-03.yaml", new Expectation(Expectation.Type.JSON, "mtad-03.yaml.json"),
            },
            // (2) Invalid parameter in a descriptor resource:
            {
                "mtad-04.yaml", new Expectation(Expectation.Type.JSON, "mtad-04.yaml.json"),
            },
            // (3) Invalid parameter value in property:
            {
                "mtad-06.yaml", new Expectation(Expectation.Type.JSON, "mtad-06.yaml.json"),
            },
            // (4) Invalid parameter value in provided dependency property:
            {
                "mtad-07.yaml", new Expectation(Expectation.Type.JSON, "mtad-07.yaml.json"),
            },
            // (3) Invalid host in a descriptor module that cannot be corrected:
//            {
//                "mtad-05.yaml", new Expectation(Expectation.Type.EXCEPTION, "Could not create a valid host from \"(__)\"")
//            },
            // (6) Invalid host in a requires dependency:
//            {
//                "mtad-08.yaml", new Expectation(Expectation.Type.JSON, "mtad-08.yaml.json"),
//            },
            // (7) Invalid parameter value in provided dependency property:
//            {
//                "mtad-09.yaml", new Expectation(Expectation.Type.JSON, "mtad-09.yaml.json"),
//            },
            // (8) Invalid parameter value in provided dependency property:
//            {
//                "mtad-10.yaml", new Expectation(Expectation.Type.EXCEPTION, "Could not create a valid route from"),
//            },
// @formatter:on
        });
    }

    @Test
    public void testValidate() {
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
