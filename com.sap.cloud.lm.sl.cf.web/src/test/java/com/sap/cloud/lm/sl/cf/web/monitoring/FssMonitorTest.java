package com.sap.cloud.lm.sl.cf.web.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;

class FssMonitorTest {

    private ApplicationConfiguration appConfigurations;

    private FssMonitor fssMonitor;

    @BeforeEach
    void setUpBefore() {
        appConfigurations = new ApplicationConfiguration();
        fssMonitor = new FssMonitor(appConfigurations);
    }

    @ParameterizedTest
    @MethodSource
    public void testParseProcessOutput(InputStream inputStream, double expectedResult) throws IOException {
        double parsedResult = fssMonitor.parseProcessOutput(inputStream);
        assertEquals(expectedResult, parsedResult);
    }

    public static Stream<Arguments> testParseProcessOutput() {
        return Stream.of(
        // @formatter:off
            Arguments.of(new ByteArrayInputStream("12K .".getBytes()), 0.01171875),
            Arguments.of(new ByteArrayInputStream("235 /var/vcap/data/".getBytes()), 235),
            Arguments.of(new ByteArrayInputStream("12M".getBytes()), 12),
            Arguments.of(new ByteArrayInputStream("12G".getBytes()), 12288),
            Arguments.of(new ByteArrayInputStream("123.50M /var/vcap/data/3fds-ddd-3455-abc34/".getBytes()), 123.50),
            Arguments.of(new ByteArrayInputStream("123.50M /var/vcap/data/3fds-ddd-3455-abc34/".getBytes()), 123.50),
            Arguments.of(new ByteArrayInputStream("12.32G".getBytes()), 12615.68)
        // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testParseProcessOutputFailure(InputStream inputStream) {
        Executable executable = () -> fssMonitor.parseProcessOutput(inputStream);
        assertThrows(IOException.class, executable);
    }

    public static Stream<Arguments> testParseProcessOutputFailure() {
        return Stream.of(
        // @formatter:off
            Arguments.of(new ByteArrayInputStream("no digits".getBytes())),
            Arguments.of(new ByteArrayInputStream("......".getBytes())),
            Arguments.of(new ByteArrayInputStream("/vcap/data/some/path/here/".getBytes()))
            // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testCacheValidation(String path, LocalTime lastCheck, boolean expectedResult) {
        fssMonitor.updateTimes.put(path, lastCheck);
        boolean actualResult = fssMonitor.isCacheValid(path);
        assertEquals(expectedResult, actualResult);
    }

    public static Stream<Arguments> testCacheValidation() {
        return Stream.of(
        // @formatter:off
            Arguments.of("/var/vcap/path", LocalTime.now(), true),
            Arguments.of(".", LocalTime.now().minusMinutes(10), true),
            Arguments.of("/home/", LocalTime.now().minusMinutes(50), false)
        // @formatter:on
        );
    }
}
