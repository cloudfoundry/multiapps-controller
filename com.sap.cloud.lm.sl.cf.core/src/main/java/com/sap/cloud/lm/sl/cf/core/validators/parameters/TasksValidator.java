package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.mta.model.v1_0.Module;
import com.sap.cloud.lm.sl.mta.schema.Element.ElementBuilder;
import com.sap.cloud.lm.sl.mta.schema.ListElement;
import com.sap.cloud.lm.sl.mta.schema.MapElement;
import com.sap.cloud.lm.sl.mta.schema.SchemaValidator;

public class TasksValidator implements ParameterValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TasksValidator.class);

    public static final String TASK_NAME_KEY = "name";
    public static final String TASK_COMMAND_KEY = "command";
    public static final String TASK_ENV_KEY = "env";

    private static final MapElement TASK = new MapElement();

    static {
        TASK.add(TASK_NAME_KEY, new ElementBuilder().type(String.class)
            .required(true)
            .buildSimple());
        TASK.add(TASK_COMMAND_KEY, new ElementBuilder().type(String.class)
            .required(true)
            .buildSimple());
        TASK.add(TASK_ENV_KEY, new ElementBuilder().type(Map.class)
            .buildSimple());
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean isValid(Object tasks) {
        if (!(tasks instanceof List)) {
            return false;
        }
        try {
            new SchemaValidator(new ListElement(TASK)).validate((List<Object>) tasks);
        } catch (ParsingException e) {
            // TODO: If we just return 'false' here, then the real cause of the issue would be lost. Refactor ParameterValidators so that
            // their validate methods throw an exception with a descriptive message, instead of just returning 'true' or 'false'.
            // LMCROSSITXSADEPLOY-237
            LOGGER.error("Error validating tasks: " + e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public Class<?> getContainerType() {
        return Module.class;
    }

    @Override
    public String getParameterName() {
        return SupportedParameters.TASKS;
    }

}
