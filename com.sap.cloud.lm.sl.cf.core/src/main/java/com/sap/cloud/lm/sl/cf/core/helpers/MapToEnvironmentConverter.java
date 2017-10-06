package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.Map;
import java.util.TreeMap;

public class MapToEnvironmentConverter {

    private ObjectToEnvironmentValueConverter objectToEnvValueConverter;

    public MapToEnvironmentConverter(boolean prettyPrinting) {
        this.objectToEnvValueConverter = new ObjectToEnvironmentValueConverter(prettyPrinting);
    }

    public Map<String, String> asEnv(Map<String, Object> map) {
        Map<String, String> result = new TreeMap<>();
        for (String key : map.keySet()) {
            Object v = map.get(key);
            String s = objectToEnvValueConverter.convert(v);
            result.put(key, s);
        }
        return result;
    }

}
