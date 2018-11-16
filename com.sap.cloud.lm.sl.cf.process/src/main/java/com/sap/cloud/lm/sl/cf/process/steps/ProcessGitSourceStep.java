package com.sap.cloud.lm.sl.cf.process.steps;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.URIBuilder;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudInfoExtended;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaArchiveBuilder;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.FileUtils;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.persistence.util.Configuration;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.GitRepoCloner;
import com.sap.cloud.lm.sl.common.SLException;

// Should be executed before ValidateDeployParametersStep as the archive ID is determined during this step execution
@Component("processGitSourceStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ProcessGitSourceStep extends SyncFlowableStep {

    private static final String SKIP_SSL_GIT_CONFIG = ".skipSslGitConfig";
    private static final String PATH_SEPARATOR = "/";
    private static final String GIT_SERVICE_URL_KEY = "git-service";
    private static final String REPOSITORY_DIRECTORY_NAME = "repos";
    private static final String MTAR_EXTENTION = ".mtar";
    public static final String META_INF_PATH = "META-INF";
    private static final String MANIFEST_PATH = "MANIFEST.MF";
    private static final String MTAD_PATH = "mtad.yaml";

    @Inject
    private ApplicationConfiguration configuration;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        try {
            getStepLogger().info(Messages.DOWNLOADING_DEPLOYABLE);

            final String gitUri = getGitUri(execution);
            final String gitRepoPath = (String) execution.getContext()
                .getVariable(Constants.PARAM_GIT_REPO_PATH);
            String processId = execution.getContext()
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

                GitRepoCloner cloner = createCloner(execution);
                getStepLogger().info(Messages.CLONING_REPOSITORY, gitUri);
                cloner.cloneRepo(gitUri, reposDir);
                final Path mtaRepoPath = reposDir.resolve(gitRepoPath)
                    .normalize();
                mtarZip = zipRepoContent(mtaRepoPath);
                uploadZipToDB(execution.getContext(), mtarZip);
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
        } catch (SLException e) {
            getStepLogger().error(e.getMessage());
            throw e;
        } catch (GitAPIException | IOException | FileStorageException e) {
            getStepLogger().error(e, Messages.ERROR_DOWNLOADING_DEPLOYABLE_FROM_GIT);
            throw new SLException(e, Messages.ERROR_PROCESSING_GIT_MTA_SOURCE);
        }
    }

    private GitRepoCloner createCloner(ExecutionWrapper execution) {
        DelegateExecution context = execution.getContext();
        GitRepoCloner cloner = new GitRepoCloner();
        cloner.setGitServiceUrlString(getGitServiceUrl(execution));
        cloner.setRefName(StepsUtil.getGitRepoRef(context));
        cloner.setGitConfigFilePath(generateGitConfigFilepath(context.getProcessInstanceId()));
        cloner.setSkipSslValidation((boolean) context.getVariable(Constants.PARAM_GIT_SKIP_SSL));
        String userName = StepsUtil.determineCurrentUser(context, getStepLogger());
        String token;
        try {
            token = clientProvider.getValidToken(userName)
                .getValue();
            cloner.setCredentials(userName, token);
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_RETRIEVING_OAUT_TOKEN);
            throw e;
        }
        return cloner;
    }

    protected String getGitUri(ExecutionWrapper execution) {
        String gitUriParam = StepsUtil.getGitRepoUri(execution.getContext());
        try {
            return new URL(gitUriParam).toString();
        } catch (MalformedURLException e) {
            String gitServiceUrl = getGitServiceUrl(execution);
            return buildUriFromRepositoryName(gitUriParam, gitServiceUrl);
        }
    }

    protected String buildUriFromRepositoryName(String gitUriParam, String gitServiceUrl) {
        try {
            URIBuilder gitUriBuilder = new URIBuilder(gitServiceUrl);
            gitUriBuilder.setPath(PATH_SEPARATOR + gitUriParam);
            return gitUriBuilder.toString();
        } catch (URISyntaxException e) {
            throw new SLException(e, Messages.ERROR_PROCESSING_GIT_URI);
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

    private String getGitServiceUrl(ExecutionWrapper execution) {
        if (!isClientExtensionsAvailable(execution)) {
            return null;
        }
        CloudInfoExtended info = getCloudInfoExtended(execution);
        return info.getServiceUrl(GIT_SERVICE_URL_KEY);
    }

    private CloudInfoExtended getCloudInfoExtended(ExecutionWrapper execution) {
        return (CloudInfoExtended) execution.getControllerClient()
            .getCloudInfo();
    }

    private boolean isClientExtensionsAvailable(ExecutionWrapper execution) {
        CloudControllerClient client = execution.getControllerClient();
        return client.getCloudInfo() instanceof CloudInfoExtended;
    }

    protected Path zipRepoContent(final Path mtaPath) throws IOException {
        getStepLogger().info(Messages.COMPRESSING_MTA_CONTENT);
        getStepLogger().debug("Zipping content of repo dir" + mtaPath.toAbsolutePath());
        if (directoryContainsManifest(mtaPath)) {
            getStepLogger().info("Detected manifest, will zip the provided directory and deploy it");
            return zipMtaFolder(mtaPath);
        } else {
            MtaArchiveBuilder mtaBuilder = new MtaArchiveBuilder(mtaPath);
            return mtaBuilder.buildMtaArchive();
        }
    }

    protected void uploadZipToDB(DelegateExecution context, final Path mtarZip) throws FileStorageException, IOException {
        InputStream mtarInputStream = null;
        getStepLogger().info(Messages.UPLOADING_MTAR);
        getStepLogger().debug("uploading file " + mtarZip.toAbsolutePath()
            .toString() + " to DB");
        try {
            Configuration fileConfiguration = configuration.getFileConfiguration();
            String spaceId = StepsUtil.getSpaceId(context);
            mtarInputStream = Files.newInputStream(mtarZip);
            String serviceId = StepsUtil.getServiceId(context);
            String mtarName = mtarZip.getFileName()
                .toString();
            FileEntry entry = fileService.addFile(spaceId, serviceId, mtarName, fileConfiguration.getFileUploadProcessor(),
                mtarInputStream);
            String uploadedMtarId = entry.getId();
            StepsUtil.setArchiveFileId(context, uploadedMtarId);
        } finally {
            IOUtils.closeQuietly(mtarInputStream);
        }
        getStepLogger().debug(Messages.MTAR_UPLOADED);
    }

    protected Path zipMtaFolder(final Path mtaPath) throws IOException {
        final Path zipFilePath = Paths.get(mtaPath.toString() + MTAR_EXTENTION);
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFilePath.toFile()))) {
            Files.walkFileTree(mtaPath, new SimpleFileVisitor<Path>() {
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (shouldOmmitFile(file)) {
                        return FileVisitResult.CONTINUE;
                    }
                    String pathName = getPathName(mtaPath, file);
                    zipOutputStream.putNextEntry(new ZipEntry(pathName));
                    Files.copy(file, zipOutputStream);
                    zipOutputStream.closeEntry();
                    return FileVisitResult.CONTINUE;
                }

                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (shouldOmmitDirectory(dir)) {
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

    private boolean shouldOmmitDirectory(Path dir) {
        return dir.toFile()
            .getName()
            .equals(org.eclipse.jgit.lib.Constants.DOT_GIT);
    }

    private boolean shouldOmmitFile(Path file) {
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
