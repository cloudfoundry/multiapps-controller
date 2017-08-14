package com.sap.cloud.lm.sl.cf.web.resources.v1;

import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dao.v1.DeployTargetDao;
import com.sap.cloud.lm.sl.cf.core.dto.persistence.PersistentObject;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.v1.DeployTargetDto;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.v1.DeployTargetsDto;
import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;

@Component("deployTargetsResourceV1")
@Path("/targets")
@Produces(MediaType.APPLICATION_XML)
public class DeployTargetsResource extends
    com.sap.cloud.lm.sl.cf.web.resources.DeployTargetsResource<Target, DeployTargetDto, com.sap.cloud.lm.sl.cf.core.dto.persistence.v1.DeployTargetDto, DeployTargetDao> {

    private static final URL TARGET_SCHEMA = DeployTargetsResource.class.getResource("/deploy-target-schema-v1.xsd");

    @Inject
    private DeployTargetDao dao;

    public DeployTargetsResource() {
        super(TARGET_SCHEMA, false);
    }

    protected DeployTargetsResource(URL schemaLocation, boolean disableAuthorization) {
        super(schemaLocation, disableAuthorization);
    }

    @Override
    protected DeployTargetDao getDao() {
        return dao;
    }

    @Override
    protected void validateRequestPayload(Target target) throws ParsingException {
        Map<String, Object> properties = target.getProperties();
        if (properties == null || !isOrgSpaceSpecified(properties)) {
            throw new ParsingException(Messages.ORG_SPACE_NOT_SPECIFIED_1);
        }
        validateTargetIdentifiers(target);
    }

    @Override
    protected DeployTargetsDto getDeployTargetsDto(List<PersistentObject<Target>> targets) {
        return new DeployTargetsDto(targets);
    }

    @Override
    protected DeployTargetDto getDeployTargetDto(PersistentObject<Target> target) {
        return new DeployTargetDto(target);
    }

    @Override
    protected Class<DeployTargetDto> getTargetDtoClass() {
        return DeployTargetDto.class;
    }

}
