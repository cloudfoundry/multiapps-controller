package org.cloudfoundry.multiapps.controller.client.facade.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

class JsonUtilTest {

    @Test
    void testUsingPrettyPrint() {
        testWithCustomPrinting();
    }

    @Test
    void testWithNoPrettyPrint() {
        testWithCustomPrinting();
    }

    private void testWithCustomPrinting() {
        Map<String, Object> fooMap = createTestProperties();

        String json = JsonUtil.convertToJson(fooMap, true);
        validateJsonMap(json);
    }

    @Test
    void testUsingTheMethodWithNoPrettyPrint() {
        Map<String, Object> fooMap = createTestProperties();

        String json = JsonUtil.convertToJson(fooMap);

        validateJsonMap(json);
    }

    private void validateJsonMap(String json) {
        System.out.println(json);

        Map<String, Object> resultFoo = JsonUtil.convertJsonToMap(json);

        assertTestProperties(resultFoo);
    }

    @Test
    void testConvertListToJsonAndBack() {
        List<String> testingList = Arrays.asList("foo", "bar", "baz");
        String jsonList = JsonUtil.convertToJson(testingList);

        System.out.println(jsonList);

        List<String> toList = JsonUtil.convertJsonToList(jsonList);

        assertEquals(3, toList.size());
        assertEquals(Arrays.asList("foo", "bar", "baz"), toList);
    }

    @Test
    void testWithNullMap() {

        Map<String, Object> foo = Collections.emptyMap();
        String fooJson = JsonUtil.convertToJson(foo);
        String barJson = "null";

        Map<String, Object> fooMap = JsonUtil.convertJsonToMap(fooJson);
        Map<String, Object> barMap = JsonUtil.convertJsonToMap(barJson);

        assertTrue(fooMap.isEmpty());
        assertNull(barMap);
    }

    private Map<String, Object> createTestProperties() {
        Map<String, Object> testProperties1 = new TreeMap<>();
        testProperties1.put("host", "localhost");
        testProperties1.put("port", 30030);
        testProperties1.put("long-value", (long) Integer.MAX_VALUE * 10);
        testProperties1.put("double-value", 1.5);
        Map<String, Object> testProperties2 = new TreeMap<>();
        testProperties2.put("port", 50000);
        testProperties2.put("long-value", (long) Integer.MAX_VALUE * 10);
        testProperties2.put("double-value", 1.5);
        testProperties1.put("test", testProperties2);
        testProperties1.put("list", Arrays.asList("foo", "bar", "baz"));
        return testProperties1;
    }

    @SuppressWarnings("unchecked")
    private void assertTestProperties(Map<String, Object> actualProperties) {
        assertTrue(actualProperties.get("host") instanceof String);
        assertTrue(actualProperties.get("port") instanceof Integer);
        assertTrue(actualProperties.get("long-value") instanceof Long);
        assertTrue(actualProperties.get("double-value") instanceof Double);
        assertTrue(((Map<String, Object>) actualProperties.get("test")).get("port") instanceof Integer);
        assertTrue(((Map<String, Object>) actualProperties.get("test")).get("long-value") instanceof Long);
        assertTrue(((Map<String, Object>) actualProperties.get("test")).get("double-value") instanceof Double);
        List<String> listFromMap = (List<String>) actualProperties.get("list");
        assertEquals(3, listFromMap.size());
        assertEquals(Arrays.asList("foo", "bar", "baz"), listFromMap);
    }

}
