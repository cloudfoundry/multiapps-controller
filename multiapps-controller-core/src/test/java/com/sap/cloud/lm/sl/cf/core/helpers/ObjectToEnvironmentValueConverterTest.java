package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.Arrays;

import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.common.util.TestUtil;
import org.cloudfoundry.multiapps.common.util.Tester;
import org.cloudfoundry.multiapps.common.util.Tester.Expectation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ObjectToEnvironmentValueConverterTest {

    private final Tester tester = Tester.forClass(getClass());

    private final String objectJsonFilePath;
    private final Expectation expectation;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) There are escape sequences and they are preceded by 0 additional escape characters:
            {
                "object-00.json", new Expectation(Expectation.Type.STRING, getResourceAsString("env-value-00.txt")),
            },
            // (1) There are no escape sequences:
            {
                "object-01.json", new Expectation(Expectation.Type.STRING, getResourceAsString("env-value-01.txt")),
            },
            // (2) There are no escape sequences, because the marker characters are preceded by escaped escape characters:
            {
                "object-02.json", new Expectation(Expectation.Type.STRING, getResourceAsString("env-value-02.txt")),
            },
            // (3) There are escape sequences and they are preceded by 2 additional escape characters:
            {
                "object-03.json", new Expectation(Expectation.Type.STRING, getResourceAsString("env-value-03.txt")),
            },
            // (4) There are custom and other escape sequences:
            {
                "object-04.json", new Expectation(Expectation.Type.STRING, getResourceAsString("env-value-04.txt")),
            },
            // (5) The object is a string:
            {
                "object-05.json", new Expectation(Expectation.Type.STRING, getResourceAsString("env-value-05.txt")),
            },
// @formatter:on
        });
    }

    public ObjectToEnvironmentValueConverterTest(String objectJsonFilePath, Expectation expectation) {
        this.objectJsonFilePath = objectJsonFilePath;
        this.expectation = expectation;
    }

    @Test
    public void testConvert() {
        tester.test(() -> new ObjectToEnvironmentValueConverter(true).convert(loadObject()), expectation);
    }

    private Object loadObject() {
        String objectAsAString = TestUtil.getResourceAsString(objectJsonFilePath, getClass());
        return JsonUtil.fromJson(objectAsAString, Object.class);
    }

    private static String getResourceAsString(String resource) {
        return TestUtil.getResourceAsString(resource, ObjectToEnvironmentValueConverterTest.class);
    }

}
