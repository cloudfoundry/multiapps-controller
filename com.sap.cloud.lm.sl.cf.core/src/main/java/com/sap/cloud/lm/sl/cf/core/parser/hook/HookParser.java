package com.sap.cloud.lm.sl.cf.core.parser.hook;

import java.util.Map;

public interface HookParser<T> {

    public T parseParameters(Map<String, Object> hookParameters);

}