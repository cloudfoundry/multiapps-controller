package com.sap.cloud.lm.sl.cf.process.metadata.parameters;

import java.util.Arrays;

import org.cloudfoundry.multiapps.common.SLException;

import com.sap.cloud.lm.sl.cf.process.DeployStrategy;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.cf.web.api.model.parameters.ParameterConverter;

public class DeployStrategyParameterConverter implements ParameterConverter {

    private static final String DEFAULT = "default";
    private static final String BLUE_GREEN = "blue-green";

    @Override
    public DeployStrategy convert(Object value) {
        String deployStrategy = String.valueOf(value);
        switch (deployStrategy) {
            case DEFAULT:
                return DeployStrategy.DEFAULT;
            case BLUE_GREEN:
                return DeployStrategy.BLUE_GREEN;
            default:
                throw new SLException(Messages.INVALID_VALUE_0_FOR_PARAMETER_1_VALID_VALUES_ARE_2,
                                      deployStrategy,
                                      Variables.DEPLOY_STRATEGY.getName(),
                                      Arrays.asList(DEFAULT, BLUE_GREEN));
        }
    }

}
