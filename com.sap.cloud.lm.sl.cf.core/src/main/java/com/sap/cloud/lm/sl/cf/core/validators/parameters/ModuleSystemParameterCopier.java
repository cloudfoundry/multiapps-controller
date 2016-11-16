package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import java.util.Collections;

import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.Module;

public class ModuleSystemParameterCopier implements ParameterValidator {

    private SystemParameters systemParameters;
    private String parameterName;

    public ModuleSystemParameterCopier(String parameterName, SystemParameters systemParameters) {
        this.systemParameters = systemParameters;
        this.parameterName = parameterName;
    }

    @Override
    public String attemptToCorrect(Object container, Object appName) {
        return getModuleSystemParameter(((Module) container).getName());
    }

    @Override
    public boolean validate(Object container, Object appName) {
        return getModuleSystemParameter(((Module) container).getName()).equals(appName);
    }

    @Override
    public String getParameterName() {
        return parameterName;
    }

    @Override
    public Class<Module> getContainerType() {
        return Module.class;
    }

    @Override
    public boolean canCorrect() {
        return true;
    }

    @SuppressWarnings("unchecked")
    private <T> T getModuleSystemParameter(String moduleName) {
        return (T) systemParameters.getModuleParameters().getOrDefault(moduleName, Collections.emptyMap()).get(parameterName);
    }

}
