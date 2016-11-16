package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class ObjectToEnvironmentValueConverterTest {

    private String objectJsonFilePath;
    private String expected;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) There are escape sequences and they are preceded by 0 additional escape characters:
            {
                "object-00.json", "R:env-value-00.txt",
            },
            // (1) There are no escape sequences:
            {
                "object-01.json", "R:env-value-01.txt",
            },
            // (2) There are no escape sequences, because the marker characters are preceded by escaped escape characters:
            {
                "object-02.json", "R:env-value-02.txt",
            },
            // (3) There are escape sequences and they are preceded by 2 additional escape characters:
            {
                "object-03.json", "R:env-value-03.txt",
            },
            // (4) There are custom and other escape sequences:
            {
                "object-04.json", "R:env-value-04.txt",
            },
            // (5) The object is a string:
            {
                "object-05.json", "R:env-value-05.txt",
            },
// @formatter:on
        });
    }

    public ObjectToEnvironmentValueConverterTest(String objectJsonFilePath, String expected) {
        this.objectJsonFilePath = objectJsonFilePath;
        this.expected = expected;
    }

    @Test
    public void testConvert() {
        TestUtil.test(() -> new ObjectToEnvironmentValueConverter(true).convert(loadObject()), expected, getClass(), false);
    }

    private Object loadObject() throws Exception {
        String objectAsAString = TestUtil.getResourceAsString(objectJsonFilePath, getClass());
        return JsonUtil.fromJson(objectAsAString, Object.class);
    }

}
