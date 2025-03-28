package org.cloudfoundry.multiapps.controller.process.metadata.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;

import java.time.Duration;

class TimeoutParameterConverterTest {

    @Test
    void testConvertWithInvalidValueType() {
        assertThrows(SLException.class, () -> new TimeoutParameterConverter(Variables.APPS_STAGE_TIMEOUT_PROCESS_VARIABLE).convert(false));
    }

    @Test
    void testConvertWithInvalidValue() {
        assertThrows(SLException.class, () -> new TimeoutParameterConverter(Variables.APPS_STAGE_TIMEOUT_PROCESS_VARIABLE).convert(-1000));
    }

    @Test
    void testConvert() {
        var expectedStartTimeout = Duration.ofSeconds(1000);
        var actualStartTimeout = new TimeoutParameterConverter(Variables.APPS_STAGE_TIMEOUT_PROCESS_VARIABLE).convert("1000");
        assertEquals(expectedStartTimeout, actualStartTimeout);
    }

}
