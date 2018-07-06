package com.sap.cloud.lm.sl.cf.process.steps;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.microbean.helm.ReleaseManager;
import org.microbean.helm.Tiller;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.process.Constants;

import hapi.release.InfoOuterClass.Info;
import hapi.release.StatusOuterClass.Status.Code;
import hapi.services.tiller.Tiller.UninstallReleaseRequest;
import hapi.services.tiller.Tiller.UninstallReleaseResponse;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;

@Component("deleteHelmReleaseStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteHelmReleaseStep extends SyncActivitiStep {

    private static final String KUBERNETES_CONFIGURATION_IS_NOT_SPECIFIED_IN_THE_ENVIRONMENT = "No Kubernetes configuration specified. Use the \"KUBERNETES_MASTER_URL\", \"KUBERNETES_USERNAME\", \"KUBERNETES_PASSWORD\" and \"KUBERNETES_NAMESPACE\" environment variables to do so.";
    private static final Integer HELM_RELEASE_OPERATION_TIMEOUT = 600;

    @Inject
    private ApplicationConfiguration configuration;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws IOException, InterruptedException, ExecutionException {
        String releaseName = getReleaseName(execution);

        DefaultKubernetesClient client = buildKubernetesClient();
        try (Tiller tiller = new Tiller(client); ReleaseManager releaseManager = new ReleaseManager(tiller)) {
            deleteRelease(releaseManager, releaseName);
        }
        return StepPhase.DONE;
    }

    private String getReleaseName(ExecutionWrapper execution) {
        return (String) execution.getContext()
            .getVariable(Constants.PARAM_MTA_ID);
    }

    // TODO: Copy-pasted from DeployHelmReleaseStep. Fix!
    private DefaultKubernetesClient buildKubernetesClient() {
        Config clientConfiguration = buildKubernetesClientConfiguration();
        return new DefaultKubernetesClient(clientConfiguration);
    }

    // TODO: Copy-pasted from DeployHelmReleaseStep. Fix!
    private Config buildKubernetesClientConfiguration() {
        validateConfiguration();
        return new ConfigBuilder().withMasterUrl(configuration.getKubernetesMasterUrl())
            .withUsername(configuration.getKubernetesUsername())
            .withPassword(configuration.getKubernetesPassword())
            .withNamespace(configuration.getKubernetesNamespace())
            .withTrustCerts(true)
            .build();
    }

    // TODO: Copy-pasted from DeployHelmReleaseStep. Fix!
    private void validateConfiguration() {
        // TODO: Move this validation to ApplicationConfiguration:
        Assert.notNull(configuration.getKubernetesMasterUrl(), KUBERNETES_CONFIGURATION_IS_NOT_SPECIFIED_IN_THE_ENVIRONMENT);
        Assert.notNull(configuration.getKubernetesUsername(), KUBERNETES_CONFIGURATION_IS_NOT_SPECIFIED_IN_THE_ENVIRONMENT);
        Assert.notNull(configuration.getKubernetesPassword(), KUBERNETES_CONFIGURATION_IS_NOT_SPECIFIED_IN_THE_ENVIRONMENT);
        Assert.notNull(configuration.getKubernetesNamespace(), KUBERNETES_CONFIGURATION_IS_NOT_SPECIFIED_IN_THE_ENVIRONMENT);
    }

    private void deleteRelease(ReleaseManager releaseManager, String releaseName)
        throws IOException, InterruptedException, ExecutionException {
        getStepLogger().info("Deleting release \"{0}\"...", releaseName);
        Future<UninstallReleaseResponse> uninstallFuture = releaseManager.uninstall(buildUninstallReleaseRequest(releaseName));
        Info info = uninstallFuture.get()
            .getRelease()
            .getInfo();
        getStepLogger().info("Status: {0}", getStatus(info));
    }

    private UninstallReleaseRequest buildUninstallReleaseRequest(String releaseName) {
        return UninstallReleaseRequest.newBuilder()
            .setName(releaseName)
            .setPurge(true)
            .setTimeout(HELM_RELEASE_OPERATION_TIMEOUT)
            .build();
    }

    // TODO: Copy-pasted from DeployHelmReleaseStep. Fix!
    private Code getStatus(Info info) {
        return info.getStatus()
            .getCode();
    }

}
