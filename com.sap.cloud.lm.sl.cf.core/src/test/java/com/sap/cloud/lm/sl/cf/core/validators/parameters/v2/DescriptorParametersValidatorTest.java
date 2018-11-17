package com.sap.cloud.lm.sl.cf.core.validators.parameters.v2;

import java.util.Arrays;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorParser;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;

@RunWith(Parameterized.class)
public class DescriptorParametersValidatorTest
    extends com.sap.cloud.lm.sl.cf.core.validators.parameters.v1.DescriptorParametersValidatorTest {

    public DescriptorParametersValidatorTest(String descriptorLocation, Expectation expectation) {
        super(descriptorLocation, expectation);
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
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
//            // (4) Invalid host in a descriptor module that cannot be corrected:
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
//            // (7) Invalid host in a requires dependency:
//            {
//                "mtad-08.yaml", new Expectation(Expectation.Type.RESOURCE, "mtad-08.yaml.json"),
//            },
// @formatter:on
        });
    }

    @Override
    protected DescriptorParser getDescriptorParser() {
        return new DescriptorParser();
    }

    @Override
    protected DescriptorParametersValidator createDescriptorParametersValidator(
        com.sap.cloud.lm.sl.mta.model.v1.DeploymentDescriptor descriptor) {
        return new DescriptorParametersValidator((DeploymentDescriptor) descriptor, PARAMETER_VALIDATORS);
    }

}
