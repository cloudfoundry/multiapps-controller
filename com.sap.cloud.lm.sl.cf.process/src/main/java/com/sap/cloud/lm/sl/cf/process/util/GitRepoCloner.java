package com.sap.cloud.lm.sl.cf.process.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Arrays;

import javax.net.ssl.SSLHandshakeException;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.steps.ProcessGitSourceStep;
import com.sap.cloud.lm.sl.common.SLException;

public class GitRepoCloner {
    private String gitServiceUrlString;
    CloneCommand cloneCommand;
    private String userName;
    private String token;
    private String refName;
    private Path gitconfigFilePath;
    private boolean skipSslValidation;
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessGitSourceStep.class);

    public GitRepoCloner() {
        cloneCommand = Git.cloneRepository();
    }

    public void setGitServiceUrlString(String gitServiceUrlString) {
        this.gitServiceUrlString = gitServiceUrlString;
    }

    public void setCredentials(String userName, String token) {
        this.userName = userName;
        this.token = token;
    }

    public void setRefName(String refName) {
        this.refName = refName;
    }

    public void cloneRepo(final String gitUri, final Path repoDir) throws InvalidRemoteException, GitAPIException, IOException {
        if (Files.exists(repoDir)) {
            LOGGER.debug("Deleting left-over repo dir" + repoDir.toAbsolutePath().toString());
            com.sap.cloud.lm.sl.cf.core.util.FileUtils.deleteDirectory(repoDir);
        }

        configureGitSslValidation();
        if (shoudlUseToken(gitServiceUrlString, gitUri)) {
            cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, token));
        }
        if (refName != null && !refName.isEmpty()) {
            String fullRefName = refName.startsWith("refs/") ? refName : "refs/heads/" + refName;
            cloneCommand.setBranchesToClone(Arrays.asList(new String[] { fullRefName }));
            cloneCommand.setBranch(fullRefName);
        }
        cloneCommand.setTimeout(290);
        cloneCommand.setDirectory(repoDir.toAbsolutePath().toFile());
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

    private boolean shoudlUseToken(String gitServiceUrlString, String gitRepoUri) throws SLException {
        if (gitServiceUrlString == null || gitRepoUri == null) {
            return false;
        }
        URL serviceUrl;
        URL repoUrl;
        try {
            serviceUrl = new URL(gitServiceUrlString);
            repoUrl = new URL(gitRepoUri);
        } catch (MalformedURLException e) {
            LOGGER.debug(MessageFormat.format("Failed to parse git-service and user URLs({0} and {1}), skipping token authentication",
                gitServiceUrlString, gitRepoUri));
            return false;
        }
        return repoUrl.getHost().equals(serviceUrl.getHost()) && repoUrl.getPort() == serviceUrl.getPort();

    }

    protected void configureGitSslValidation() throws SLException, IOException {
        if (!skipSslValidation) {
            return;
        }
        LOGGER.debug("Skipping https ssl validation");
        if (Files.exists(gitconfigFilePath)) {
            Files.delete(gitconfigFilePath);
        }
        File userConcigFile = gitconfigFilePath.toFile();
        try (PrintWriter configWriter = new PrintWriter(userConcigFile, "UTF-8");) {
            configWriter.println("[http]");
            configWriter.println("\t" + "sslVerify = false");
        } catch (FileNotFoundException e) {
            LOGGER.error(Messages.COULD_NOT_CONFIGURE_GIT_TO_SKIP_SSL, e);
            throw new SLException(Messages.COULD_NOT_CONFIGURE_GIT_TO_SKIP_SSL, e);
        }
        SystemReader.setInstance(new SystemReader() {
            @Override
            public FileBasedConfig openUserConfig(Config parent, FS fs) {
                return new FileBasedConfig(parent, userConcigFile, fs);
            }

            @Override
            public FileBasedConfig openSystemConfig(Config parent, FS fs) {
                File configFile = fs.getGitSystemConfig();
                if (configFile == null) {
                    return new FileBasedConfig(null, fs) {
                        public void load() {
                            // empty, do not load
                        }

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
        this.gitconfigFilePath = gitConfigFilePath;

    }

    public void setSkipSslValidation(boolean skipSslValidation) {
        this.skipSslValidation = skipSslValidation;
    }
}
