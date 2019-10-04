package com.sap.cloud.lm.sl.cf.web.resources;

import static com.sap.cloud.lm.sl.cf.core.model.ResourceMetadata.RequestParameters.ORGANIZATION;
import static com.sap.cloud.lm.sl.cf.core.model.ResourceMetadata.RequestParameters.SPACE;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.helpers.ClientHelper;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscriptions;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationSubscriptionService;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;

@RestController
@RequestMapping("/rest/configuration-subscriptions")
public class ConfigurationSubscriptionsResource {

    @Inject
    private ConfigurationSubscriptionService configurationSubscriptionsService;

    @Inject
    private CloudControllerClientProvider clientProvider;

    @GetMapping(produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<ConfigurationSubscriptions>
           getConfigurationSubscriptions(@RequestParam(ORGANIZATION) String org,
                                         @RequestParam(name = SPACE, required = false) String space) {
        CloudControllerClient client = getCloudFoundryClient();
        List<CloudSpace> clientSpaces = getClientSpaces(org, space, client);

        List<ConfigurationSubscription> configurationSubscriptions = getConfigurationEntries(clientSpaces, client);

        return ResponseEntity.ok()
                             .body(wrap(configurationSubscriptions));
    }

    private List<CloudSpace> getClientSpaces(String org, String space, CloudControllerClient client) {
        if (space == null) {
            return client.getSpaces(org);
        }
        return Collections.singletonList(client.getSpace(org, space));
    }

    private List<ConfigurationSubscription> getConfigurationEntries(List<CloudSpace> clientSpaces, CloudControllerClient client) {
        return clientSpaces.stream()
                           .map(clientSpace -> getConfigurationEntries(client, clientSpace))
                           .flatMap(List::stream)
                           .collect(Collectors.toList());
    }

    private List<ConfigurationSubscription> getConfigurationEntries(CloudControllerClient client, CloudSpace clientSpace) {
        return configurationSubscriptionsService.createQuery()
                                                .spaceId(computeSpaceId(client, clientSpace.getOrganization()
                                                                                           .getName(),
                                                                        clientSpace.getName()))
                                                .list();
    }

    private ConfigurationSubscriptions wrap(List<ConfigurationSubscription> configurationSubscriptions) {
        return new ConfigurationSubscriptions(configurationSubscriptions);
    }

    private CloudControllerClient getCloudFoundryClient() {
        UserInfo userInfo = SecurityContextUtil.getUserInfo();
        return clientProvider.getControllerClient(userInfo.getName());
    }

    private String computeSpaceId(CloudControllerClient client, String orgName, String spaceName) {
        return new ClientHelper(client).computeSpaceId(orgName, spaceName);
    }

}
