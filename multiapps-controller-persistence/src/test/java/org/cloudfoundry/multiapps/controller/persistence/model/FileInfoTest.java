package org.cloudfoundry.multiapps.controller.persistence.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;

import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileInfoTest {

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("file-info-test");
    }

    @AfterEach
    void tearDown() throws IOException {
        try (var stream = Files.walk(tempDir)) {
            stream.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                  .forEach(p -> p.toFile()
                                 .delete());
        }
    }

    @Test
    void testGetInputStreamReadsFromUnderlyingFile() throws Exception {
        Path file = tempDir.resolve("payload.bin");
        Files.writeString(file, "hello");

        FileInfo info = ImmutableFileInfo.builder()
                                         .size(BigInteger.valueOf(5))
                                         .digest("digest")
                                         .digestAlgorithm("SHA-256")
                                         .file(file.toFile())
                                         .build();

        try (InputStream in = info.getInputStream()) {
            Assertions.assertEquals("hello", new String(in.readAllBytes()));
        }
    }

    @Test
    void testGetInputStreamWrapsFileNotFoundInFileStorageException() {
        File missing = tempDir.resolve("does-not-exist").toFile();

        FileInfo info = ImmutableFileInfo.builder()
                                         .size(BigInteger.ZERO)
                                         .digest("digest")
                                         .digestAlgorithm("SHA-256")
                                         .file(missing)
                                         .build();

        Assertions.assertThrows(FileStorageException.class, info::getInputStream);
    }
}
