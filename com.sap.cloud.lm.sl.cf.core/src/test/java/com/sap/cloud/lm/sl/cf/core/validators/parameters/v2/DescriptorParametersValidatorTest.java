package com.sap.cloud.lm.sl.cf.core.validators.parameters.v2;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.validators.parameters.DomainValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.HostValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParameterValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.PortValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.RouteValidator;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorParser;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.Resource;

@RunWith(Parameterized.class)
public class DescriptorParametersValidatorTest {

    protected static final List<ParameterValidator> PARAMETER_VALIDATORS = Arrays.asList(new PortValidator(), new HostValidator(),
                                                                                         new DomainValidator(), new TestValidator(),
                                                                                         new RouteValidator());

    private String descriptorLocation;
    private Expectation expectation;

    private DescriptorParametersValidator validator;

    public DescriptorParametersValidatorTest(String descriptorLocation, Expectation expectation) {
        this.descriptorLocation = descriptorLocation;
        this.expectation = expectation;
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
            // TODO
            // @formatter:off
            // (0) All parameters are valid:
            {
                "mtad-01.yaml", new Expectation(Expectation.Type.RESOURCE, "mtad-01.yaml.json"),
            },
            // (1) Invalid port in a descriptor module:
            {
                "mtad-02.yaml", new Expectation(Expectation.Type.EXCEPTION, "Value for parameter \"foo#port\" is not valid and cannot be corrected")
            },
            // (2) Invalid host in a descriptor module:
            {
                "mtad-03.yaml", new Expectation(Expectation.Type.RESOURCE, "mtad-03.yaml.json"),
            },
            // (3) Invalid parameter in a descriptor resource:
            {
                "mtad-04.yaml", new Expectation(Expectation.Type.RESOURCE, "mtad-04.yaml.json"),
            },
            // (4) Invalid host in a descriptor module that cannot be corrected:
//            {
//                "mtad-05.yaml", new Expectation(Expectation.Type.EXCEPTION, "Could not create a valid host from \"(__)\"")
//            },
            // (5) Invalid parameter value in property:
            {
                "mtad-06.yaml", new Expectation(Expectation.Type.RESOURCE, "mtad-06.yaml.json"),
            },
            // (6) Invalid parameter value in provided dependency property:
            {
                "mtad-07.yaml", new Expectation(Expectation.Type.RESOURCE, "mtad-07.yaml.json"),
            },
            // (7) Invalid host in a requires dependency:
//            {
//                "mtad-08.yaml", new Expectation(Expectation.Type.RESOURCE, "mtad-08.yaml.json"),
//            },
            // (8) Invalid parameter value in provided dependency property:
//            {
//                "mtad-09.yaml", new Expectation(Expectation.Type.RESOURCE, "mtad-09.yaml.json"),
//            },
            // (9) Invalid parameter value in provided dependency property:
//            {
//                "mtad-10.yaml", new Expectation(Expectation.Type.EXCEPTION, "Could not create a valid route from"),
//            },
// @formatter:on
        });
    }

    @Before
    public void setUp() throws Exception {
        String descriptorYaml = TestUtil.getResourceAsString(descriptorLocation, getClass());
        validator = createDescriptorParametersValidator(getDescriptorParser().parseDeploymentDescriptorYaml(descriptorYaml));
    }

    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

    protected DescriptorParametersValidator createDescriptorParametersValidator(DeploymentDescriptor descriptor) {
        return new DescriptorParametersValidator(descriptor, PARAMETER_VALIDATORS);
    }

    @Test
    public void testValidate() {
        TestUtil.test(() -> validator.validate(), expectation, getClass());
    }

    protected static class TestValidator implements ParameterValidator {

        @Override
        public boolean isValid(Object parameter) {
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
        public Object attemptToCorrect(Object parameter) {
            return "test";
        }

        @Override
        public boolean canCorrect() {
            return true;
        }

    }

}
