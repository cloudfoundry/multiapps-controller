package org.cloudfoundry.multiapps.controller.process.metadata.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.cloudfoundry.multiapps.common.SLException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

class StartTimeoutParameterConverterTest {

    @Test
    void testConvertWithInvalidValueType() {
        assertThrows(SLException.class, () -> new StartTimeoutParameterConverter().convert(false));
    }

    @Test
    void testConvertWithInvalidValue() {
        assertThrows(SLException.class, () -> new StartTimeoutParameterConverter().convert(-1000));
    }

    @Test
    void testConvert() {
        var expectedStartTimeout = Duration.ofSeconds(1000);
        var actualStartTimeout = new StartTimeoutParameterConverter().convert("1000");
        assertEquals(expectedStartTimeout, actualStartTimeout);
    }

}
