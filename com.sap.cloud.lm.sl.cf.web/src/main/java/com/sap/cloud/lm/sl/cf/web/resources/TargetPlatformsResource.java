package com.sap.cloud.lm.sl.cf.web.resources;

import static com.sap.cloud.lm.sl.common.util.CommonUtil.cast;

import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.core.dao.DeployTargetDao;
import com.sap.cloud.lm.sl.cf.core.dto.persistence.DeployTargetDto;
import com.sap.cloud.lm.sl.cf.core.dto.persistence.PersistentObject;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.TargetPlatformDto;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.TargetPlatformsDto;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationUtil;
import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MiscUtil;
import com.sap.cloud.lm.sl.common.util.XmlUtil;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.Schemas;
import com.sap.cloud.lm.sl.mta.model.v1_0.PlatformModuleType;
import com.sap.cloud.lm.sl.mta.model.v1_0.PlatformResourceType;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;
import com.sap.cloud.lm.sl.mta.parsers.v1_0.PlatformModuleTypeParser;
import com.sap.cloud.lm.sl.mta.parsers.v1_0.PlatformResourceTypeParser;
import com.sap.cloud.lm.sl.mta.parsers.v1_0.TargetParser;

@Path("/platforms")
@Produces(MediaType.APPLICATION_XML)

public abstract class TargetPlatformsResource<Tgt extends Target, Dto extends DeployTargetDto<Tgt>, Dao extends DeployTargetDao<Tgt, Dto>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TargetPlatformsResource.class);

    @Inject
    private CloudFoundryClientProvider clientProvider;

    @Context
    protected HttpServletRequest request;

    protected final URL schemaLocation;
    protected final boolean disableAuthorization;

    protected TargetPlatformsResource(URL schemaLocation, boolean disableAuthorization) {
        this.schemaLocation = schemaLocation;
        this.disableAuthorization = disableAuthorization;
    }

    @PUT
    @Path("/{name}")
    @Consumes(MediaType.APPLICATION_XML)
    public Response updatePlatform(@PathParam("name") String name, String xml) throws SLException {
        String action = Messages.UPDATE_TARGET_PLATFORM;
        ensureUserIsAuthorized(action);
        AuditLoggingProvider.getFacade().logConfigUpdate(name);
        try {
            Tgt target = parsePlatformXml(xml, schemaLocation);
            validateRequestPayload(target);
            long targetId = getDao().findByName(target.getName()).getId();
            getDao().merge(targetId, target);
            // CHECKSTYLE:OFF
        } catch (Throwable e) {
            // CHECKSTYLE:ON
            AuditLoggingProvider.getFacade().logConfigUpdated(false);
            throw e;
        }

        AuditLoggingProvider.getFacade().logConfigUpdated(true);
        return Response.status(Status.OK).build();
    }

    @GET
    public Response getAllPlatforms() throws SLException {
        List<Tgt> targets = new ArrayList<Tgt>();
        for (PersistentObject<Tgt> platform : getDao().findAll()) {
            targets.add(platform.getObject());
        }
        TargetPlatformsDto dto = getTargetPlatformsDto(targets);
        return Response.status(Status.OK).entity(XmlUtil.toXml(dto, true)).build();
    }

    @GET
    @Path("/{name}")
    public Response getPlatform(@PathParam("name") String name) throws SLException {
        PersistentObject<Tgt> findByName = getDao().findByName(name);
        TargetPlatformDto dto = getTargetPlatformDto(findByName.getObject());
        return Response.status(Status.OK).entity(XmlUtil.toXml(dto, true)).build();
    }

    @DELETE
    @Path("/{name}")
    public Response deletePlatform(@PathParam("name") String name) throws SLException {
        String action = Messages.DELETE_TARGET_PLATFORM;
        ensureUserIsAuthorized(action);
        AuditLoggingProvider.getFacade().logConfigDelete(name);
        try {
            PersistentObject<? extends Target> target = getDao().findByName(name);
            getDao().remove(target.getId());
            // CHECKSTYLE:OFF
        } catch (Throwable e) {
            // CHECKSTYLE:ON
            AuditLoggingProvider.getFacade().logConfigUpdated(false);
            throw e;
        }

        AuditLoggingProvider.getFacade().logConfigUpdated(true);
        return Response.status(Status.OK).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    public Response createPlatform(String xml) throws SLException {
        String action = Messages.CREATE_TARGET_PLATFORM;
        ensureUserIsAuthorized(action);
        try {
            Tgt target = parsePlatformXml(xml, schemaLocation);
            AuditLoggingProvider.getFacade().logConfigCreate(target.getName());
            validateRequestPayload(target);
            getDao().add(target);
            LOGGER.debug(MessageFormat.format("Persisted target platform: {0}", JsonUtil.toJson(target, true)));
            // CHECKSTYLE:OFF
        } catch (Throwable e) {
            // CHECKSTYLE:ON
            AuditLoggingProvider.getFacade().logConfigUpdated(false);
            throw e;
        }

        AuditLoggingProvider.getFacade().logConfigUpdated(true);
        return Response.status(Status.CREATED).build();
    }

    protected void ensureUserIsAuthorized(String action) {
        if (!disableAuthorization) {
            AuthorizationChecker.ensureUserIsAuthorized(request, clientProvider, SecurityContextUtil.getUserInfo(),
                ConfigurationUtil.getSpaceGuid(), action);
        }
    }

    protected Tgt parsePlatformXml(String xml, URL schemaLocation) throws ParsingException {
        Tgt target = cast(XmlUtil.fromXml(xml, getTargetPlatformDtoClass(), schemaLocation).toTargetPlatform());
        return target;
    }

    protected boolean isOrgSpaceSpecified(Map<String, Object> properties) {
        return properties.containsKey(SupportedParameters.ORG) && properties.containsKey(SupportedParameters.SPACE);
    }

    protected void validatePlatformIdentifiers(Target target) throws ParsingException {
        validateIdentifier(TargetParser.NAME, target.getName());
        validateIdentifier(TargetParser.TYPE, target.getType());
        List<PlatformModuleType> moduleTypes = target.getModuleTypes1_0();
        if (moduleTypes == null) {
            return;
        }
        for (int i = 0; i < moduleTypes.size(); i++) {
            validateIdentifier(String.format("%s#%d#%s", TargetParser.MODULE_TYPES, i, PlatformModuleTypeParser.NAME),
                moduleTypes.get(i).getName());
        }
        List<PlatformResourceType> resourceTypes = target.getResourceTypes1_0();
        if (resourceTypes == null) {
            return;
        }
        for (int i = 0; i < resourceTypes.size(); i++) {
            validateIdentifier(String.format("%s#%d#%s", TargetParser.RESOURCE_TYPES, i, PlatformResourceTypeParser.NAME),
                resourceTypes.get(i).getName());
        }
    }

    protected void validateIdentifier(String identifierName, String identifier) throws ParsingException {
        if (identifier == null) {
            return;
        }

        if (identifier.length() > Schemas.MTA_IDENTIFIER_MAX_LENGTH) {
            throw new ParsingException(com.sap.cloud.lm.sl.mta.message.Messages.VALUE_TOO_LONG, identifierName,
                Schemas.MTA_IDENTIFIER_MAX_LENGTH);
        }

        if (!identifier.matches(Schemas.MTA_IDENTIFIER_PATTERN)) {
            throw new ParsingException(com.sap.cloud.lm.sl.mta.message.Messages.INVALID_STRING_VALUE_FOR_KEY, identifierName,
                MiscUtil.outlineProblematicCharacter(Schemas.MTA_IDENTIFIER_PATTERN, identifier));
        }
    }

    protected abstract DeployTargetDao<Tgt, Dto> getDao();

    protected abstract void validateRequestPayload(Tgt target) throws ParsingException;

    protected abstract TargetPlatformDto getTargetPlatformDto(Tgt target);

    protected abstract TargetPlatformsDto getTargetPlatformsDto(List<Tgt> targets);

    protected abstract Class<? extends TargetPlatformDto> getTargetPlatformDtoClass();

}
