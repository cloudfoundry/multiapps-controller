package com.sap.cloud.lm.sl.cf.web.resources;

import java.net.URL;
import java.text.MessageFormat;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.core.dao.DeployTargetDao;
import com.sap.cloud.lm.sl.cf.core.dto.persistence.PersistentObject;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.DeployTargetDto;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.DeployTargetsDto;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationUtil;
import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.ListUtil;
import com.sap.cloud.lm.sl.common.util.MiscUtil;
import com.sap.cloud.lm.sl.common.util.XmlUtil;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.Schemas;
import com.sap.cloud.lm.sl.mta.model.v1_0.PlatformModuleType;
import com.sap.cloud.lm.sl.mta.model.v1_0.PlatformResourceType;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;
import com.sap.cloud.lm.sl.mta.parsers.v1_0.PlatformModuleTypeParser;
import com.sap.cloud.lm.sl.mta.parsers.v1_0.PlatformResourceTypeParser;
import com.sap.cloud.lm.sl.mta.parsers.v1_0.TargetParser;

@Path("/targets")
@Produces(MediaType.APPLICATION_XML)
public abstract class DeployTargetsResource<Tgt extends Target, SDto extends DeployTargetDto<Tgt>, PDto extends com.sap.cloud.lm.sl.cf.core.dto.persistence.DeployTargetDto<Tgt>, Dao extends DeployTargetDao<Tgt, PDto>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeployTargetsResource.class);

    @Inject
    private CloudFoundryClientProvider clientProvider;

    @Context
    protected HttpServletRequest request;

    protected final URL schemaLocation;
    protected final boolean disableAuthorization;

    protected DeployTargetsResource(URL schemaLocation, boolean disableAuthorization) {
        this.schemaLocation = schemaLocation;
        this.disableAuthorization = disableAuthorization;
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_XML)
    public Response updateTarget(@PathParam("id") long id, String xml) throws SLException {
        String action = Messages.UPDATE_DEPLOY_TARGET;
        ensureUserIsAuthorized(action);
        AuditLoggingProvider.getFacade().logConfigUpdate(String.valueOf(id));
        PersistentObject<Tgt> updated;
        try {
            Tgt target = parseTargetXml(xml, schemaLocation);
            validateRequestPayload(target);
            updated = getDao().merge(id, target);
            // CHECKSTYLE:OFF
        } catch (Throwable e) {
            // CHECKSTYLE:ON
            AuditLoggingProvider.getFacade().logConfigUpdated(false);
            throw e;
        }

        AuditLoggingProvider.getFacade().logConfigUpdated(true);
        return Response.status(Status.OK).entity(XmlUtil.toXml(getDeployTargetDto(updated), true)).build();
    }

    @GET
    public Response getTargets(@QueryParam("name") String name) throws SLException {
        List<PersistentObject<Tgt>> targets;
        if (name == null) {
            targets = getDao().findAll();
        } else {
            targets = ListUtil.asList(getDao().findByName(name));
        }
        String entity = XmlUtil.toXml(getDeployTargetsDto(targets), true);
        return Response.status(Status.OK).entity(entity).build();
    }

    @GET
    @Path("/{id}")
    public Response getTarget(@PathParam("id") long id) throws SLException {
        SDto dto = getDeployTargetDto(getDao().find(id));
        return Response.status(Status.OK).entity(XmlUtil.toXml(dto, true)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteTarget(@PathParam("id") long id) throws SLException {
        String action = Messages.DELETE_DEPLOY_TARGET;
        ensureUserIsAuthorized(action);
        AuditLoggingProvider.getFacade().logConfigDelete(String.valueOf(id));
        try {
            getDao().remove(id);
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
    public Response createTarget(String xml) throws SLException {
        String action = Messages.CREATE_DEPLOY_TARGET;
        ensureUserIsAuthorized(action);
        PersistentObject<Tgt> persisted;
        try {
            Tgt target = parseTargetXml(xml, schemaLocation);
            AuditLoggingProvider.getFacade().logConfigCreate(target.getName());
            validateRequestPayload(target);
            persisted = getDao().add(target);
            LOGGER.debug(MessageFormat.format("Persisted target: {0}", JsonUtil.toJson(target, true)));
            // CHECKSTYLE:OFF
        } catch (Throwable e) {
            // CHECKSTYLE:ON
            AuditLoggingProvider.getFacade().logConfigUpdated(false);
            throw e;
        }

        AuditLoggingProvider.getFacade().logConfigUpdated(true);
        String entity = XmlUtil.toXml(getDeployTargetDto(persisted), true);
        return Response.status(Status.CREATED).entity(entity).build();
    }

    protected void ensureUserIsAuthorized(String action) {
        if (!disableAuthorization) {
            AuthorizationChecker.ensureUserIsAuthorized(request, clientProvider, SecurityContextUtil.getUserInfo(),
                ConfigurationUtil.getSpaceGuid(), action);
        }
    }

    protected Tgt parseTargetXml(String xml, URL schemaLocation) throws ParsingException {
        SDto targetsDto = XmlUtil.fromXml(xml, getTargetDtoClass(), schemaLocation);
        return targetsDto.toDeployTarget().getObject();
    }

    protected boolean isOrgSpaceSpecified(Map<String, Object> properties) {
        return properties.containsKey(SupportedParameters.ORG) && properties.containsKey(SupportedParameters.SPACE);
    }

    protected void validateTargetIdentifiers(Target target) throws ParsingException {
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

    protected abstract Dao getDao();

    protected abstract void validateRequestPayload(Tgt target) throws ParsingException;

    protected abstract SDto getDeployTargetDto(PersistentObject<Tgt> target);

    protected abstract DeployTargetsDto<Tgt, SDto> getDeployTargetsDto(List<PersistentObject<Tgt>> targets);

    protected abstract Class<SDto> getTargetDtoClass();

}
