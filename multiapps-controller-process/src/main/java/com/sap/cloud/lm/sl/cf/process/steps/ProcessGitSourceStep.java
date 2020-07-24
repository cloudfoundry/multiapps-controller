package com.sap.cloud.lm.sl.cf.process.steps;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.FilenameUtils;
import org.cloudfoundry.multiapps.common.ContentException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.core.helpers.DescriptorParserFacadeFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaArchiveBuilder;
import com.sap.cloud.lm.sl.cf.core.util.FileUtils;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.util.GitRepoCloner;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

// Should be executed before ValidateDeployParametersStep as the archive ID is determined during this step execution
@Named("processGitSourceStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ProcessGitSourceStep extends SyncFlowableStep {

    private static final String SKIP_SSL_GIT_CONFIG = ".skipSslGitConfig";
    private static final String PATH_SEPARATOR = "/";
    private static final String REPOSITORY_DIRECTORY_NAME = "repos";
    private static final String MTAR_EXTENTION = ".mtar";
    public static final String META_INF_PATH = "META-INF";
    private static final String MANIFEST_PATH = "MANIFEST.MF";
    private static final String MTAD_PATH = "mtad.yaml";

    @Inject
    protected DescriptorParserFacadeFactory descriptorParserFactory;

    @Override
    protected StepPhase executeStep(ProcessContext context) throws IOException, GitAPIException, FileStorageException {
        getStepLogger().info(Messages.DOWNLOADING_DEPLOYABLE);

        final String gitUri = getGitUri(context);
        final String gitRepoPath = context.getVariable(Variables.GIT_REPO_PATH);
        String processId = context.getExecution()
                                  .getProcessInstanceId();
        final String repoName = extractRepoName(gitUri, processId);
        final Path reposDir = Paths.get(REPOSITORY_DIRECTORY_NAME, repoName);
        Path gitConfigFilePath = generateGitConfigFilepath(processId);
        if (!reposDir.toFile()
                     .exists()) {
            Files.createDirectories(reposDir);
        }
        Path mtarZip = null;
        try {

            GitRepoCloner cloner = createCloner(context);
            getStepLogger().info(Messages.CLONING_REPOSITORY, gitUri);
            cloner.cloneRepo(gitUri, reposDir);
            final Path mtaRepoPath = reposDir.resolve(gitRepoPath)
                                             .normalize();
            mtarZip = zipRepoContent(mtaRepoPath);
            uploadZipToDB(context, mtarZip);
        } finally {
            try {
                deleteTemporaryRepositoryDirectory(reposDir);
                if (gitConfigFilePath.toFile()
                                     .exists()) {
                    Files.delete(gitConfigFilePath);
                }
                if (mtarZip != null && mtarZip.toFile()
                                              .exists()) {
                    FileUtils.deleteDirectory(mtarZip);
                }
            } catch (IOException e) {
                // ignore such cases
            }
        }
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_DOWNLOADING_DEPLOYABLE_FROM_GIT;
    }

    private GitRepoCloner createCloner(ProcessContext context) {
        DelegateExecution execution = context.getExecution();
        GitRepoCloner cloner = new GitRepoCloner();
        cloner.setRefName(StepsUtil.getGitRepoRef(context));
        cloner.setGitConfigFilePath(generateGitConfigFilepath(execution.getProcessInstanceId()));
        cloner.setSkipSslValidation(context.getVariable(Variables.GIT_SKIP_SSL));
        return cloner;
    }

    protected String getGitUri(ProcessContext context) {
        String gitUriParam = StepsUtil.getGitRepoUri(context);
        try {
            return new URL(gitUriParam).toString();
        } catch (MalformedURLException e) {
            throw new ContentException(e, Messages.GIT_URI_IS_NOT_SPECIFIED);
        }
    }

    private Path generateGitConfigFilepath(String processId) {
        return Paths.get(REPOSITORY_DIRECTORY_NAME, SKIP_SSL_GIT_CONFIG + processId);
    }

    protected String extractRepoName(String gitUri, String processId) {
        if (!gitUri.endsWith(PATH_SEPARATOR + org.eclipse.jgit.lib.Constants.DOT_GIT)) {
            return gitUri.substring(gitUri.lastIndexOf(PATH_SEPARATOR) + 1) + processId;
        }
        String repoLocation = gitUri.substring(0, gitUri.lastIndexOf(PATH_SEPARATOR + org.eclipse.jgit.lib.Constants.DOT_GIT));
        return repoLocation.substring(repoLocation.lastIndexOf(PATH_SEPARATOR) + 1) + processId;
    }

    protected Path zipRepoContent(final Path mtaPath) throws IOException {
        getStepLogger().info(Messages.COMPRESSING_MTA_CONTENT);
        getStepLogger().debug("Zipping content of repo dir" + mtaPath.toAbsolutePath());
        if (directoryContainsManifest(mtaPath)) {
            getStepLogger().info("Detected manifest, will zip the provided directory and deploy it");
            return zipMtaFolder(mtaPath);
        } else {
            MtaArchiveBuilder mtaBuilder = new MtaArchiveBuilder(mtaPath, descriptorParserFactory.getInstance());
            return mtaBuilder.buildMtaArchive();
        }
    }

    protected void uploadZipToDB(ProcessContext context, final Path mtarZip) throws FileStorageException, IOException {
        getStepLogger().info(Messages.UPLOADING_MTAR);
        getStepLogger().debug("uploading file " + mtarZip.toAbsolutePath()
                                                         .toString()
            + " to DB");
        String spaceId = context.getVariable(Variables.SPACE_GUID);
        try (InputStream mtarInputStream = Files.newInputStream(mtarZip)) {
            String serviceId = context.getVariable(Variables.SERVICE_ID);
            String mtarName = mtarZip.getFileName()
                                     .toString();
            FileEntry entry = fileService.addFile(spaceId, serviceId, mtarName, mtarInputStream);
            String uploadedMtarId = entry.getId();
            context.setVariable(Variables.APP_ARCHIVE_ID, uploadedMtarId);
        }
        getStepLogger().debug(Messages.MTAR_UPLOADED);
    }

    protected Path zipMtaFolder(final Path mtaPath) throws IOException {
        final Path zipFilePath = Paths.get(mtaPath.toString() + MTAR_EXTENTION);
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFilePath.toFile()))) {
            Files.walkFileTree(mtaPath, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (shouldOmitFile(file)) {
                        return FileVisitResult.CONTINUE;
                    }
                    String pathName = getPathName(mtaPath, file);
                    zipOutputStream.putNextEntry(new ZipEntry(pathName));
                    Files.copy(file, zipOutputStream);
                    zipOutputStream.closeEntry();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (shouldOmitDirectory(dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    String pathName = getPathName(mtaPath, dir) + PATH_SEPARATOR;
                    zipOutputStream.putNextEntry(new ZipEntry(pathName));
                    zipOutputStream.closeEntry();
                    return FileVisitResult.CONTINUE;
                }

            });
        }
        return zipFilePath;
    }

    private boolean shouldOmitDirectory(Path dir) {
        return dir.toFile()
                  .getName()
                  .equals(org.eclipse.jgit.lib.Constants.DOT_GIT);
    }

    private boolean shouldOmitFile(Path file) {
        return file.toFile()
                   .getName()
                   .equals(org.eclipse.jgit.lib.Constants.DOT_GIT_IGNORE);
    }

    private String getPathName(Path parentFolder, Path fileToAppend) {
        Path relativePath = parentFolder.relativize(fileToAppend);
        return FilenameUtils.separatorsToUnix(relativePath.toString());
    }

    private boolean directoryContainsManifest(Path mtaPath) {
        Path metaInfPath = mtaPath.resolve(META_INF_PATH);
        File manifestFile = metaInfPath.resolve(MANIFEST_PATH)
                                       .toFile();
        File mtadFile = metaInfPath.resolve(MTAD_PATH)
                                   .toFile();
        return manifestFile.exists() && mtadFile.exists();
    }

    private void deleteTemporaryRepositoryDirectory(Path clonedRepoDir) throws IOException {

        // Workaround in JGit for deleting a cloned repository directory
        org.eclipse.jgit.util.FileUtils.delete(clonedRepoDir.toFile(),
                                               org.eclipse.jgit.util.FileUtils.RETRY | org.eclipse.jgit.util.FileUtils.RECURSIVE);
    }
}
