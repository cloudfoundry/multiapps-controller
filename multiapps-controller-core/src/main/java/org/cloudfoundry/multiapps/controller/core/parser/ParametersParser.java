package org.cloudfoundry.multiapps.controller.core.parser;

import java.util.List;
import java.util.Map;

public interface ParametersParser<R> {

    R parse(List<Map<String, Object>> parametersList);

}
