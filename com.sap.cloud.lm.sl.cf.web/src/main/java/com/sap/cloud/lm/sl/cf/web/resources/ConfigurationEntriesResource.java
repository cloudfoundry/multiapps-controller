package com.sap.cloud.lm.sl.cf.web.resources;

import static com.sap.cloud.lm.sl.cf.core.model.ResourceMetadata.RequestParameters.ID;
import static com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil.findConfigurationEntries;
import static java.text.MessageFormat.format;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.gson.reflect.TypeToken;
import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationSubscriptionDao;
import com.sap.cloud.lm.sl.cf.core.dto.ConfigurationEntryDto;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.ConfigurationEntriesDto;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.ConfigurationFilterDto;
import com.sap.cloud.lm.sl.cf.core.helpers.ConfigurationEntriesPurger;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationFilter;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;
import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.CommonUtil;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.XmlUtil;

@Component
@Path("/configuration-entries")
@Produces(MediaType.APPLICATION_XML)
public class ConfigurationEntriesResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationEntriesResource.class);

    private static final String KEYVALUE_SEPARATOR = ":";
    private static final String PURGE_COMMAND = "Purge configuration entries and subscriptions";

    private static final URL CREATE_CONFIGURATION_ENTRY_SCHEMA_LOCATION = ConfigurationEntriesResource.class.getResource(
        "/create-configuration-entry-schema.xsd");
    private static final URL UPDATE_CONFIGURATION_ENTRY_SCHEMA_LOCATION = ConfigurationEntriesResource.class.getResource(
        "/update-configuration-entry-schema.xsd");
    private static final URL CONFIGURATION_FILTER_SCHEMA_LOCATION = ConfigurationEntriesResource.class.getResource(
        "/configuration-filter-schema.xsd");

    @Inject
    private ConfigurationEntryDao entryDao;

    @Inject
    private ConfigurationSubscriptionDao subscriptionDao;

    @Inject
    private CloudFoundryClientProvider clientProvider;

    @Context
    private HttpServletRequest request;

    protected Response filterConfigurationEntries(ConfigurationFilter filter) throws ParsingException {
        try {
            List<ConfigurationEntry> entries = findConfigurationEntries(entryDao, filter);
            return Response.status(Response.Status.OK).entity(wrap(entries)).build();
        } catch (IllegalArgumentException e) {
            /**
             * Thrown if the version parameter is not a valid version requirement.
             */
            throw new ParsingException(e.getMessage(), e);
        }
    }

    private ConfigurationEntriesDto wrap(List<ConfigurationEntry> entries) {
        List<ConfigurationEntryDto> dtos = new ArrayList<>();
        for (ConfigurationEntry entry : entries) {
            dtos.add(new ConfigurationEntryDto(entry));
        }
        return new ConfigurationEntriesDto(dtos);
    }

    @Path("/{id}")
    @GET
    public Response getConfigurationEntry(@PathParam(ID) long id) throws SLException {
        return Response.status(Response.Status.OK).entity(new ConfigurationEntryDto(entryDao.find(id))).build();
    }

    private Map<String, String> parseContentFilterParameter(List<String> content) throws ParsingException {
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

    private Map<String, String> parseContentQueryJsonParameter(List<String> content) throws ParsingException {
        return JsonUtil.fromJson(content.get(0), new TypeToken<Map<String, String>>() {
        }.getType());
    }

    private Map<String, String> parseContentQueryListParameter(List<String> content) throws ParsingException {
        Map<String, String> parsedContent = new HashMap<String, String>();
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
    public Response createConfigurationEntry(String xml) throws SLException {
        ConfigurationEntryDto dto = parseDto(xml, CREATE_CONFIGURATION_ENTRY_SCHEMA_LOCATION);

        ConfigurationEntry result = entryDao.add(dto.toConfigurationEntry());

        return Response.status(Response.Status.CREATED).entity(new ConfigurationEntryDto(result)).build();
    }

    @Path("/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_XML)
    public Response updateConfigurationEntry(@PathParam(ID) long id, String xml) throws SLException {
        ConfigurationEntryDto dto = parseDto(xml, UPDATE_CONFIGURATION_ENTRY_SCHEMA_LOCATION);
        if (dto.getId() != 0 && dto.getId() != id) {
            throw new ParsingException(Messages.CONFIGURATION_ENTRY_ID_CANNOT_BE_UPDATED, id);
        }

        ConfigurationEntry result = entryDao.update(id, dto.toConfigurationEntry());

        return Response.status(Response.Status.OK).entity(new ConfigurationEntryDto(result)).build();
    }

    private ConfigurationEntryDto parseDto(String dXml, URL schemaLocation) throws ParsingException {
        return XmlUtil.fromXml(dXml, ConfigurationEntryDto.class, schemaLocation);
    }

    @Path("/{id}")
    @DELETE
    public Response deleteConfigurationEntry(@PathParam(ID) long id) throws SLException {
        entryDao.remove(id);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @GET
    public Response getConfigurationEntries(@BeanParam ConfigurationFilterDto filterDto) throws SLException {
        return filterConfigurationEntries(asConfigurationFilter(filterDto));
    }

    private ConfigurationFilterDto parseFilterBean(String fXml) throws ParsingException {
        return XmlUtil.fromXml(fXml, ConfigurationFilterDto.class, CONFIGURATION_FILTER_SCHEMA_LOCATION);
    }

    @Path("/searches")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    public Response search(String xml) throws SLException {
        return filterConfigurationEntries(asConfigurationFilter(parseFilterBean(xml)));
    }

    private ConfigurationFilter asConfigurationFilter(ConfigurationFilterDto bean) throws SLException {
        String providerId = bean.getProviderId();
        String providerVersion = bean.getProviderVersion();
        String providerNid = bean.getProviderNid();
        Map<String, String> content = parseContentFilterParameter(bean.getContent());
        String targetSpace = bean.getTargetSpace();
        return new ConfigurationFilter(providerNid, providerId, providerVersion, targetSpace, content);
    }

    @Path("/purge")
    @POST
    public Response purgeConfigurationRegistry(@QueryParam("org") String org, @QueryParam("space") String space) {
        if (CommonUtil.isNullOrEmpty(org) || CommonUtil.isNullOrEmpty(space)) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Messages.ORG_AND_SPACE_MUST_BE_SPECIFIED).build();
        }

        UserInfo userInfo = SecurityContextUtil.getUserInfo();
        AuthorizationChecker.ensureUserIsAuthorized(request, clientProvider, userInfo, org, space, PURGE_COMMAND);
        CloudFoundryOperations client = clientProvider.getCloudFoundryClient(userInfo.getName(), org, space, null);
        ConfigurationEntriesPurger.purge(client, org, space, entryDao, subscriptionDao);
        return Response.status(Response.Status.NO_CONTENT).build();
    }
}
