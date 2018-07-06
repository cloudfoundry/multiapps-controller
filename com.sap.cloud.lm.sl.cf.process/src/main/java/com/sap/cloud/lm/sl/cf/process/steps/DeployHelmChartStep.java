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

import com.google.protobuf.InvalidProtocolBufferException;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;

import hapi.chart.ChartOuterClass.Chart;
import hapi.release.StatusOuterClass.Status.Code;
import hapi.services.tiller.Tiller.InstallReleaseRequest;
import hapi.services.tiller.Tiller.InstallReleaseResponse;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;

@Component("deployHelmChartStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeployHelmChartStep extends SyncActivitiStep {

    private static final String KUBERNETES_CONFIGURATION_IS_NOT_SPECIFIED_IN_THE_ENVIRONMENT = "No Kubernetes configuration specified. Use the \"KUBERNETES_MASTER_URL\", \"KUBERNETES_USERNAME\", \"KUBERNETES_PASSWORD\" and \"KUBERNETES_NAMESPACE\" environment variables to do so.";
    private static final Integer HELM_INSTALL_TIMEOUT = 600;

    @Inject
    private ApplicationConfiguration configuration;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws IOException, InterruptedException, ExecutionException {
        Chart chart = getChart(execution);
        getStepLogger().info("Installing chart \"{0}\"...", chart.getMetadata()
            .getName());

        DefaultKubernetesClient client = buildKubernetesClient();
        try (Tiller tiller = new Tiller(client); ReleaseManager releaseManager = new ReleaseManager(tiller)) {
            Future<InstallReleaseResponse> releaseFuture = releaseManager.install(buildInstallReleaseRequest(chart), chart.toBuilder());
            getStepLogger().info("Status: {0}", getStatusCode(releaseFuture));
        }
        return StepPhase.DONE;
    }

    private Chart getChart(ExecutionWrapper execution) throws InvalidProtocolBufferException {
        byte[] chart = (byte[]) execution.getContext()
            .getVariable(BuildHelmChartStep.VAR_HELM_CHART);
        return Chart.parseFrom(chart);
    }

    private DefaultKubernetesClient buildKubernetesClient() {
        Config clientConfiguration = buildKubernetesClientConfiguration();
        return new DefaultKubernetesClient(clientConfiguration);
    }

    private Config buildKubernetesClientConfiguration() {
        validateConfiguration();
        return new ConfigBuilder().withMasterUrl(configuration.getKubernetesMasterUrl())
            .withUsername(configuration.getKubernetesUsername())
            .withPassword(configuration.getKubernetesPassword())
            .withNamespace(configuration.getKubernetesNamespace())
            .withTrustCerts(true)
            .build();
    }

    private void validateConfiguration() {
        // TODO: Move this validation to ApplicationConfiguration:
        Assert.notNull(configuration.getKubernetesMasterUrl(), KUBERNETES_CONFIGURATION_IS_NOT_SPECIFIED_IN_THE_ENVIRONMENT);
        Assert.notNull(configuration.getKubernetesUsername(), KUBERNETES_CONFIGURATION_IS_NOT_SPECIFIED_IN_THE_ENVIRONMENT);
        Assert.notNull(configuration.getKubernetesPassword(), KUBERNETES_CONFIGURATION_IS_NOT_SPECIFIED_IN_THE_ENVIRONMENT);
        Assert.notNull(configuration.getKubernetesNamespace(), KUBERNETES_CONFIGURATION_IS_NOT_SPECIFIED_IN_THE_ENVIRONMENT);
    }

    private InstallReleaseRequest.Builder buildInstallReleaseRequest(Chart chart) {
        String releaseName = chart.getMetadata()
            .getName();
        InstallReleaseRequest.Builder releaseRequestBuilder = InstallReleaseRequest.newBuilder();
        releaseRequestBuilder.setName(releaseName);
        releaseRequestBuilder.setWait(true);
        releaseRequestBuilder.setTimeout(HELM_INSTALL_TIMEOUT);
        return releaseRequestBuilder;
    }

    private Code getStatusCode(Future<InstallReleaseResponse> releaseFuture) throws InterruptedException, ExecutionException {
        return releaseFuture.get()
            .getRelease()
            .getInfo()
            .getStatus()
            .getCode();
    }

}
