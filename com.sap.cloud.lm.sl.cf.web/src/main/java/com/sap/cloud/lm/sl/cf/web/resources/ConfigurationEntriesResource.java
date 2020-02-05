package com.sap.cloud.lm.sl.cf.web.resources;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.EnvMtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.MtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaConfigurationPurger;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationEntryService;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationSubscriptionService;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;

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
    @Inject
    private EnvMtaMetadataParser envMtaMetadataParser;

    @PostMapping("/purge")
    public ResponseEntity<Void> purgeConfigurationRegistry(HttpServletRequest request,
                                                           @RequestParam(REQUEST_PARAM_ORGANIZATION) String organization,
                                                           @RequestParam(REQUEST_PARAM_SPACE) String space) {
        CloudControllerClient client = createClient(organization, space);
        MtaConfigurationPurger configurationPurger = new MtaConfigurationPurger(client,
                                                                                configurationEntryService,
                                                                                configurationSubscriptionService,
                                                                                mtaMetadataParser,
                                                                                envMtaMetadataParser);
        configurationPurger.purge(organization, space);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                             .build();
    }

    private CloudControllerClient createClient(String organization, String space) {
        UserInfo userInfo = SecurityContextUtil.getUserInfo();
        return clientProvider.getControllerClient(userInfo.getName(), organization, space, null);
    }

}
