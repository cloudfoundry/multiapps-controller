package org.cloudfoundry.multiapps.controller.web.configuration;

import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.EngineConfigurator;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.api.types.VariableTypes;
import org.flowable.variable.service.impl.types.LongStringType;

public class CustomEngineConfigurator implements EngineConfigurator {

    @Override
    public void beforeInit(AbstractEngineConfiguration engineConfiguration) {

    }

    @Override
    public void configure(AbstractEngineConfiguration engineConfiguration) {
        ProcessEngineConfigurationImpl configuration = (ProcessEngineConfigurationImpl) engineConfiguration;
        VariableTypes variableTypes = configuration.getVariableTypes();
        VariableType longStringVariableType = variableTypes.getVariableType(LongStringType.TYPE_NAME);
        int longStringVariableTypeIndex = variableTypes.getTypeIndex(LongStringType.TYPE_NAME);
        // We are first removing the Flowable LongStringType, and then we add our custom
        variableTypes.removeType(longStringVariableType);
        variableTypes.addType(new CustomLongStringType(configuration.getMaxLengthString()), longStringVariableTypeIndex);
    }

    @Override
    public int getPriority() {
        return 0;
    }
}
