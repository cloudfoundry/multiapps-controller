package org.cloudfoundry.multiapps.controller.process.security;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cloudfoundry.multiapps.controller.process.security.store.SecretTokenStore;
import org.cloudfoundry.multiapps.controller.process.security.util.SecretTokenUtil;
import org.cloudfoundry.multiapps.controller.process.variables.Serializer;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class SecretTokenSerializerTest {

    private SecretTokenStore secretTokenStore;

    private SecretTransformationStrategy secretTransformationStrategy;

    private static final String VARIABLE_NAME = "fake_variable";
    private static final String PROCESS_INSTANCE_ID = "pid_test";

    @BeforeEach
    void setUp() {
        secretTokenStore = Mockito.mock(SecretTokenStore.class);
        secretTransformationStrategy = Mockito.mock(SecretTransformationStrategy.class);

        when(secretTransformationStrategy.getJsonSecretFieldNames()).thenReturn(Collections.emptySet());
    }

    public static final class StringSerializerHelper implements Serializer<String> {
        @Override
        public Object serialize(String value) {
            return value;
        }

        @Override
        public String deserialize(Object serializedValue) {
            if (serializedValue == null) {
                return null;
            }
            return serializedValue.toString();
        }

        public String deserialize(Object serializedValue, VariableContainer container) {
            return deserialize(serializedValue);
        }
    }

    public static final class ListSerializerHelper implements Serializer<List<String>> {
        @Override
        public Object serialize(List<String> value) {
            return value;
        }

        @Override
        public List<String> deserialize(Object serializedValue) {
            return (List<String>) serializedValue;
        }

        public List<String> deserialize(Object serializedValue, VariableContainer container) {
            return (List<String>) serializedValue;
        }
    }

    @Test
    void testSerializeAndDeserializeSuccessWhenEntireVariableIsSecretString() {
        Set<String> names = new HashSet<>();
        names.add("value");
        when(secretTransformationStrategy.getJsonSecretFieldNames()).thenReturn(names);

        when(secretTokenStore.put(PROCESS_INSTANCE_ID, VARIABLE_NAME, "secret_text")).thenReturn(7L);
        when(secretTokenStore.get(PROCESS_INSTANCE_ID, 7L)).thenReturn("secret_text");

        SecretTokenSerializer<String> serializer = new SecretTokenSerializer<>(
            new StringSerializerHelper(), secretTokenStore, secretTransformationStrategy,
            PROCESS_INSTANCE_ID, VARIABLE_NAME);

        String inputJson = "{\"value\":\"secret_text\"}";
        Object encryptedToken = serializer.serialize(inputJson);
        assertInstanceOf(String.class, encryptedToken);
        assertNotEquals(inputJson, encryptedToken);

        String plainText = serializer.deserialize(encryptedToken);
        assertEquals(inputJson, plainText);
    }

    @Test
    void testSerializeAndDeserializeSuccessWhenJsonWithStringField() {
        Set<String> secretNames = new HashSet<>();
        secretNames.add("password");
        when(secretTransformationStrategy.getJsonSecretFieldNames()).thenReturn(secretNames);

        when(secretTokenStore.put(eq(PROCESS_INSTANCE_ID), eq(VARIABLE_NAME), eq("internal_one"))).thenReturn(13L);
        when(secretTokenStore.get(PROCESS_INSTANCE_ID, 13L)).thenReturn("internal_one");

        SecretTokenSerializer<String> serializer = new SecretTokenSerializer<>(
            new StringSerializerHelper(), secretTokenStore, secretTransformationStrategy,
            PROCESS_INSTANCE_ID, VARIABLE_NAME);

        String testInput = "{\"user\":\"u\",\"password\":\"internal_one\",\"other\":123}";
        Object serialized = serializer.serialize(testInput);
        assertInstanceOf(String.class, serialized);
        String tokenizedJson = (String) serialized;
        assertTrue(tokenizedJson.contains("password"));
        assertFalse(tokenizedJson.contains("internal_one"));

        String valueAtFirst = serializer.deserialize(tokenizedJson);
        assertEquals(testInput, valueAtFirst);
    }

    @Test
    void testSerializeAndDeserializeSuccessWhenJsonWithStringFieldLargerOne() {
        Set<String> secretNames = new HashSet<>();
        secretNames.add("config");
        when(secretTransformationStrategy.getJsonSecretFieldNames()).thenReturn(secretNames);

        when(secretTokenStore.put(eq(PROCESS_INSTANCE_ID), eq(VARIABLE_NAME), eq("must_not_be_shown"))).thenReturn(15L);
        when(secretTokenStore.get(PROCESS_INSTANCE_ID, 15L)).thenReturn("must_not_be_shown");

        SecretTokenSerializer<String> serializer = new SecretTokenSerializer<>(
            new StringSerializerHelper(), secretTokenStore, secretTransformationStrategy,
            PROCESS_INSTANCE_ID, VARIABLE_NAME);

        String testInput =
            "{\"id\":\"0001\",\"type\":\"donut\",\"name\":\"Cake\",\"image\":{\"url\":\"images/0001.jpg\",\"width\":200,\"height\":200},\"thumbnail\":{\"url\":\"images/thumbnails/0001.jpg\",\"config\":\"must_not_be_shown\",\"height\":32}}";
        Object serialized = serializer.serialize(testInput);
        assertInstanceOf(String.class, serialized);
        String tokenizedJson = (String) serialized;
        assertTrue(tokenizedJson.contains("config"));
        assertFalse(tokenizedJson.contains("must_not_be_shown"));

        String valueAtFirst = serializer.deserialize(tokenizedJson);
        assertEquals(testInput, valueAtFirst);
    }

    @Test
    void testSerializeSuccessWhenSecretParameterIsAReference() {
        Set<String> secretNames = new HashSet<>();
        secretNames.add("password");
        when(secretTransformationStrategy.getJsonSecretFieldNames()).thenReturn(secretNames);

        SecretTokenSerializer<String> serializer = new SecretTokenSerializer<>(
            new StringSerializerHelper(), secretTokenStore, secretTransformationStrategy,
            PROCESS_INSTANCE_ID, VARIABLE_NAME);

        String testInput = "{\"password\":\"${referenceToSomething}\"}";
        Object testOutput = serializer.serialize(testInput);

        assertInstanceOf(String.class, testOutput);
        String tokenizedJson = (String) testOutput;
        assertEquals(testInput, tokenizedJson);
    }

    @Test
    void testDeserializeWhenPlainTokenString() {
        String token = SecretTokenUtil.of(42L);
        when(secretTokenStore.get(PROCESS_INSTANCE_ID, 42L)).thenReturn("plain_value");

        SecretTokenSerializer<String> serializer = new SecretTokenSerializer<>(
            new StringSerializerHelper(), secretTokenStore, secretTransformationStrategy,
            PROCESS_INSTANCE_ID, VARIABLE_NAME);

        String result = serializer.deserialize(token);
        assertEquals("plain_value", result);
    }

    @Test
    void testSerializeLeavesPlainNonJsonUntouchedWhenCsvDisabled() {
        when(secretTransformationStrategy.getJsonSecretFieldNames()).thenReturn(Collections.emptySet());

        SecretTokenSerializer<String> serializer = new SecretTokenSerializer<>(
            new StringSerializerHelper(), secretTokenStore, secretTransformationStrategy,
            PROCESS_INSTANCE_ID, VARIABLE_NAME);

        String testInput = "just_a_plain_string";
        Object result = serializer.serialize(testInput);
        assertEquals(testInput, result);
        assertEquals(testInput, serializer.deserialize(result));
    }

    @Test
    void testSerializeAndDeserializeWhenListOfJsonElements() {
        Set<String> secretNames = new HashSet<>();
        secretNames.add("password");
        when(secretTransformationStrategy.getJsonSecretFieldNames()).thenReturn(secretNames);

        when(secretTokenStore.put(PROCESS_INSTANCE_ID, VARIABLE_NAME, "p1")).thenReturn(100L);
        when(secretTokenStore.get(PROCESS_INSTANCE_ID, 100L)).thenReturn("p1");

        SecretTokenSerializer<List<String>> serializer = new SecretTokenSerializer<>(
            new ListSerializerHelper(), secretTokenStore, secretTransformationStrategy,
            PROCESS_INSTANCE_ID, VARIABLE_NAME);

        List<String> testInput = List.of("{\"password\":\"p1\"}", "{\"other\":1}");
        Object result = serializer.serialize(testInput);
        assertInstanceOf(List.class, result);
        List<Object> tokenized = (List<Object>) result;

        String firstElement = String.valueOf(tokenized.get(0));
        assertTrue(firstElement.contains("password"));
        assertFalse(firstElement.contains("p1"));

        List<String> valueAtFirst = serializer.deserialize(tokenized);
        assertEquals(testInput, valueAtFirst);
    }

}
