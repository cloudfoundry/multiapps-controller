package com.sap.cloud.lm.sl.cf.core.validators.parameters.v2_0;

import java.util.Arrays;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.mta.handlers.v2_0.DescriptorParser;
import com.sap.cloud.lm.sl.mta.model.v2_0.DeploymentDescriptor;

@RunWith(Parameterized.class)
public class DescriptorParametersValidatorTest
    extends com.sap.cloud.lm.sl.cf.core.validators.parameters.v1_0.DescriptorParametersValidatorTest {

    public DescriptorParametersValidatorTest(String descriptorLocation, String expected) {
        super(descriptorLocation, expected);
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) All parameters are valid:
            {
                "mtad-01.yaml", "R:mtad-01.yaml.json",
            },
            // (1) Invalid port in a descriptor module:
            {
                "mtad-02.yaml", "E:Value for parameter \"foo#port\" is not valid and cannot be corrected",
            },
            // (2) Invalid host in a descriptor module:
            {
                "mtad-03.yaml", "R:mtad-03.yaml.json",
            },
            // (3) Invalid parameter in a descriptor resource:
            {
                "mtad-04.yaml", "R:mtad-04.yaml.json",
            },
//            // (4) Invalid host in a descriptor module that cannot be corrected:
//            {
//                "mtad-05.yaml", "E:Could not create a valid host from \"(__)\"",
//            },
            // (5) Invalid parameter value in property:
            {
                "mtad-06.yaml", "R:mtad-06.yaml.json",
            },
            // (6) Invalid parameter value in provided dependency property:
            {
                "mtad-07.yaml", "R:mtad-07.yaml.json",
            },
//            // (7) Invalid host in a requires dependency:
//            {
//                "mtad-08.yaml", "R:mtad-08.yaml.json",
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
        com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor descriptor) {
        return new DescriptorParametersValidator((DeploymentDescriptor) descriptor, PARAMETER_VALIDATORS);
    }

}
