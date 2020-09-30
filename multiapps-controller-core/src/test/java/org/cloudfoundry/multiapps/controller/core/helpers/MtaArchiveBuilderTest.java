package org.cloudfoundry.multiapps.controller.core.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.apache.commons.io.IOUtils;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.util.FileUtils;
import org.cloudfoundry.multiapps.mta.handlers.DescriptorParserFacade;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MtaArchiveBuilderTest {

    private MtaArchiveBuilder mtaArchiveBuilder;

    public static Stream<Arguments> test() {
        return Stream.of(
// @formatter:off
            // (0) testNoDeploymentDescriptor
             Arguments.of("src/test/resources/org/cloudfoundry/multiapps/controller/core/helpers/mta-dir2/",
                     MessageFormat.format(Messages.DIRECTORY_0_DOES_NOT_CONTAIN_MANDATORY_DEPLOYMENT_DESCRIPTOR_FILE_1, "mta-dir2", "mtad.yaml"), true),
             // (1) testCannotReadDeploymentDescriptor
             Arguments.of("src/test/resources/org/cloudfoundry/multiapps/controller/core/helpers/mta-dir3",
                     MessageFormat.format(Messages.FAILED_TO_READ_DEPLOYMENT_DESCRIPTOR_0,
                             Paths.get("src/test/resources/org/cloudfoundry/multiapps/controller/core/helpers/mta-dir3/mtad.yaml").toAbsolutePath()), true), 
             // (2) testNotSupportedMtaVersion
             Arguments.of("src/test/resources/org/cloudfoundry/multiapps/controller/core/helpers/mta-dir4",
                     MessageFormat.format(Messages.THE_DEPLOYMENT_DESCRIPTOR_0_SPECIFIES_NOT_SUPPORTED_MTA_VERSION_1,
                             Paths.get("src/test/resources/org/cloudfoundry/multiapps/controller/core/helpers/mta-dir4/mtad.yaml").toAbsolutePath(), "1"), true),
             // (3) testNotNormalizedModulePath
             Arguments.of("src/test/resources/org/cloudfoundry/multiapps/controller/core/helpers/mta-dir5",
                     MessageFormat.format(org.cloudfoundry.multiapps.controller.core.Messages.PATH_SHOULD_BE_NORMALIZED, "../web/"), false),
             // (4) testNotNormalizedResourceConfigPath
             Arguments.of("src/test/resources/org/cloudfoundry/multiapps/controller/core/helpers/mta-dir6",
                     MessageFormat.format(org.cloudfoundry.multiapps.controller.core.Messages.PATH_SHOULD_BE_NORMALIZED, "../xs-security.json"), false),
             // (5) testNotExistingModulePath
             Arguments.of("src/test/resources/org/cloudfoundry/multiapps/controller/core/helpers/mta-dir7",
                     MessageFormat.format(Messages.PATH_IS_RESOLVED_TO_NOT_EXISTING_FILE, "web1/",
                             Paths.get("src/test/resources/org/cloudfoundry/multiapps/controller/core/helpers/mta-dir7/web1/").toAbsolutePath()), false),
             // (6) testNotExistingResourceConfigPath
             Arguments.of("src/test/resources/org/cloudfoundry/multiapps/controller/core/helpers/mta-dir8",
                     MessageFormat.format(Messages.PATH_IS_RESOLVED_TO_NOT_EXISTING_FILE, "config",
                             Paths.get("src/test/resources/org/cloudfoundry/multiapps/controller/core/helpers/mta-dir8/config/").toAbsolutePath()), false),
             // (7) testWindowsPathSeparatorsInPathsIsInvalid
             Arguments.of("src/test/resources/org/cloudfoundry/multiapps/controller/core/helpers/mta-dir9",
                     MessageFormat.format(org.cloudfoundry.multiapps.controller.core.Messages.PATH_MUST_NOT_CONTAIN_WINDOWS_SEPARATORS, "web\\"), false),
             // (8) testAbsolutePathIsInvalid
             Arguments.of(isWindows() ? "src/test/resources/org/cloudfoundry/multiapps/controller/core/helpers/mta-dir11/" :
                             "src/test/resources/org/cloudfoundry/multiapps/controller/core/helpers/mta-dir10",
                     MessageFormat.format(org.cloudfoundry.multiapps.controller.core.Messages.PATH_SHOULD_NOT_BE_ABSOLUTE, isWindows() ? "C:/web" : "/web/asd"), false), 
             // (9) working scenario
             Arguments.of("src/test/resources/org/cloudfoundry/multiapps/controller/core/helpers/mta-dir/",
                     null, false)
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void test(String pathString, String expectedExceptionMessage, boolean exceptionThrownDuringMtaArchiveBuilderInitialization)
        throws IOException {
        if (exceptionThrownDuringMtaArchiveBuilderInitialization) {
            Path path = Paths.get(pathString);
            Exception exception = assertThrows(SLException.class, () -> initMtaArchiveBuilder(path));
            assertEquals(expectedExceptionMessage, exception.getMessage());
            return;
        }
        initMtaArchiveBuilder(Paths.get(pathString));
        if (expectedExceptionMessage != null) {
            Exception exception = assertThrows(SLException.class, () -> mtaArchiveBuilder.buildMtaArchive());
            assertEquals(expectedExceptionMessage, exception.getMessage());
            return;
        }
        Path mtaArchiveFile = mtaArchiveBuilder.buildMtaArchive();

        // test working scenario
        assertEquals(Paths.get("src/test/resources/org/cloudfoundry/multiapps/controller/core/helpers/mta-dir/mta-assembly/mta-dir.mtar")
                          .toAbsolutePath(),
                     mtaArchiveFile.toAbsolutePath());
        assertEquals(6, mtaArchiveBuilder.getManifestEntries()
                                         .size());
        try (JarInputStream in = new JarInputStream(Files.newInputStream(mtaArchiveFile))) {
            Manifest manifest = in.getManifest();
            assertTrue(manifest.getEntries()
                               .containsKey(MtaArchiveBuilder.DEPLOYMENT_DESCRIPTOR_ARCHIVE_PATH));
            assertEquals("node-hello-world", manifest.getEntries()
                                                     .get("web/")
                                                     .getValue(MtaArchiveHelper.ATTR_MTA_MODULE));
            assertEquals("node-hello-world-backend", manifest.getEntries()
                                                             .get("js/")
                                                             .getValue(MtaArchiveHelper.ATTR_MTA_MODULE));
            assertEquals("node-hello-world-db", manifest.getEntries()
                                                        .get("db/")
                                                        .getValue(MtaArchiveHelper.ATTR_MTA_MODULE));
            assertEquals("nodejs-uaa", manifest.getEntries()
                                               .get("xs-security.json")
                                               .getValue(MtaArchiveHelper.ATTR_MTA_RESOURCE));
            assertEquals("node-hello-world-backend/nodejs-hdi-container", manifest.getEntries()
                                                                                  .get("backend-hdi-params.json")
                                                                                  .getValue(MtaArchiveHelper.ATTR_MTA_REQUIRES_DEPENDENCY));
        }
        List<Path> jarEntries = mtaArchiveBuilder.getJarEntries();
        List<String> fileNames = List.of("web", "js", "backend-hdi-params.json", "db", "xs-security.json", "mtad.yaml");
        assertEquals(fileNames.size(), jarEntries.size());
        for (String file : fileNames) {
            assertTrue(jarEntries.stream()
                                 .anyMatch(jarEntry -> jarEntry.getFileName()
                                                               .toString()
                                                               .equals(file)));
        }

        assertExistingJarFile(mtaArchiveFile, MtaArchiveBuilder.DEPLOYMENT_DESCRIPTOR_ARCHIVE_PATH,
                              Files.readAllBytes(Paths.get("src/test/resources/org/cloudfoundry/multiapps/controller/core/helpers/mtad.yaml")));
        assertExistingJarDirectory(mtaArchiveFile, "db/");
        assertExistingJarDirectory(mtaArchiveFile, "db/src/");
        assertExistingJarFile(mtaArchiveFile, "db/package.json",
                              Files.readAllBytes(Paths.get("src/test/resources/org/cloudfoundry/multiapps/controller/core/helpers/package.json")));
    }

    @AfterEach
    void cleanUp() throws Exception {
        if (mtaArchiveBuilder != null && Files.exists(mtaArchiveBuilder.getMtaAssemblyDir())) {
            FileUtils.deleteDirectory(mtaArchiveBuilder.getMtaAssemblyDir());
        }
    }

    private void initMtaArchiveBuilder(Path mtaDir) {
        this.mtaArchiveBuilder = new MtaArchiveBuilder(mtaDir, new DescriptorParserFacade());
    }

    private static boolean isWindows() {
        return System.getProperty("os.name")
                     .startsWith("Windows");
    }

    private void assertExistingJarDirectory(Path mtaArchiveFile, String dirName) throws IOException {
        try (JarInputStream in = new JarInputStream(Files.newInputStream(mtaArchiveFile))) {
            for (ZipEntry e; (e = in.getNextEntry()) != null;) {
                if (dirName.equals(e.getName()) && e.isDirectory()) {
                    return;
                }
            }
            throw new AssertionError(MessageFormat.format("Zip archive directory \"{0}\" not found", dirName));
        }
    }

    private void assertExistingJarFile(Path mtaArchiveFile, String fileName, byte[] expectedContent) throws IOException {
        try (JarInputStream in = new JarInputStream(Files.newInputStream(mtaArchiveFile))) {
            for (ZipEntry e; (e = in.getNextEntry()) != null;) {
                if (fileName.equals(e.getName()) && !e.isDirectory()) {
                    Assertions.assertArrayEquals(expectedContent, IOUtils.toByteArray(in));
                    return;
                }
            }
            throw new AssertionError(MessageFormat.format("Zip archive file \"{0}\" could not be found", fileName));
        }
    }

}
