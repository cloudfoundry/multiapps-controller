package org.cloudfoundry.multiapps.controller.process.security;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.security.store.SecretTokenStore;
import org.cloudfoundry.multiapps.controller.process.security.util.SecretTokenUtil;
import org.cloudfoundry.multiapps.controller.process.variables.Serializer;
import org.flowable.common.engine.api.variable.VariableContainer;

public class SecretTokenSerializer<T> implements Serializer<T> {

    private static ObjectMapper objectMapper = new ObjectMapper();

    private Serializer<T> serializer;

    private SecretTokenStore secretTokenStore;

    private SecretTransformationStrategy secretTransformationStrategy;

    private final String processInstanceId;

    private final String variableName;

    public SecretTokenSerializer(Serializer<T> serializer, SecretTokenStore secretTokenStore,
                                 SecretTransformationStrategy secretTransformationStrategy, String processInstanceId, String variableName) {
        this.serializer = serializer;
        this.secretTokenStore = secretTokenStore;
        this.secretTransformationStrategy = secretTransformationStrategy;
        this.processInstanceId = processInstanceId;
        this.variableName = variableName;
    }

    @Override
    public Object serialize(T value) {
        if (value == null) {
            return serializer.serialize(null);
        }

        Object encodedObject = serializer.serialize(value);

        if (encodedObject instanceof String) {
            return handleString((String) encodedObject, true);
        }

        if (encodedObject instanceof byte[]) {
            return handleBytes((byte[]) encodedObject, true);
        }

        if (encodedObject instanceof List) {
            return handleList((List<Object>) encodedObject, true);
        }

        return encodedObject;
    }

    @Override
    public T deserialize(Object serializedValue) {
        if (serializedValue == null) {
            return serializer.deserialize(null);
        }

        Object valueToDecode = serializedValue;

        if (serializedValue instanceof String) {
            valueToDecode = handleString((String) serializedValue, false);
        } else if (serializedValue instanceof byte[]) {
            valueToDecode = handleBytes((byte[]) serializedValue, false);
        } else if (serializedValue instanceof List) {
            valueToDecode = handleList((List<Object>) serializedValue, false);
        }

        return serializer.deserialize(valueToDecode);
    }

    @Override
    public T deserialize(Object serializedValue, VariableContainer container) {
        if (serializedValue == null) {
            return serializer.deserialize(null);
        }

        Object valueToDecode = serializedValue;

        if (serializedValue instanceof String) {
            valueToDecode = handleString((String) serializedValue, false);
        } else if (serializedValue instanceof byte[]) {
            valueToDecode = handleBytes((byte[]) serializedValue, false);
        } else if (serializedValue instanceof List) {
            valueToDecode = handleList((List<Object>) serializedValue, false);
        }

        return serializer.deserialize(valueToDecode, container);
    }

    private Object handleString(String stringObject, boolean censor) {
        String transformedJson = transformJson(stringObject, censor);
        if (transformedJson != null) {
            return transformedJson;
        }

        if (!censor && SecretTokenUtil.isToken(stringObject)) {
            return detokenize(stringObject);
        }

        return stringObject;
    }

    private Object handleBytes(byte[] bytesArray, boolean censor) {
        String string = new String(bytesArray);
        String transformedJson = transformJson(string, censor);

        if (transformedJson != null) {
            return transformedJson.getBytes();
        }

        return bytesArray;
    }

    private Object handleList(List<Object> list, boolean censor) {
        return list.stream()
                   .map(element -> {
                       if (element instanceof String) {
                           String transformedJson = transformJson((String) element, censor);
                           if (transformedJson != null) {
                               return transformedJson;
                           } else {
                               return element;
                           }
                       } else if (element instanceof byte[]) {
                           String transformedJson = transformJson(new String((byte[]) element), censor);
                           if (transformedJson != null) {
                               return transformedJson.getBytes();
                           } else {
                               return element;
                           }
                       }
                       return element;
                   })
                   .collect(Collectors.toList());
    }

    private String transformJson(String candidate, boolean censor) {
        if (!isStringJson(candidate)) {
            return null;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(candidate);
            Set<String> keys = lowerCase(secretTransformationStrategy.getJsonSecretFieldNames());
            boolean[] changed = new boolean[1];

            JsonNode output = processJsonValue(rootNode, keys, censor, changed);

            if (changed[0]) {
                return objectMapper.writeValueAsString(output);
            }
            return null;
        } catch (Exception e) {
            throw new SecretTokenSerializerJsonException(
                MessageFormat.format(Messages.JSON_TRANSFORMATION_FAILED_FOR_VARIABLE_0, variableName), e);
        }
    }

    private boolean isStringJson(String string) {
        if (string == null) {
            return false;
        }
        string = string.trim();
        return string.startsWith("{") || string.startsWith("[");
    }

    private JsonNode processJsonValue(JsonNode currentNode, Set<String> keys, boolean censor, boolean[] changed) {
        if (currentNode.isObject()) {
            ObjectNode objectNode = currentNode.deepCopy();

            List<String> fields = new ArrayList<>();
            Iterator<String> fieldsIterator = objectNode.fieldNames();

            while (fieldsIterator.hasNext()) {
                fields.add(fieldsIterator.next());
            }

            for (String currentField : fields) {
                JsonNode childNode = objectNode.get(currentField);
                JsonNode processedNode = processJsonValue(childNode, keys, censor, changed);

                boolean doesNameMatch = keys.contains(currentField.toLowerCase());

                if (doesNameMatch && childNode.isValueNode()) {
                    String currentValue = null;
                    if (!childNode.isNull()) {
                        currentValue = childNode.asText();
                    }

                    if (censor) {
                        if (SecretTokenUtil.isToken(currentValue) || isPlaceholder(currentValue)) {
                            objectNode.put(currentField, currentValue);
                        } else {
                            objectNode.put(currentField, tokenize(currentValue));
                            changed[0] = true;
                        }
                    } else {
                        if (SecretTokenUtil.isToken(currentValue)) {
                            objectNode.put(currentField, detokenize(currentValue));
                            changed[0] = true;
                        } else {
                            objectNode.set(currentField, processedNode);
                        }
                    }
                } else {
                    objectNode.set(currentField, processedNode);
                }
            }
            return objectNode;

        }

        if (currentNode.isArray()) {
            ArrayNode arrayNode = currentNode.deepCopy();
            for (int i = 0; i < arrayNode.size(); i++) {
                arrayNode.set(i, processJsonValue(arrayNode.get(i), keys, censor, changed));
            }
            return arrayNode;
        }

        if (currentNode.isTextual()) {
            String value = currentNode.asText();
            if (!censor && SecretTokenUtil.isToken(value)) {
                changed[0] = true;
                String detokenizedValue = detokenize(value);
                return TextNode.valueOf(detokenizedValue);
            }
        }

        return currentNode;
    }

    private String tokenize(String plainText) {
        long id;
        if (plainText != null) {
            id = secretTokenStore.put(processInstanceId, variableName, plainText);
        } else {
            id = secretTokenStore.put(processInstanceId, variableName, "");
        }
        return SecretTokenUtil.of(id);
    }

    private String detokenize(String token) {
        long id = SecretTokenUtil.id(token);
        String result = secretTokenStore.get(processInstanceId, id);
        if (result == null) {
            throw new MissingSecretTokenException(
                MessageFormat.format(Messages.SECRET_VALUE_NOT_FOUND_FOR_TOKEN_0_PID_1_VARIABLE_2, token, processInstanceId,
                                     variableName));
        }
        return result;
    }

    private static Set<String> lowerCase(Set<String> keys) {
        if (keys == null) {
            return Collections.emptySet();
        }

        return keys.stream()
                   .filter(Objects::nonNull)
                   .map(String::toLowerCase)
                   .collect(Collectors.toSet());
    }

    private boolean isPlaceholder(String value) {
        if (value == null) {
            return false;
        }

        if (value.contains(Constants.PLACEHOLDER_PREFIX) && value.contains(Constants.PLACEHOLDER_POSTFIX)) {
            return true;
        }

        String trimmedValue = value.trim();
        return trimmedValue.startsWith(Constants.PLACEHOLDER_PREFIX) && trimmedValue.endsWith(Constants.PLACEHOLDER_POSTFIX);
    }

}
