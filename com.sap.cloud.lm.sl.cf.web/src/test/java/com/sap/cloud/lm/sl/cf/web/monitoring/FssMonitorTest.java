package com.sap.cloud.lm.sl.cf.web.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;

class FssMonitorTest {

    private static Path tempDir;

    private ApplicationConfiguration appConfigurations;

    private FssMonitor fssMonitor;

    @BeforeAll
    static void setUpBeforeAll() throws IOException {
        tempDir = Files.createTempDirectory("testMonitor");
    }

    @AfterAll
    static void teardownAfterAll() throws IOException {
        Files.delete(tempDir);
    }

    @BeforeEach
    void setUpBefore() {
        appConfigurations = new ApplicationConfiguration();
        fssMonitor = new FssMonitor(appConfigurations);
    }

    @ParameterizedTest
    @MethodSource
    public void testGetUsedSpace(String path, LocalTime lastCheck, long cachedValue, long expectedResult) {
        fssMonitor.updateTimesMap.put(path, lastCheck);
        fssMonitor.usedSpaceMap.put(path, cachedValue);
        long actualResult = fssMonitor.calculateUsedSpace(path);
        assertEquals(expectedResult, actualResult);
    }

    public static Stream<Arguments> testGetUsedSpace() throws IOException {
        return Stream.of(
        // @formatter:off
            Arguments.of(tempDir.toString(), LocalTime.now(), 10, 10),
            Arguments.of(tempDir.toString(), LocalTime.now().minusMinutes(10), 200, 200),
            Arguments.of(tempDir.toString(), LocalTime.now().minusMinutes(50), 0, getSizeOfDir(tempDir)),
            Arguments.of("?!TEST", LocalTime.now().minusMinutes(50), 0, 0)
        // @formatter:on
        );
    }

    private static long getSizeOfDir(Path path) throws IOException {
        long sizeOfDir = Files.walk(path)
            .mapToLong(p -> p.toFile()
                .length())
            .sum();
        return sizeOfDir;
    }

}
