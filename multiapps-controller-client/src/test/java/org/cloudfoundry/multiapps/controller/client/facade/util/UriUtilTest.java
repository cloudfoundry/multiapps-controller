package org.cloudfoundry.multiapps.controller.client.facade.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class UriUtilTest {

    @Test
    void testEncodeCharsWithOneChar() {
        String encodedString = UriUtil.encodeChars("space,name", List.of(","));
        assertEquals("space%2Cname", encodedString);
    }

    @Test
    void testEncodeCharsWithMultipleChars() {
        String encodedString = UriUtil.encodeChars("org space,name", List.of(",", " "));
        assertEquals("org%20space%2Cname", encodedString);
    }
}
