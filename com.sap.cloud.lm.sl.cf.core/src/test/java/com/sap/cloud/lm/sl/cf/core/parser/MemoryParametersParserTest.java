package com.sap.cloud.lm.sl.cf.core.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.common.ContentException;;

@RunWith(Parameterized.class)
public class MemoryParametersParserTest {

    private String memoryString;
    private Integer expectedParsedMemory;
    private Class<? extends RuntimeException> expectedExceptionClass;
    private List<Map<String, Object>> parametersList = new ArrayList<>();
    private static final Integer DEFAULT_MEMORY = 100;
    private final MemoryParametersParser parser = new MemoryParametersParser(SupportedParameters.MEMORY, "100");

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            {
                "2m", 2, null
            },
            {
                "4M", 4, null
            },
            {
                "5mb", 5, null
            },
            {
                "10MB", 10, null
            },
            {
                "5mB", 5, null
            },
            {
                "2g", 2 * 1024, null
            },
            {
                "3gb", 3 * 1024, null
            },
            {
                "5G", 5 * 1024, null
            },
            {
                "6GB", 6 * 1024, null
            },
            {
                "12gB", 12 * 1024, null
            },
            {
                "100", 100, null
            },
            {
                "test-mb", null, ContentException.class
            },
            {
                null, DEFAULT_MEMORY, null
            }
// @formatter:on
        });
    }

    public MemoryParametersParserTest(String memoryString, Integer expectedParsedMemory,
        Class<? extends RuntimeException> expectedExceptionClass) {
        this.memoryString = memoryString;
        this.expectedParsedMemory = expectedParsedMemory;
        this.expectedExceptionClass = expectedExceptionClass;
    }

    @Before
    public void setUp() {
        Map<String, Object> memoryParameterMap = new HashMap<>();
        memoryParameterMap.put(SupportedParameters.MEMORY, this.memoryString);
        parametersList.add(memoryParameterMap);
    }

    @Test
    public void testMemoryParsing() {
        assumeTrue(memoryString != null);
        if (expectedExceptionClass != null) {
            expectedException.expect(expectedExceptionClass);
        }
        Integer memory = parser.parse(parametersList);
        assertEquals(expectedParsedMemory, memory);

    }

    @Test
    public void testDefaultMemoryParsing() {
        assumeTrue(memoryString == null);
        Integer memory = parser.parse(new ArrayList<>());
        assertEquals(DEFAULT_MEMORY, memory);
    }
}
