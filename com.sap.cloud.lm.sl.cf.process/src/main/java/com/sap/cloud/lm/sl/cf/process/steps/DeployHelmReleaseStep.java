package com.sap.cloud.lm.sl.cf.process.steps;

import java.io.IOException;
import java.util.Iterator;
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
import hapi.release.InfoOuterClass.Info;
import hapi.release.ReleaseOuterClass.Release;
import hapi.release.StatusOuterClass.Status.Code;
import hapi.services.tiller.Tiller.InstallReleaseRequest;
import hapi.services.tiller.Tiller.InstallReleaseResponse;
import hapi.services.tiller.Tiller.ListReleasesRequest;
import hapi.services.tiller.Tiller.ListReleasesResponse;
import hapi.services.tiller.Tiller.UpdateReleaseRequest;
import hapi.services.tiller.Tiller.UpdateReleaseResponse;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;

@Component("deployHelmReleaseStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeployHelmReleaseStep extends SyncActivitiStep {

    private static final String KUBERNETES_CONFIGURATION_IS_NOT_SPECIFIED_IN_THE_ENVIRONMENT = "No Kubernetes configuration specified. Use the \"KUBERNETES_MASTER_URL\", \"KUBERNETES_USERNAME\", \"KUBERNETES_PASSWORD\" and \"KUBERNETES_NAMESPACE\" environment variables to do so.";
    private static final Integer HELM_RELEASE_OPERATION_TIMEOUT = 600;

    @Inject
    private ApplicationConfiguration configuration;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws IOException, InterruptedException, ExecutionException {
        Chart chart = getChart(execution);

        DefaultKubernetesClient client = buildKubernetesClient();
        try (Tiller tiller = new Tiller(client); ReleaseManager releaseManager = new ReleaseManager(tiller)) {
            Code releaseStatus = getReleaseStatus(releaseManager, chart);
            getStepLogger().info("Current status: {0}", releaseStatus);
            if (releaseStatus == Code.UNKNOWN) {
                deploy(releaseManager, chart);
            } else if (releaseStatus == Code.DEPLOYED) {
                update(releaseManager, chart);
            } else {
                throw new UnsupportedOperationException("The current release status is not supported.");
            }
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

    private Code getReleaseStatus(ReleaseManager releaseManager, Chart chart) throws IOException, InterruptedException, ExecutionException {
        Iterator<ListReleasesResponse> releasesIterator = releaseManager.list(buildListReleasesRequest());
        while (releasesIterator.hasNext()) {
            ListReleasesResponse releases = releasesIterator.next();
            for (Release release : releases.getReleasesList()) {
                getStepLogger().debug("Checking release \"{0}\" in namespace \"{1}\"...", release.getName(), release.getNamespace());
                if (release.getName()
                    .equals(getReleaseName(chart))) {
                    Info info = release.getInfo();
                    return getStatus(info);
                }
            }
        }
        return Code.UNKNOWN;
    }

    private ListReleasesRequest buildListReleasesRequest() {
        return ListReleasesRequest.newBuilder()
            .setNamespace(configuration.getKubernetesNamespace())
            .build();
    }

    private Code getStatus(Info info) {
        return info.getStatus()
            .getCode();
    }

    private void deploy(ReleaseManager releaseManager, Chart chart) throws IOException, InterruptedException, ExecutionException {
        getStepLogger().info("Installing release \"{0}\"...", getReleaseName(chart));
        Future<InstallReleaseResponse> releaseFuture = releaseManager.install(buildInstallReleaseRequest(chart), chart.toBuilder());
        getStepLogger().info("Status: {0}", getInstallStatus(releaseFuture));
    }

    private InstallReleaseRequest.Builder buildInstallReleaseRequest(Chart chart) {
        return InstallReleaseRequest.newBuilder()
            .setName(getReleaseName(chart))
            .setWait(true)
            .setTimeout(HELM_RELEASE_OPERATION_TIMEOUT);
    }

    private String getReleaseName(Chart chart) {
        return String.format("%s-%s", chart.getMetadata()
            .getName(), configuration.getKubernetesNamespace());
    }

    private Code getInstallStatus(Future<InstallReleaseResponse> releaseFuture) throws InterruptedException, ExecutionException {
        Info info = releaseFuture.get()
            .getRelease()
            .getInfo();
        return getStatus(info);
    }

    private void update(ReleaseManager releaseManager, Chart chart) throws IOException, InterruptedException, ExecutionException {
        getStepLogger().info("Updating release \"{0}\"...", getReleaseName(chart));
        Future<UpdateReleaseResponse> releaseFuture = releaseManager.update(buildUpdateReleaseRequest(chart), chart.toBuilder());
        getStepLogger().info("Status: {0}", getUpdateStatus(releaseFuture));
    }

    private UpdateReleaseRequest.Builder buildUpdateReleaseRequest(Chart chart) {
        return UpdateReleaseRequest.newBuilder()
            .setName(getReleaseName(chart))
            .setWait(true)
            .setTimeout(HELM_RELEASE_OPERATION_TIMEOUT);
    }

    private Object getUpdateStatus(Future<UpdateReleaseResponse> releaseFuture) throws InterruptedException, ExecutionException {
        Info info = releaseFuture.get()
            .getRelease()
            .getInfo();
        return getStatus(info);
    }

}
