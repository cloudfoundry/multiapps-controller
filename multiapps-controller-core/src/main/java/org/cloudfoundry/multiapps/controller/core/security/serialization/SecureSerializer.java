package org.cloudfoundry.multiapps.controller.core.security.serialization;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class SecureSerializer {

    protected final SecureSerializerConfiguration configuration;

    protected SecureSerializer(SecureSerializerConfiguration configuration) {
        this.configuration = configuration;
    }

    public String serialize(Object object) {
        Object objectTree = toTree(object);
        objectTree = maskSensitiveElements(objectTree);
        return serializeTree(objectTree);
    }

    private Object maskSensitiveElements(Object object) {
        return maskSensitiveElements("", object);
    }

    private Object maskSensitiveElements(String objectName, Object object) {
        if (objectName == null || object == null) {
            return null;
        }
        if (isSensitive(objectName) || isSensitive(object)) {
            return SecureSerializerConfiguration.SECURE_SERIALIZATION_MASK;
        }
        if (object instanceof Collection) {
            return maskSensitiveElements((Collection<?>) object);
        }
        if (object instanceof Map) {
            return maskSensitiveElements((Map<?, ?>) object);
        }
        return object;
    }

    private Object maskSensitiveElements(Collection<?> collection) {
        return collection.stream()
                         .map(this::maskSensitiveElements)
                         .collect(Collectors.toList());
    }

    private Object maskSensitiveElements(Map<?, ?> map) {
        // Do not refactor this loop into a stream. Collectors.toMap has a known bug (https://bugs.openjdk.java.net/browse/JDK-8148463) and
        // throws NullPointerExceptions when trying to insert null values in the map.
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object maskedValue = maskSensitiveElements((String) entry.getKey(), entry.getValue());
            result.put((String) entry.getKey(), maskedValue);
        }
        return result;
    }

    private boolean isSensitive(Object object) {
        return isScalar(object) && isSensitive(object.toString());
    }

    private boolean isSensitive(String string) {
        return configuration.isSensitive(string);
    }

    private boolean isScalar(Object object) {
        return !(object instanceof Collection) && !(object instanceof Map);
    }

    protected abstract String serializeTree(Object object);

    protected abstract Object toTree(Object object);

}
