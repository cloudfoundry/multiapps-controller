package org.cloudfoundry.multiapps.controller.client.facade.util;

import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CloudUtilTest {

    @Test
    void testParseIntegerFromNumber() {
        Assertions.assertEquals(Integer.valueOf(42), CloudUtil.parse(Integer.class, 42L));
    }

    @Test
    void testParseIntegerFromString() {
        Assertions.assertEquals(Integer.valueOf(7), CloudUtil.parse(Integer.class, "7"));
    }

    @Test
    void testParseLongFromNumber() {
        Assertions.assertEquals(Long.valueOf(99L), CloudUtil.parse(Long.class, 99));
    }

    @Test
    void testParseDoubleFromString() {
        Assertions.assertEquals(Double.valueOf(1.5), CloudUtil.parse(Double.class, "1.5"));
    }

    @Test
    void testParseReturnsZeroDefaultWhenObjectIsNullForInteger() {
        Assertions.assertEquals(Integer.valueOf(0), CloudUtil.parse(Integer.class, null));
    }

    @Test
    void testParseReturnsZeroDefaultWhenObjectIsNullForLong() {
        Assertions.assertEquals(Long.valueOf(0L), CloudUtil.parse(Long.class, null));
    }

    @Test
    void testParseReturnsZeroDefaultWhenObjectIsNullForDouble() {
        Assertions.assertEquals(Double.valueOf(0.0), CloudUtil.parse(Double.class, null));
    }

    @Test
    void testParseReturnsNullWhenObjectIsNullForUnknownDefaultType() {
        Assertions.assertNull(CloudUtil.parse(String.class, null));
    }

    @Test
    void testParseStringPassesThrough() {
        Assertions.assertEquals("hello", CloudUtil.parse(String.class, "hello"));
    }

    @Test
    void testParseDateReturnsNullForUnparsable() {
        Assertions.assertNull(CloudUtil.parse(Date.class, "not-a-date"));
    }

    @Test
    void testParseReturnsNullOnClassCastFailure() {
        Assertions.assertNull(CloudUtil.parse(Boolean.class, new Object()));
    }
}
