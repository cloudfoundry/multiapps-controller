package org.cloudfoundry.multiapps.controller.core.cf.metadata.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MtaMetadataUtilTest {

    @ParameterizedTest
    @MethodSource
    void testIsLegacyMd5Label(String labelValue, boolean expected) {
        assertEquals(expected, MtaMetadataUtil.isLegacyMd5Label(labelValue));
    }

    static Stream<Arguments> testIsLegacyMd5Label() {
        String mtaId = "anatz";
        String legacyMd5Label = "5ed8aa122cb6c03d9815e8adb57c67a2";
        return Stream.of(Arguments.of(legacyMd5Label, true),
                         Arguments.of(MtaMetadataUtil.getHashedLabel(mtaId), false),
                         Arguments.of(null, false),
                         Arguments.of("", false));
    }
}
