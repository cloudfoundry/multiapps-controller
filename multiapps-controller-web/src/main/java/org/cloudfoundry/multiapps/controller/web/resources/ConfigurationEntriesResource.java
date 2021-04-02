package org.cloudfoundry.multiapps.controller.web.resources;

import javax.inject.Inject;

import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.processor.MtaMetadataParser;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaConfigurationPurger;
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
    private ConfigurationEntryService configurationEntryService;
    @Inject
    private ConfigurationSubscriptionService configurationSubscriptionService;
    @Inject
    private CloudControllerClientProvider clientProvider;
    @Inject
    private MtaMetadataParser mtaMetadataParser;

    @PostMapping("/purge")
    public ResponseEntity<Void> purgeConfigurationRegistry(@RequestParam(REQUEST_PARAM_ORGANIZATION) String organization,
                                                           @RequestParam(REQUEST_PARAM_SPACE) String space) {
        CloudControllerClient client = createClient(organization, space);
        MtaConfigurationPurger configurationPurger = new MtaConfigurationPurger(client,
                                                                                configurationEntryService,
                                                                                configurationSubscriptionService,
                                                                                mtaMetadataParser);
        configurationPurger.purge(organization, space);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                             .build();
    }

    private CloudControllerClient createClient(String organization, String space) {
        UserInfo userInfo = SecurityContextUtil.getUserInfo();
        return clientProvider.getControllerClient(userInfo.getName(), organization, space, null);
    }

}
