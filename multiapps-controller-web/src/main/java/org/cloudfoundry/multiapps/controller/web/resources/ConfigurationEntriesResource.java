package org.cloudfoundry.multiapps.controller.web.resources;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.processor.MtaMetadataParser;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaConfigurationPurger;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.core.util.UserInfo;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationEntryService;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationSubscriptionService;
import org.cloudfoundry.multiapps.controller.web.util.SecurityContextUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;

@RestController
@RequestMapping("/rest/configuration-entries")
public class ConfigurationEntriesResource {

    public static final String REQUEST_PARAM_ORGANIZATION = "org";
    public static final String REQUEST_PARAM_SPACE = "space";

    @Inject
    @Named("configurationEntryService")
    private ConfigurationEntryService configurationEntryService;
    @Inject
    @Named("configurationSubscriptionService")
    private ConfigurationSubscriptionService configurationSubscriptionService;
    @Inject
    private CloudControllerClientProvider clientProvider;
    @Inject
    private CloudControllerClientFactory clientFactory;
    @Inject
    private MtaMetadataParser mtaMetadataParser;
    @Inject
    private TokenService tokenService;

    @PostMapping("/purge")
    public ResponseEntity<Void> purgeConfigurationRegistry(@RequestParam(REQUEST_PARAM_ORGANIZATION) String organization,
                                                           @RequestParam(REQUEST_PARAM_SPACE) String space) {
        UserInfo user = SecurityContextUtil.getUserInfo();
        var spaceClient = clientFactory.createSpaceClient(tokenService.getToken(user.getName()));

        var cloudSpace = spaceClient.getSpace(organization, space);

        CloudControllerClient client = clientProvider.getControllerClientWithNoCorrelation(user.getName(), cloudSpace.getGuid()
                                                                                                                     .toString());
        MtaConfigurationPurger configurationPurger = new MtaConfigurationPurger(client, spaceClient,
                                                                                configurationEntryService,
                                                                                configurationSubscriptionService,
                                                                                mtaMetadataParser);
        configurationPurger.purge(organization, space);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                             .build();
    }

}
