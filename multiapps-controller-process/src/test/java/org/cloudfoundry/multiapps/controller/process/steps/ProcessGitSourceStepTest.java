package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.helpers.DescriptorParserFacadeFactory;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.handlers.DescriptorParserFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

class ProcessGitSourceStepTest extends SyncFlowableStepTest<ProcessGitSourceStep> {

    private static final String PROCESS_INSTANCE_ID = "1234";

    @Override
    protected ProcessGitSourceStep createStep() {
        return new ProcessGitSourceStep() {

            @Override
            protected StepLogger getStepLogger() {
                return stepLogger;
            }

        };
    }

    @Test
    void getGitUriTest() throws SLException {
        String gitUri = "https://somehost.com/somerepo/.git";
        context.setVariable(Variables.GIT_URI, gitUri);
        assertEquals(gitUri, step.getGitUri(context));
    }

    @Test
    void testExtractRepoName() {
        String somerepoName = step.extractRepoName("https://somehost.com/somerepo/.git", PROCESS_INSTANCE_ID);
        assertEquals("somerepo" + PROCESS_INSTANCE_ID, somerepoName);

        String otherrepoName = step.extractRepoName("https://somehost.com/otherrepo", PROCESS_INSTANCE_ID);
        assertEquals("otherrepo" + PROCESS_INSTANCE_ID, otherrepoName);
    }

    public static Stream<Arguments> testZipRepoContent() {
        return Stream.of(
        // @formatter:off
                // (0) META-INF contains MANIFEST.MF but not mtad.yaml => should create mta-assembly:
                Arguments.of("test-repo-01"),
                // (1) META-INF contains MANIFEST.MF and mtad.yaml => should only zip dir:
                Arguments.of("test-repo-02"),
                // (2) Repo doesn't contain META-INF => should create mta-assembly:
                Arguments.of("test-repo-03")
                // @formatter:on
        );
    }

    // Repo contains MANIFEST.MF but not mtad.yaml
    @ParameterizedTest
    @MethodSource
    void testZipRepoContent(String repository) throws Exception {
        DescriptorParserFacadeFactory descriptorParserFacadeFactory = Mockito.mock(DescriptorParserFacadeFactory.class);
        Mockito.when(descriptorParserFacadeFactory.getInstance())
               .thenReturn(new DescriptorParserFacade());
        step.descriptorParserFactory = descriptorParserFacadeFactory;

        Path repoDir = Paths.get(getClass().getResource(repository)
                                           .toURI());
        Path mtarZip = null;
        try {
            mtarZip = step.zipRepoContent(repoDir.toAbsolutePath());
            URI jarMtarUri = URI.create("jar:" + mtarZip.toAbsolutePath()
                                                        .toUri()
                                                        .toString());
            try (FileSystem mtarFS = FileSystems.newFileSystem(jarMtarUri, new HashMap<>())) {
                Path mtarRoot = mtarFS.getRootDirectories()
                                      .iterator()
                                      .next();
                assertFalse(Files.exists(mtarRoot.resolve(".git")));
                assertFalse(Files.exists(mtarRoot.resolve(".gitignore")));
                assertTrue(Files.exists(mtarRoot.resolve("a/cool-script.script")));
                assertTrue(Files.exists(mtarRoot.resolve("META-INF/mtad.yaml")));
                assertTrue(Files.exists(mtarRoot.resolve("META-INF/MANIFEST.MF")));
            }
        } finally {
            if (mtarZip != null) {
                Files.deleteIfExists(mtarZip);
            }
        }
    }
}
