package org.cloudfoundry.multiapps.controller.web.monitoring;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FssMonitorTest {

    private static Path tempDir;

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
        MockitoAnnotations.openMocks(this);
        ApplicationConfiguration appConfigurations = new ApplicationConfiguration();
        fssMonitor = new FssMonitor(appConfigurations);
    }

    @ParameterizedTest
    @MethodSource
    void testGetUsedSpace(File path, LocalDateTime lastCheck, long cachedValue, long expectedResult) {
        fssMonitor.updateTimesMap.put(path, lastCheck);
        fssMonitor.usedSpaceMap.put(path, cachedValue);
        long actualResult = fssMonitor.calculateUsedSpace(path.getAbsolutePath());
        assertEquals(expectedResult, actualResult);
    }

    static Stream<Arguments> testGetUsedSpace() throws IOException {
        return Stream.of(Arguments.of(tempDir.toFile(), LocalDateTime.now(), 10, 10), Arguments.of(tempDir.toFile(), LocalDateTime.now()
                                                                                                                                  .minusMinutes(
                                                                                                                                      10),
                                                                                                   200, 200),
                         Arguments.of(tempDir.toFile(), LocalDateTime.now()
                                                                     .minusMinutes(50),
                                      0, getSizeOfDir(tempDir)));
    }

    private static long getSizeOfDir(Path filePath) {
        return FileUtils.sizeOf(filePath.toFile());
    }

}
