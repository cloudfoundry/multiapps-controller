package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class MapToEnvironmentConverter {

    private ObjectToEnvironmentValueConverter objectToEnvValueConverter;

    public MapToEnvironmentConverter(boolean prettyPrinting) {
        this.objectToEnvValueConverter = new ObjectToEnvironmentValueConverter(prettyPrinting);
    }

    public Map<String, String> asEnv(Map<String, Object> map) {
        Map<String, String> result = new TreeMap<>();
        for (Entry<String, Object> entry : map.entrySet()) {
            String value = objectToEnvValueConverter.convert(entry.getValue());
            result.put(entry.getKey(), value);
        }
        return result;
    }

}
