package com.sap.cloud.lm.sl.cf.process.steps;

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

import org.activiti.engine.delegate.DelegateExecution;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.URIBuilder;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudInfoExtended;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaArchiveBuilder;
import com.sap.cloud.lm.sl.cf.core.util.Configuration;
import com.sap.cloud.lm.sl.cf.core.util.FileUtils;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.GitRepoCloner;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.persistence.services.FileStorageException;

// Should be executed before ValidateDeployParametersStep as the archive ID is determined during this step execution
@Component("processGitSourceStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ProcessGitSourceStep extends AbstractProcessStep {

    private static final String SKIP_SSL_GIT_CONFIG = ".skipSslGitConfig";
    private static final String PATH_SEPARATOR = "/";
    private static final String GIT_SERVICE_URL_KEY = "git-service";
    private static final String REPOSITORY_DIRECTORY_NAME = "repos";
    private static final String MTAR_EXTENTION = ".mtar";
    public static final String META_INF_PATH = "META-INF";
    private static final String MANIFEST_PATH = "MANIFEST.MF";
    private static final String MTAD_PATH = "mtad.yaml";

    @Inject
    private Configuration configuration;

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        getStepLogger().logActivitiTask();
        try {
            getStepLogger().info(Messages.DOWNLOADING_DEPLOYABLE);

            final String gitUri = getGitUri(context);
            final String gitRepoPath = (String) context.getVariable(Constants.PARAM_GIT_REPO_PATH);
            String processId = context.getProcessInstanceId();
            final String repoName = extractRepoName(gitUri, processId);
            final Path reposDir = Paths.get(REPOSITORY_DIRECTORY_NAME, repoName);
            Path gitConfigFilePath = generateGitConfigFilepath(processId);
            if (!Files.exists(reposDir)) {
                Files.createDirectories(reposDir);
            }
            Path mtarZip = null;
            try {

                GitRepoCloner cloner = createCloner(context);
                getStepLogger().info(Messages.CLONING_REPOSITORY, gitUri);
                cloner.cloneRepo(gitUri, reposDir);
                final Path mtaRepoPath = reposDir.resolve(gitRepoPath).normalize();
                mtarZip = zipRepoContent(mtaRepoPath);
                uploadZipToDB(context, mtarZip);
            } finally {
                try {
                    deleteTemporaryRepositoryDirectory(reposDir);
                    if (Files.exists(gitConfigFilePath)) {
                        Files.delete(gitConfigFilePath);
                    }
                    if (mtarZip != null && Files.exists(mtarZip)) {
                        FileUtils.deleteDirectory(mtarZip);
                    }
                } catch (IOException e) {
                    // ignore such cases
                }
            }
            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            getStepLogger().error(e.getMessage());
            throw e;
        } catch (GitAPIException | IOException | FileStorageException e) {
            getStepLogger().error(e, Messages.ERROR_DOWNLOADING_DEPLOYABLE_FROM_GIT);
            throw new SLException(e, Messages.ERROR_PROCESSING_GIT_MTA_SOURCE);
        }
    }

    private GitRepoCloner createCloner(DelegateExecution context) {
        GitRepoCloner cloner = new GitRepoCloner();
        cloner.setGitServiceUrlString(getGitServiceUrl(context));
        cloner.setRefName((String) context.getVariable(Constants.PARAM_GIT_REF));
        cloner.setGitConfigFilePath(generateGitConfigFilepath(context.getProcessInstanceId()));
        cloner.setSkipSslValidation((boolean) context.getVariable(Constants.PARAM_GIT_SKIP_SSL));
        String userName = StepsUtil.determineCurrentUser(context, getStepLogger());
        String token;
        try {
            token = clientProvider.getValidToken(userName).getValue();
            cloner.setCredentials(userName, token);
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_RETRIEVING_OAUT_TOKEN);
            throw e;
        }
        return cloner;
    }

    protected String getGitUri(DelegateExecution context) throws SLException {
        String gitUriParam = (String) context.getVariable(Constants.PARAM_GIT_URI);
        try {
            return new URL(gitUriParam).toString();
        } catch (MalformedURLException e) {
            String gitServiceUrl = getGitServiceUrl(context);
            return buildUriFromRepositoryName(gitUriParam, gitServiceUrl);
        }
    }

    protected String buildUriFromRepositoryName(String gitUriParam, String gitServiceUrl) throws SLException {
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

    private String getGitServiceUrl(DelegateExecution context) throws SLException {
        if (!isClientExtensionsAvailable(context)) {
            return null;
        }
        CloudInfoExtended info = getCloudInfoExtended(context);
        return info.getServiceUrl(GIT_SERVICE_URL_KEY);
    }

    private CloudInfoExtended getCloudInfoExtended(DelegateExecution context) throws SLException {
        return (CloudInfoExtended) getCloudFoundryClient(context).getCloudInfo();
    }

    private boolean isClientExtensionsAvailable(DelegateExecution context) throws SLException {
        CloudFoundryOperations client = getCloudFoundryClient(context);
        return client.getCloudInfo() instanceof CloudInfoExtended;
    }

    protected Path zipRepoContent(final Path mtaPath) throws IOException, SLException {
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

    protected void uploadZipToDB(DelegateExecution context, final Path mtarZip) throws SLException, FileStorageException, IOException {
        InputStream mtarInputStream = null;
        getStepLogger().info(Messages.UPLOADING_MTAR);
        getStepLogger().debug("uploading file " + mtarZip.toAbsolutePath().toString() + " to DB");
        try {
            com.sap.cloud.lm.sl.persistence.util.Configuration fileConfiguration = configuration.getFileConfiguration();
            String spaceId = StepsUtil.getSpaceId(context);
            mtarInputStream = Files.newInputStream(mtarZip);
            String serviceId = StepsUtil.getServiceId(context);
            String mtarName = mtarZip.getFileName().toString();
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
        return dir.toFile().getName().equals(org.eclipse.jgit.lib.Constants.DOT_GIT);
    }

    private boolean shouldOmmitFile(Path file) {
        return file.toFile().getName().equals(org.eclipse.jgit.lib.Constants.DOT_GIT_IGNORE);
    }

    private String getPathName(Path parentFolder, Path fileToAppend) {
        Path relativePath = parentFolder.relativize(fileToAppend);
        return FilenameUtils.separatorsToUnix(relativePath.toString());
    }

    private boolean directoryContainsManifest(Path mtaPath) {
        Path metaInfPath = mtaPath.resolve(META_INF_PATH);
        Path manifestPath = metaInfPath.resolve(MANIFEST_PATH);
        Path mtadPath = metaInfPath.resolve(MTAD_PATH);
        return Files.exists(manifestPath) && Files.exists(mtadPath);
    }

    private void deleteTemporaryRepositoryDirectory(Path clonedRepoDir) throws IOException {

        // Workaround in JGit for deleting a cloned repository directory
        org.eclipse.jgit.util.FileUtils.delete(clonedRepoDir.toFile(),
            org.eclipse.jgit.util.FileUtils.RETRY | org.eclipse.jgit.util.FileUtils.RECURSIVE);
    }

}