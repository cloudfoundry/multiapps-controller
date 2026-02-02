package org.cloudfoundry.multiapps.controller.process.security;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.security.store.SecretTokenStore;
import org.cloudfoundry.multiapps.controller.process.security.util.SecretTokenUtil;
import org.cloudfoundry.multiapps.controller.process.variables.Serializer;
import org.flowable.common.engine.api.variable.VariableContainer;

public class SecretTokenSerializer<T> implements Serializer<T> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private final Serializer<T> serializer;

    private final SecretTokenStore secretTokenStore;

    private final Set<String> secretValues;

    private final String processInstanceId;

    private final String variableName;

    public SecretTokenSerializer(Serializer<T> serializer, SecretTokenStore secretTokenStore, Set<String> secretValues,
                                 String processInstanceId,
                                 String variableName) {
        this.serializer = serializer;
        this.secretTokenStore = secretTokenStore;
        this.secretValues = secretValues;
        this.processInstanceId = processInstanceId;
        this.variableName = variableName;
    }

    @Override
    public Object serialize(T value) {
        Object encodedObject = serializer.serialize(value);

        if (encodedObject instanceof String) {
            return encodeString((String) encodedObject);
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

        Object valueToDecode = deserializeHelper(serializedValue);
        return serializer.deserialize(valueToDecode);
    }

    @Override
    public T deserialize(Object serializedValue, VariableContainer container) {
        if (serializedValue == null) {
            return serializer.deserialize(null);
        }

        Object valueToDecode = deserializeHelper(serializedValue);
        return serializer.deserialize(valueToDecode, container);
    }

    private Object deserializeHelper(Object serializedValue) {
        Object valueToDecode = serializedValue;

        if (serializedValue instanceof String) {
            valueToDecode = decodeString((String) serializedValue);
        } else if (serializedValue instanceof byte[]) {
            valueToDecode = handleBytes((byte[]) serializedValue, false);
        } else if (serializedValue instanceof List) {
            valueToDecode = handleList((List<Object>) serializedValue, false);
        }

        return valueToDecode;
    }

    private Object decodeString(String stringObject) {
        String transformedJson = transformJson(stringObject, false);
        if (transformedJson != null) {
            return transformedJson;
        }

        if (SecretTokenUtil.isSecretToken(stringObject)) {
            return detokenize(stringObject);
        }

        return stringObject;
    }

    private Object encodeString(String stringObject) {
        String transformedJson = transformJson(stringObject, true);
        if (transformedJson != null) {
            return transformedJson;
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
                           return getResultFromTransformedJson(transformedJson, element, false);
                       } else if (element instanceof byte[]) {
                           String transformedJson = transformJson(new String((byte[]) element), censor);
                           return getResultFromTransformedJson(transformedJson, element, true);
                       }
                       return element;
                   })
                   .collect(Collectors.toList());
    }

    private Object getResultFromTransformedJson(String transformedJson, Object element, boolean isElementInstanceOfByte) {
        if (transformedJson != null) {
            if (!isElementInstanceOfByte) {
                return transformedJson;
            } else {
                return transformedJson.getBytes();
            }
        } else {
            return element;
        }
    }

    private String transformJson(String candidate, boolean censor) {
        if (!isValid(candidate)) {
            return null;
        }

        try {
            JsonNode rootNode = OBJECT_MAPPER.readTree(candidate);
            AtomicBoolean changed = new AtomicBoolean();
            JsonNode output = processJsonValue(rootNode, secretValues, censor, changed);

            if (changed.get()) {
                return OBJECT_MAPPER.writeValueAsString(output);
            }
            return null;
        } catch (Exception e) {
            throw new SLException(MessageFormat.format(Messages.JSON_TRANSFORMATION_FAILED_FOR_VARIABLE_0, variableName), e);
        }
    }

    public boolean isValid(String json) {
        try {
            OBJECT_MAPPER.readTree(json);
        } catch (JacksonException e) {
            return false;
        }
        return true;
    }

    private JsonNode processJsonValue(JsonNode currentNode, Set<String> keys, boolean censor, AtomicBoolean changed) {
        if (currentNode.isObject()) {
            return processObjectNode(currentNode, keys, censor, changed);
        }

        if (currentNode.isArray()) {
            return processArrayNode(currentNode, keys, censor, changed);
        }

        if (currentNode.isTextual()) {
            return processTextualNode(currentNode, censor, changed);
        }

        return currentNode;
    }

    private JsonNode processObjectNode(JsonNode currentNode, Set<String> keys, boolean censor, AtomicBoolean changed) {
        ObjectNode objectNode = currentNode.deepCopy();

        List<String> fields = new ArrayList<>();
        Iterator<String> fieldsIterator = objectNode.fieldNames();

        while (fieldsIterator.hasNext()) {
            fields.add(fieldsIterator.next());
        }

        for (String currentField : fields) {
            JsonNode childNode = objectNode.get(currentField);
            JsonNode processedNode = processJsonValue(childNode, keys, censor, changed);

            boolean isCurrentKeySecretValue = keys.contains(currentField);
            determineWhetherToCensorOrUncensor(childNode, processedNode, objectNode, currentField, censor, isCurrentKeySecretValue,
                                               changed);
        }
        return objectNode;
    }

    private void determineWhetherToCensorOrUncensor(JsonNode childNode, JsonNode processedNode, ObjectNode objectNode, String currentField,
                                                    boolean censor, boolean isCurrentKeySecretValue, AtomicBoolean changed) {
        if (isCurrentKeySecretValue && childNode.isValueNode()) {
            String currentValue = convertChildNodeToText(childNode);
            if (censor) {
                censorValue(objectNode, currentValue, currentField, changed);
            } else {
                uncensorValue(objectNode, currentValue, currentField, changed, processedNode);
            }
        } else {
            objectNode.set(currentField, processedNode);
        }
    }

    private JsonNode processArrayNode(JsonNode currentNode, Set<String> keys, boolean censor, AtomicBoolean changed) {
        ArrayNode arrayNode = currentNode.deepCopy();
        for (int i = 0; i < arrayNode.size(); i++) {
            arrayNode.set(i, processJsonValue(arrayNode.get(i), keys, censor, changed));
        }
        return arrayNode;
    }

    private JsonNode processTextualNode(JsonNode currentNode, boolean censor, AtomicBoolean changed) {
        String value = currentNode.asText();
        if (!censor && SecretTokenUtil.isSecretToken(value)) {
            changed.set(true);
            String detokenizedValue = detokenize(value);
            return forceToInteger(detokenizedValue);
        }
        return currentNode;
    }

    private void censorValue(ObjectNode objectNode, String currentValue, String currentField, AtomicBoolean changed) {
        if (SecretTokenUtil.isSecretToken(currentValue) || isPlaceholder(currentValue)) {
            objectNode.put(currentField, currentValue);
        } else {
            objectNode.put(currentField, tokenize(currentValue));
            changed.set(true);
        }
    }

    private void uncensorValue(ObjectNode objectNode, String currentValue, String currentField, AtomicBoolean changed,
                               JsonNode processedNode) {
        if (SecretTokenUtil.isSecretToken(currentValue)) {
            String detokenizedValue = detokenize(currentValue);
            JsonNode jsonConverted = forceToInteger(detokenizedValue);
            objectNode.set(currentField, jsonConverted);
            changed.set(true);
        } else {
            objectNode.set(currentField, processedNode);
        }
    }

    private String convertChildNodeToText(JsonNode childNode) {
        if (!childNode.isNull()) {
            return childNode.asText();
        }
        return null;
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
        long id = SecretTokenUtil.extractId(token);
        String result = secretTokenStore.get(processInstanceId, id);
        if (result == null) {
            throw new SLException(
                MessageFormat.format(Messages.SECRET_VALUE_NOT_FOUND_FOR_TOKEN_0_PID_1_VARIABLE_2, token, processInstanceId, variableName));
        }
        return result;
    }

    private boolean isPlaceholder(String value) {
        if (value == null) {
            return false;
        }

        return value.contains(Constants.PLACEHOLDER_PREFIX) && value.contains(Constants.PLACEHOLDER_SUFFIX);
    }

    private static boolean looksLikeStandardInteger(String numberString) {
        if (numberString == null) {
            return false;
        }

        return Constants.STANDARD_INT_PATTERN.matcher(numberString)
                                             .matches();
    }

    private static JsonNode forceToInteger(String numberString) {
        if (!looksLikeStandardInteger(numberString)) {
            return TextNode.valueOf(numberString);
        }

        try {
            int parsedNumber = Integer.parseInt(numberString);
            return IntNode.valueOf(parsedNumber);
        } catch (NumberFormatException e) {
            return TextNode.valueOf(numberString);
        }

    }

}
