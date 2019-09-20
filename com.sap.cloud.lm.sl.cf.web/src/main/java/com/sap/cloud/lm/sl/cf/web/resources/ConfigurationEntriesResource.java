package com.sap.cloud.lm.sl.cf.web.resources;

import static com.sap.cloud.lm.sl.cf.core.model.ResourceMetadata.RequestParameters.ID;
import static com.sap.cloud.lm.sl.cf.core.model.ResourceMetadata.RequestParameters.ORG;
import static com.sap.cloud.lm.sl.cf.core.model.ResourceMetadata.RequestParameters.SPACE;
import static com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil.findConfigurationEntries;
import static com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil.getGlobalConfigTarget;
import static java.text.MessageFormat.format;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.servlet.http.HttpServletRequest;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.ConfigurationEntriesDto;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.ConfigurationEntryDto;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.ConfigurationFilterDto;
import com.sap.cloud.lm.sl.cf.core.filters.TargetWildcardFilter;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaConfigurationPurger;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationFilter;
import com.sap.cloud.lm.sl.cf.core.model.ResourceMetadata.RequestParameters;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationEntryService;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationSubscriptionService;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;
import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.XmlUtil;

@RestController
@RequestMapping("/rest/configuration-entries")
public class ConfigurationEntriesResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationEntriesResource.class);

    private static final String KEYVALUE_SEPARATOR = ":";

    private static final URL CREATE_CONFIGURATION_ENTRY_SCHEMA_LOCATION = ConfigurationEntriesResource.class.getResource("/create-configuration-entry-schema.xsd");
    private static final URL UPDATE_CONFIGURATION_ENTRY_SCHEMA_LOCATION = ConfigurationEntriesResource.class.getResource("/update-configuration-entry-schema.xsd");
    private static final URL CONFIGURATION_FILTER_SCHEMA_LOCATION = ConfigurationEntriesResource.class.getResource("/configuration-filter-schema.xsd");
    protected Supplier<UserInfo> userInfoSupplier = SecurityContextUtil::getUserInfo;

    @Inject
    private ConfigurationEntryService configurationEntryService;
    @Inject
    private ConfigurationSubscriptionService configurationSubscriptionService;
    @Inject
    private CloudControllerClientProvider clientProvider;
    @Inject
    private ApplicationConfiguration configuration;

    protected ResponseEntity<ConfigurationEntriesDto> filterConfigurationEntries(ConfigurationFilter filter) {
        try {
            CloudTarget globalConfigTarget = getGlobalConfigTarget(configuration);
            List<ConfigurationEntry> entries = findConfigurationEntries(configurationEntryService, filter, getUserTargets(),
                                                                        globalConfigTarget);
            return ResponseEntity.ok()
                                 .body(wrap(entries));
        } catch (IllegalArgumentException e) {
            /**
             * Thrown if the version parameter is not a valid version requirement.
             */
            throw new ParsingException(e.getMessage(), e);
        }
    }

    private List<CloudTarget> getUserTargets() {
        UserInfo userInfo = userInfoSupplier.get();
        CloudControllerClient client = clientProvider.getControllerClient(userInfo.getName());
        return client.getSpaces()
                     .stream()
                     .map(this::getCloudTarget)
                     .collect(Collectors.toList());
    }

    private CloudTarget getCloudTarget(CloudSpace cloudSpace) {
        return new CloudTarget(cloudSpace.getOrganization()
                                         .getName(),
                               cloudSpace.getName());
    }

    private ConfigurationEntriesDto wrap(List<ConfigurationEntry> entries) {
        return new ConfigurationEntriesDto(entries.stream()
                                                  .map(ConfigurationEntryDto::new)
                                                  .collect(Collectors.toList()));
    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<ConfigurationEntryDto> getConfigurationEntry(@PathVariable(ID) long id) {
        ConfigurationEntryDto configurationEntry = getConfigurationEntryDto(id);
        return ResponseEntity.ok()
                             .body(configurationEntry);
    }

    private ConfigurationEntryDto getConfigurationEntryDto(long id) {
        try {
            return new ConfigurationEntryDto(configurationEntryService.createQuery()
                                                                      .id(id)
                                                                      .singleResult());
        } catch (NoResultException e) {
            return null;
        }
    }

    private Map<String, Object> parseContentFilterParameter(List<String> content) {
        if (content == null || content.isEmpty()) {
            return null;
        }

        ParsingException contentJsonParsingException = null;
        ParsingException contentListParsingException = null;

        try {
            return parseContentQueryJsonParameter(content);
        } catch (ParsingException e) {
            contentJsonParsingException = e;
        }
        try {
            return parseContentQueryListParameter(content);
        } catch (ParsingException e) {
            contentListParsingException = e;
        }

        LOGGER.error(format(Messages.COULD_NOT_PARSE_CONTENT_PARAMETER_AS_JSON, content), contentJsonParsingException);
        LOGGER.error(format(Messages.COULD_NOT_PARSE_CONTENT_PARAMETER_AS_LIST, content), contentListParsingException);

        throw new ParsingException(Messages.COULD_NOT_PARSE_CONTENT_PARAMETER);
    }

    private Map<String, Object> parseContentQueryJsonParameter(List<String> content) {
        return JsonUtil.fromJson(content.get(0), new TypeReference<Map<String, Object>>() {
        });
    }

    private Map<String, Object> parseContentQueryListParameter(List<String> content) {
        Map<String, Object> parsedContent = new HashMap<>();
        for (String property : content) {
            String[] keyValuePair = property.split(KEYVALUE_SEPARATOR, 2);
            if (keyValuePair.length != 2) {
                throw new ParsingException(Messages.PROPERTY_DOES_NOT_CONTAIN_KEY_VALUE_PAIR, property);
            }
            parsedContent.put(keyValuePair[0], keyValuePair[1]);
        }
        return parsedContent;
    }

    @PostMapping(consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<ConfigurationEntryDto> createConfigurationEntry(@RequestBody String xml) {
        ConfigurationEntryDto dto = parseDto(xml, CREATE_CONFIGURATION_ENTRY_SCHEMA_LOCATION);
        ConfigurationEntry configurationEntry = dto.toConfigurationEntry();
        if (configurationEntry.getTargetSpace() == null) {
            throw new ParsingException(Messages.ORG_SPACE_NOT_SPECIFIED_2);
        }
        configurationEntryService.add(configurationEntry);
        AuditLoggingProvider.getFacade()
                            .logConfigCreate(configurationEntry);
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(new ConfigurationEntryDto(configurationEntry));
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<ConfigurationEntryDto> updateConfigurationEntry(@PathVariable(ID) long id, @RequestBody String xml) {
        ConfigurationEntryDto dto = parseDto(xml, UPDATE_CONFIGURATION_ENTRY_SCHEMA_LOCATION);
        if (dto.getId() != 0 && dto.getId() != id) {
            throw new ParsingException(Messages.CONFIGURATION_ENTRY_ID_CANNOT_BE_UPDATED, id);
        }

        ConfigurationEntry result = configurationEntryService.update(id, dto.toConfigurationEntry());
        AuditLoggingProvider.getFacade()
                            .logConfigUpdate(result);

        return ResponseEntity.ok()
                             .body(new ConfigurationEntryDto(result));
    }

    private ConfigurationEntryDto parseDto(String dXml, URL schemaLocation) {
        return XmlUtil.fromXml(dXml, ConfigurationEntryDto.class, schemaLocation);
    }

    @DeleteMapping(path = "/{id}", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<ConfigurationEntryDto> deleteConfigurationEntry(@PathVariable(ID) long id) {
        try {
            ConfigurationEntry entry = configurationEntryService.createQuery()
                                                                .id(id)
                                                                .singleResult();
            configurationEntryService.createQuery()
                                     .id(id)
                                     .delete();
            AuditLoggingProvider.getFacade()
                                .logConfigDelete(entry);
            return ResponseEntity.noContent()
                                 .build();
        } catch (NoResultException e) {
            return ResponseEntity.notFound()
                                 .build();
        }
    }

    @GetMapping(produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<ConfigurationEntriesDto>
           getConfigurationEntries(@RequestParam(name = RequestParameters.PROVIDER_NID, required = false) String providerNid,
                                   @RequestParam(name = RequestParameters.PROVIDER_ID, required = false) String providerId,
                                   @RequestParam(name = RequestParameters.VERSION, required = false) String providerVersion,
                                   @RequestParam(name = RequestParameters.CONTENT, required = false) List<String> content,
                                   @RequestParam(name = RequestParameters.TARGET_SPACE, required = false) String targetSpace,
                                   @RequestParam(name = RequestParameters.ORG, required = false, defaultValue = TargetWildcardFilter.ANY_TARGET_WILDCARD) String org,
                                   @RequestParam(name = RequestParameters.SPACE, required = false, defaultValue = TargetWildcardFilter.ANY_TARGET_WILDCARD) String space) {
        ConfigurationFilterDto filterDto = new ConfigurationFilterDto(providerNid,
                                                                      providerId,
                                                                      providerVersion,
                                                                      new CloudTarget(org, space),
                                                                      targetSpace,
                                                                      content);
        return filterConfigurationEntries(asConfigurationFilter(filterDto));
    }

    private ConfigurationFilterDto parseFilterBean(String fXml) {
        return XmlUtil.fromXml(fXml, ConfigurationFilterDto.class, CONFIGURATION_FILTER_SCHEMA_LOCATION);
    }

    @PostMapping(path = "/searches", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<ConfigurationEntriesDto> search(@RequestBody String xml) {
        return filterConfigurationEntries(asConfigurationFilter(parseFilterBean(xml)));
    }

    private ConfigurationFilter asConfigurationFilter(ConfigurationFilterDto bean) {
        String providerId = bean.getProviderId();
        String providerVersion = bean.getProviderVersion();
        String providerNid = bean.getProviderNid();
        Map<String, Object> content = parseContentFilterParameter(bean.getContent());
        CloudTarget target = bean.getCloudTarget() == null ? ConfigurationEntriesUtil.createImplicitCloudTarget(bean.getTargetSpace())
            : bean.getCloudTarget();
        return new ConfigurationFilter(providerNid, providerId, providerVersion, target, content);
    }

    @PostMapping("/purge")
    public ResponseEntity<Void> purgeConfigurationRegistry(HttpServletRequest request, @RequestParam(ORG) String org,
                                                           @RequestParam(SPACE) String space) {
        UserInfo userInfo = SecurityContextUtil.getUserInfo();
        CloudControllerClient client = clientProvider.getControllerClient(userInfo.getName(), org, space, null);
        MtaConfigurationPurger purger = new MtaConfigurationPurger(client, configurationEntryService, configurationSubscriptionService);
        purger.purge(org, space);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                             .build();
    }
}
