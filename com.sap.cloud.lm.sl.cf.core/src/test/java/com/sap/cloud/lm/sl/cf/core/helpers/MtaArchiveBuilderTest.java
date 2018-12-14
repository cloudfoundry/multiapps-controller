package com.sap.cloud.lm.sl.cf.core.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.util.FileUtils;
import com.sap.cloud.lm.sl.common.SLException;

@RunWith(Parameterized.class)
public class MtaArchiveBuilderTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private MtaArchiveBuilder mtaArchiveBuilder;
    private String path;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) testNoDeploymentDescriptor
             { "src/test/resources/com/sap/cloud/lm/sl/cf/core/helpers/mta-dir2/",
                MessageFormat.format(Messages.DIRECTORY_0_DOES_NOT_CONTAIN_MANDATORY_DEPLOYMENT_DESCRIPTOR_FILE_1, "mta-dir2", "mtad.yaml")
             }, 
             // (1) testCannotReadDeploymentDescriptor
             {
                 //TODO ask what should code do when someone is trying to use version 1
                "src/test/resources/com/sap/cloud/lm/sl/cf/core/helpers/mta-dir3",
                MessageFormat.format(Messages.FAILED_TO_READ_DEPLOYMENT_DESCRIPTOR_0,
                Paths.get("src/test/resources/com/sap/cloud/lm/sl/cf/core/helpers/mta-dir3/mtad.yaml").toAbsolutePath())
             }, 
             // (2) testNotSupportedMtaVersion
             {
                "src/test/resources/com/sap/cloud/lm/sl/cf/core/helpers/mta-dir4",
                MessageFormat.format(Messages.THE_DEPLOYMENT_DESCRIPTOR_0_SPECIFIES_NOT_SUPPORTED_MTA_VERSION_1,
                Paths.get("src/test/resources/com/sap/cloud/lm/sl/cf/core/helpers/mta-dir4/mtad.yaml").toAbsolutePath(), "1")
             },
             // (3) testNotNormalizedModulePath
             {
                "src/test/resources/com/sap/cloud/lm/sl/cf/core/helpers/mta-dir5",
                MessageFormat.format(com.sap.cloud.lm.sl.cf.core.message.Messages.PATH_SHOULD_BE_NORMALIZED, "../web/")
                
             },
             // (4) testNotNormalizedResourceConfigPath
             { 
                "src/test/resources/com/sap/cloud/lm/sl/cf/core/helpers/mta-dir6",
                MessageFormat.format(com.sap.cloud.lm.sl.cf.core.message.Messages.PATH_SHOULD_BE_NORMALIZED, "../xs-security.json")
             },
             // (5) testNotExistingModulePath
             {
                "src/test/resources/com/sap/cloud/lm/sl/cf/core/helpers/mta-dir7",
                MessageFormat.format(Messages.PATH_IS_RESOLVED_TO_NOT_EXISTING_FILE, "web1/",
                Paths.get("src/test/resources/com/sap/cloud/lm/sl/cf/core/helpers/mta-dir7/web1/").toAbsolutePath())
             },
             // (6) testNotExistingResourceConfigPath
             {
                "src/test/resources/com/sap/cloud/lm/sl/cf/core/helpers/mta-dir8",
                MessageFormat.format(Messages.PATH_IS_RESOLVED_TO_NOT_EXISTING_FILE, "config",
                Paths.get("src/test/resources/com/sap/cloud/lm/sl/cf/core/helpers/mta-dir8/config/").toAbsolutePath())
             },
             // (7) testWindowsPathSeparatorsInPathsIsInvalid
             {
                "src/test/resources/com/sap/cloud/lm/sl/cf/core/helpers/mta-dir9",
                MessageFormat.format(com.sap.cloud.lm.sl.cf.core.message.Messages.PATH_MUST_NOT_CONTAIN_WINDOWS_SEPARATORS, "web\\")
             },
             // (8) testAbsolutePathIsInvalid
             { isWindows() ? "src/test/resources/com/sap/cloud/lm/sl/cf/core/helpers/mta-dir11/" :
                "src/test/resources/com/sap/cloud/lm/sl/cf/core/helpers/mta-dir10",
               MessageFormat.format(com.sap.cloud.lm.sl.cf.core.message.Messages.PATH_SHOULD_NOT_BE_ABSOLUTE, isWindows() ? "C:/web" : "/web/asd")
             }, 
             // (9) working scenario
             { 
                 "src/test/resources/com/sap/cloud/lm/sl/cf/core/helpers/mta-dir/", 
                 null
             }
// @formatter:on
        });
    }

    public MtaArchiveBuilderTest(String path, String expectedExceptionMessage) {
        this.path = path;
        prepareException(expectedExceptionMessage);
    }

    @Test
    public void test() throws Exception {
        initMtaArchiveBuilder(Paths.get(path));
        Path mtaArchiveFile = mtaArchiveBuilder.buildMtaArchive();

        // test working scenario
        assertEquals(Paths.get("src/test/resources/com/sap/cloud/lm/sl/cf/core/helpers/mta-dir/mta-assembly/mta-dir.mtar")
            .toAbsolutePath(), mtaArchiveFile.toAbsolutePath());
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
        List<String> fileNames = Arrays.asList("web", "js", "backend-hdi-params.json", "db", "xs-security.json", "mtad.yaml");
        assertEquals(fileNames.size(), jarEntries.size());
        for (String file : fileNames) {
            assertTrue(jarEntries.stream()
                .anyMatch(jarEntry -> jarEntry.getFileName()
                    .toString()
                    .equals(file)));
        }

        assertExistingJarFile(mtaArchiveFile, MtaArchiveBuilder.DEPLOYMENT_DESCRIPTOR_ARCHIVE_PATH,
            new String(Files.readAllBytes(Paths.get("src/test/resources/com/sap/cloud/lm/sl/cf/core/helpers/mtad.yaml"))));
        assertExistingJarDirectory(mtaArchiveFile, "db/");
        assertExistingJarDirectory(mtaArchiveFile, "db/src/");
        assertExistingJarFile(mtaArchiveFile, "db/package.json",
            new String(Files.readAllBytes(Paths.get("src/test/resources/com/sap/cloud/lm/sl/cf/core/helpers/package.json"))));
    }

    private void prepareException(String message) {
        if (message != null) {
            expectedException.expect(SLException.class);
            expectedException.expectMessage(message);
        }
    }

    @After
    public void cleanUp() throws Exception {
        if (mtaArchiveBuilder != null && Files.exists(mtaArchiveBuilder.getMtaAssemblyDir())) {
            FileUtils.deleteDirectory(mtaArchiveBuilder.getMtaAssemblyDir());
        }
    }

    private void initMtaArchiveBuilder(Path mtaDir) throws Exception {
        this.mtaArchiveBuilder = new MtaArchiveBuilder(mtaDir);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name")
            .startsWith("Windows");
    }

    private void assertExistingJarDirectory(Path mtaArchiveFile, String dirName) throws Exception {
        try (JarInputStream in = new JarInputStream(Files.newInputStream(mtaArchiveFile))) {
            for (ZipEntry e; (e = in.getNextEntry()) != null;) {
                if (dirName.equals(e.getName()) && e.isDirectory()) {
                    return;
                }
            }
            throw new AssertionError(MessageFormat.format("Zip archive directory \"{0}\" not found", dirName));
        }
    }

    private void assertExistingJarFile(Path mtaArchiveFile, String fileName, String expectedContent) throws Exception {
        try (JarInputStream in = new JarInputStream(Files.newInputStream(mtaArchiveFile))) {
            for (ZipEntry e; (e = in.getNextEntry()) != null;) {
                if (fileName.equals(e.getName()) && !e.isDirectory()) {
                    StringBuilder textBuilder = new StringBuilder();
                    try (Reader reader = new BufferedReader(new InputStreamReader(in))) {
                        int c = 0;
                        while ((c = reader.read()) != -1) {
                            textBuilder.append((char) c);
                        }
                    }
                    assertEquals(expectedContent, textBuilder.toString());
                    return;
                }
            }
            throw new AssertionError(MessageFormat.format("Zip archive file \"{0}\" could not be found", fileName));
        }
    }

}
