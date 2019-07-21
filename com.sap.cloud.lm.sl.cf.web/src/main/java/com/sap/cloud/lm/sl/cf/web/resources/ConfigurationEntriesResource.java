package com.sap.cloud.lm.sl.cf.web.resources;

import static com.sap.cloud.lm.sl.cf.core.model.ResourceMetadata.RequestParameters.ID;
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
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationSubscriptionDao;
import com.sap.cloud.lm.sl.cf.core.dao.filters.ConfigurationFilter;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.ConfigurationEntriesDto;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.ConfigurationEntryDto;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.ConfigurationFilterDto;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaConfigurationPurger;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;
import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.cf.web.security.AuthorizationChecker;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.XmlUtil;

@Component
@Path("/configuration-entries")
@Produces(MediaType.APPLICATION_XML)
public class ConfigurationEntriesResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationEntriesResource.class);

    private static final String KEYVALUE_SEPARATOR = ":";
    private static final String PURGE_COMMAND = "Purge configuration entries and subscriptions";

    private static final URL CREATE_CONFIGURATION_ENTRY_SCHEMA_LOCATION = ConfigurationEntriesResource.class
        .getResource("/create-configuration-entry-schema.xsd");
    private static final URL UPDATE_CONFIGURATION_ENTRY_SCHEMA_LOCATION = ConfigurationEntriesResource.class
        .getResource("/update-configuration-entry-schema.xsd");
    private static final URL CONFIGURATION_FILTER_SCHEMA_LOCATION = ConfigurationEntriesResource.class
        .getResource("/configuration-filter-schema.xsd");
    protected Supplier<UserInfo> userInfoSupplier = SecurityContextUtil::getUserInfo;

    @Inject
    private ConfigurationEntryDao entryDao;

    @Inject
    private ConfigurationSubscriptionDao subscriptionDao;

    @Inject
    private CloudControllerClientProvider clientProvider;

    @Inject
    private AuthorizationChecker authorizationChecker;

    @Inject
    private ApplicationConfiguration configuration;

    @Context
    private HttpServletRequest request;

    protected Response filterConfigurationEntries(ConfigurationFilter filter) {
        try {
            CloudTarget globalConfigTarget = getGlobalConfigTarget(configuration);
            List<ConfigurationEntry> entries = findConfigurationEntries(entryDao, filter, getUserTargets(), globalConfigTarget);
            return Response.status(Response.Status.OK)
                .entity(wrap(entries))
                .build();
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
            .getName(), cloudSpace.getName());
    }

    private ConfigurationEntriesDto wrap(List<ConfigurationEntry> entries) {
        return new ConfigurationEntriesDto(entries.stream()
            .map(ConfigurationEntryDto::new)
            .collect(Collectors.toList()));
    }

    @Path("/{id}")
    @GET
    public Response getConfigurationEntry(@PathParam(ID) long id) {
        return Response.status(Response.Status.OK)
            .entity(new ConfigurationEntryDto(entryDao.find(id)))
            .build();
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

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    public Response createConfigurationEntry(String xml) {

        ConfigurationEntryDto dto = parseDto(xml, CREATE_CONFIGURATION_ENTRY_SCHEMA_LOCATION);
        ConfigurationEntry configurationEntry = dto.toConfigurationEntry();
        if (configurationEntry.getTargetSpace() == null) {
            throw new ParsingException(Messages.ORG_SPACE_NOT_SPECIFIED_2);
        }
        entryDao.add(configurationEntry);
        AuditLoggingProvider.getFacade()
            .logConfigCreate(configurationEntry);
        return Response.status(Response.Status.CREATED)
            .entity(new ConfigurationEntryDto(configurationEntry))
            .build();
        // TODO: check if this would work fine:
        // return Response.status(Response.Status.CREATED).entity(dto).build();
    }

    @Path("/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_XML)
    public Response updateConfigurationEntry(@PathParam(ID) long id, String xml) {
        ConfigurationEntryDto dto = parseDto(xml, UPDATE_CONFIGURATION_ENTRY_SCHEMA_LOCATION);
        if (dto.getId() != 0 && dto.getId() != id) {
            throw new ParsingException(Messages.CONFIGURATION_ENTRY_ID_CANNOT_BE_UPDATED, id);
        }

        ConfigurationEntry result = entryDao.update(id, dto.toConfigurationEntry());
        AuditLoggingProvider.getFacade()
            .logConfigUpdate(result);

        return Response.status(Response.Status.OK)
            .entity(new ConfigurationEntryDto(result))
            .build();
    }

    private ConfigurationEntryDto parseDto(String dXml, URL schemaLocation) {
        return XmlUtil.fromXml(dXml, ConfigurationEntryDto.class, schemaLocation);
    }

    @Path("/{id}")
    @DELETE
    public Response deleteConfigurationEntry(@PathParam(ID) long id) {
        ConfigurationEntry entry = entryDao.find(id);
        if (entry == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .build();
        }
        entryDao.remove(id);
        AuditLoggingProvider.getFacade()
            .logConfigDelete(entry);
        return Response.status(Response.Status.NO_CONTENT)
            .build();
    }

    @GET
    public Response getConfigurationEntries(@BeanParam ConfigurationFilterDto filterDto) {
        return filterConfigurationEntries(asConfigurationFilter(filterDto));
    }

    private ConfigurationFilterDto parseFilterBean(String fXml) {
        return XmlUtil.fromXml(fXml, ConfigurationFilterDto.class, CONFIGURATION_FILTER_SCHEMA_LOCATION);
    }

    @Path("/searches")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    public Response search(String xml) {
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

    @Path("/purge")
    @POST
    public Response purgeConfigurationRegistry(@QueryParam("org") String org, @QueryParam("space") String space) {
        if (StringUtils.isEmpty(org) || StringUtils.isEmpty(space)) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Messages.ORG_AND_SPACE_MUST_BE_SPECIFIED)
                .build();
        }

        UserInfo userInfo = SecurityContextUtil.getUserInfo();
        authorizationChecker.ensureUserIsAuthorized(request, userInfo, org, space, PURGE_COMMAND);
        CloudControllerClient client = clientProvider.getControllerClient(userInfo.getName(), org, space, null);
        MtaConfigurationPurger purger = new MtaConfigurationPurger(client, entryDao, subscriptionDao);
        purger.purge(org, space);
        return Response.status(Response.Status.NO_CONTENT)
            .build();
    }
}
