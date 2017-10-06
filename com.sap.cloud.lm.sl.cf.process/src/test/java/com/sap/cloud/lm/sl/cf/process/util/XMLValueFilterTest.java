package com.sap.cloud.lm.sl.cf.process.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class XMLValueFilterTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private String input;
    private String expectedOutput;

    // "\u004f\u0055\u0054\u0020\u0020\u0020\u0050\u004b\u0005\u0006\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0066\u0069"

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (00) Real scenario:
            {
                "\u004f\u0055\u0054\u0020\u0020\u0020\u0050\u004b\u0005\u0006\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0020\u0066\u0069",
                "OUT   PK**                  fi"
            },
            // (01) No special characters:
            {
                "   Out    Sample output text with no illegal characters",
                "   Out    Sample output text with no illegal characters"
            },
            // (02) Legal xml reserved characters:
            {
                "   Out    Sample output text with >legal /xml character<s",
                "   Out    Sample output text with >legal /xml character<s"
            },
            // (02) Legal xml reserved characters:
            {
                "Many replacements \u0000\u0001\u0002\u0003 with \u0004\u0005\u0006\u0007 random characters \u0008\u000B\u000C in betwen \u000E\u000F\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019\u001A\u001B\u001C\u001D\u001E\u001F\uFFFE\uFFFF",
                "Many replacements **** with **** random characters *** in betwen ********************"
            },
// @formatter:on
        });
    }

    public XMLValueFilterTest(String input, String output) {
        this.input = input;
        expectedOutput = output;
    }

    @Test
    public void testBuildpackOutputFilter() {
        assertEquals(expectedOutput, new XMLValueFilter(input).getFiltered());
    }

}
