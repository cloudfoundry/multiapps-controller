package org.cloudfoundry.multiapps.controller.core.cf.detect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AppSuffixDeterminerTest {

    static Stream<Arguments> testAppSuffixDeterminer() {
        return Stream.of(
        //@formatter:off
            // (1) Keep original app names is not set and the process is not after resume phase
            Arguments.of(false, false, false),
            // (2) Keep original app names is set but the process is not after resume phase
            Arguments.of(true, false, false),
            // (3) Keep original app names is not set but the process is after resume phase
            Arguments.of(false, true, false),
            // (4) Keep original app names is set and the process is after resume phase
            Arguments.of(true, true, true)
        //@formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAppSuffixDeterminer(boolean keepOriginalNamesAfterDeploy, boolean isAfterResumePhase, boolean shouldAppendApplicationSuffix) {
        AppSuffixDeterminer appSuffixDeterminer = new AppSuffixDeterminer(keepOriginalNamesAfterDeploy, isAfterResumePhase);
        assertEquals(shouldAppendApplicationSuffix, appSuffixDeterminer.shouldAppendApplicationSuffix());
    }
}
