package org.cloudfoundry.multiapps.controller.web.resources;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.core.auditlogging.MtaConfigurationPurgerAuditLog;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.processor.MtaMetadataParser;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaConfigurationPurger;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.core.util.UserInfo;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationEntryService;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationSubscriptionService;
import org.cloudfoundry.multiapps.controller.web.Constants;
import org.cloudfoundry.multiapps.controller.web.util.SecurityContextUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = Constants.Resources.CONFIGURATION_ENTRIES)
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
    @Inject
    private MtaConfigurationPurgerAuditLog mtaConfigurationPurgerAuditLog;

    @PostMapping(value = Constants.Endpoints.PURGE)
    public ResponseEntity<Void> purgeConfigurationRegistry(@RequestParam(REQUEST_PARAM_ORGANIZATION) String organization,
                                                           @RequestParam(REQUEST_PARAM_SPACE) String space) {
        UserInfo user = SecurityContextUtil.getUserInfo();
        var spaceClient = clientFactory.createSpaceClient(tokenService.getToken(user.getId()));

        var cloudSpace = spaceClient.getSpace(organization, space);

        CloudControllerClient client = clientProvider.getControllerClientWithNoCorrelation(user.getId(),
                                                                                           cloudSpace.getGuid()
                                                                                                     .toString());
        MtaConfigurationPurger configurationPurger = new MtaConfigurationPurger(client, spaceClient, configurationEntryService,
                                                                                configurationSubscriptionService, mtaMetadataParser,
                                                                                mtaConfigurationPurgerAuditLog);
        configurationPurger.purge(organization, space);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                             .build();
    }

}
