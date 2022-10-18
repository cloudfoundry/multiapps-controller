package org.cloudfoundry.multiapps.controller.shutdown.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.shutdown.client.configuration.EnvironmentBasedShutdownConfiguration;
import org.cloudfoundry.multiapps.controller.shutdown.client.configuration.ShutdownConfiguration;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudControllerClientImpl;
import com.sap.cloudfoundry.client.facade.CloudCredentials;
import com.sap.cloudfoundry.client.facade.domain.InstancesInfo;

public class ApplicationShutdownExecutor {

    public static void main(String[] args) {
        new ApplicationShutdownExecutor().execute();
    }

    private final ShutdownConfiguration shutdownConfiguration = new EnvironmentBasedShutdownConfiguration();
    private final ShutdownClientFactory shutdownClientFactory = new ShutdownClientFactory();
    private final ApplicationInstanceShutdownExecutor instanceShutdownExecutor = new ApplicationInstanceShutdownExecutor(shutdownConfiguration,
                                                                                                                         shutdownClientFactory);

    public void execute() {
        int applicationInstancesCount = getApplicationInstancesCount(shutdownConfiguration);
        shutdownInstances(applicationInstancesCount);
    }

    private void shutdownInstances(int applicationInstancesCount) {
        UUID applicationGuid = shutdownConfiguration.getApplicationGuid();
        for (int i = 0; i < applicationInstancesCount; i++) {
            instanceShutdownExecutor.execute(applicationGuid, i);
        }
    }

    private static int getApplicationInstancesCount(ShutdownConfiguration shutdownConfiguration) {
        CloudControllerClient client = createCloudControllerClient(shutdownConfiguration);
        InstancesInfo instances = client.getApplicationInstances(shutdownConfiguration.getApplicationGuid());
        return instances.getInstances()
                        .size();
    }

    private static CloudControllerClient createCloudControllerClient(ShutdownConfiguration shutdownConfiguration) {
        URL cloudControllerUrl = toURL(shutdownConfiguration.getCloudControllerUrl());
        return new CloudControllerClientImpl(cloudControllerUrl, createCloudCredentials(shutdownConfiguration));
    }

    private static URL toURL(String string) {
        try {
            return new URL(string);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(MessageFormat.format("{0} is not a valid URL.", string));
        }
    }

    private static CloudCredentials createCloudCredentials(ShutdownConfiguration shutdownConfiguration) {
        return new CloudCredentials(shutdownConfiguration.getUsername(), shutdownConfiguration.getPassword());
    }

}
