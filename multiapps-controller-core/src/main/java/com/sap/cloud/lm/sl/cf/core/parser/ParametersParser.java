package com.sap.cloud.lm.sl.cf.core.parser;

import java.util.List;
import java.util.Map;

public interface ParametersParser<R> {

    R parse(List<Map<String, Object>> parametersList);

}
