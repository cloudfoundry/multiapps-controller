package com.sap.cloud.lm.sl.cf.process.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Collections;

import javax.net.ssl.SSLHandshakeException;

import org.cloudfoundry.multiapps.common.SLException;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.process.Messages;

public class GitRepoCloner {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitRepoCloner.class);

    private final CloneCommand cloneCommand;
    private String refName;
    private Path gitConfigFilePath;
    private boolean skipSslValidation;

    public GitRepoCloner() {
        cloneCommand = Git.cloneRepository();
    }

    public void setRefName(String refName) {
        this.refName = refName;
    }

    public void cloneRepo(final String gitUri, final Path repoDir) throws GitAPIException, IOException {
        if (repoDir.toFile()
                   .exists()) {
            LOGGER.debug(MessageFormat.format("Deleting left-over repo dir {0}", repoDir.toAbsolutePath()));
            com.sap.cloud.lm.sl.cf.core.util.FileUtils.deleteDirectory(repoDir);
        }

        configureGitSslValidation();
        if (refName != null && !refName.isEmpty()) {
            String fullRefName = refName.startsWith("refs/") ? refName : "refs/heads/" + refName;
            cloneCommand.setBranchesToClone(Collections.singletonList(fullRefName));
            cloneCommand.setBranch(fullRefName);
        }
        cloneCommand.setTimeout(290);
        cloneCommand.setDirectory(repoDir.toAbsolutePath()
                                         .toFile());
        cloneCommand.setURI(gitUri);
        LOGGER.debug(MessageFormat.format("cloning repo with url {0} in repo dir {1} ref '{2}'", gitUri, repoDir.toAbsolutePath()));
        try (Git callInstance = cloneCommand.call()) {
            Repository repo = callInstance.getRepository();
            repo.close();
        } catch (TransportException e) {
            Throwable cause1 = e.getCause();
            if (cause1 != null && cause1.getCause() instanceof SSLHandshakeException) {
                throw new SLException(cause1.getCause(), "Failed to establish ssl connection"); // NOSONAR
            }
            throw e;
        }
    }

    protected void configureGitSslValidation() throws IOException {
        if (!skipSslValidation) {
            return;
        }
        LOGGER.debug("Skipping https ssl validation");
        if (gitConfigFilePath.toFile()
                             .exists()) {
            Files.delete(gitConfigFilePath);
        }
        File userConfigFile = gitConfigFilePath.toFile();
        try (PrintWriter configWriter = new PrintWriter(userConfigFile, "UTF-8")) {
            configWriter.println("[http]");
            configWriter.println("\t" + "sslVerify = false");
        } catch (FileNotFoundException e) {
            LOGGER.error(Messages.COULD_NOT_CONFIGURE_GIT_TO_SKIP_SSL, e);
            throw new SLException(Messages.COULD_NOT_CONFIGURE_GIT_TO_SKIP_SSL, e);
        }
        SystemReader.setInstance(new SystemReader() {
            @Override
            public FileBasedConfig openUserConfig(Config parent, FS fs) {
                return new FileBasedConfig(parent, userConfigFile, fs);
            }

            @Override
            public FileBasedConfig openSystemConfig(Config parent, FS fs) {
                File configFile = fs.getGitSystemConfig();
                if (configFile == null) {
                    return new FileBasedConfig(null, fs) {

                        @Override
                        public void load() {
                            // empty, do not load
                        }

                        @Override
                        public boolean isOutdated() {
                            // regular class would bomb here
                            return false;
                        }

                    };
                }
                return new FileBasedConfig(parent, configFile, fs);
            }

            @Override
            public String getenv(String variable) {
                return System.getenv(variable);
            }

            @Override
            public String getProperty(String key) {
                return System.getProperty(key);
            }

            @Override
            public long getCurrentTime() {
                return System.currentTimeMillis();
            }

            @Override
            public int getTimezone(long when) {
                return getTimeZone().getOffset(when) / (60 * 1000);
            }

            @Override
            public String getHostname() {
                return "";
            }
        });
    }

    public void setGitConfigFilePath(Path gitConfigFilePath) {
        this.gitConfigFilePath = gitConfigFilePath;

    }

    public void setSkipSslValidation(boolean skipSslValidation) {
        this.skipSslValidation = skipSslValidation;
    }
}
