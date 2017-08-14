package com.sap.cloud.lm.sl.cf.web.resources.v3;

import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dao.v3.DeployTargetDao;
import com.sap.cloud.lm.sl.cf.core.dto.persistence.PersistentObject;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.v3.DeployTargetDto;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.v3.DeployTargetsDto;
import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.ListUtil;
import com.sap.cloud.lm.sl.mta.model.v3_1.Target;

@Component("deployTargetsResourceV3")
@Path("/v3/targets")
@Produces(MediaType.APPLICATION_XML)
public class DeployTargetsResource extends
    com.sap.cloud.lm.sl.cf.web.resources.DeployTargetsResource<Target, DeployTargetDto, com.sap.cloud.lm.sl.cf.core.dto.persistence.v3.DeployTargetDto, DeployTargetDao> {

    private static final URL PLATFORM_SCHEMA = DeployTargetsResource.class.getResource("/deploy-target-schema-v3.xsd");

    @Inject
    private DeployTargetDao dao;

    public DeployTargetsResource() {
        super(PLATFORM_SCHEMA, false);
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
        Map<String, Object> parameters = ((Target) target).getParameters();
        if (parameters == null || !isOrgSpaceSpecified(parameters)) {
            throw new ParsingException(Messages.ORG_SPACE_NOT_SPECIFIED_2);
        }
        validateTargetIdentifiers(target);
    }

    @Override
    protected DeployTargetsDto getDeployTargetsDto(List<PersistentObject<Target>> targets) {
        return new DeployTargetsDto(ListUtil.cast(targets));
    }

    @Override
    protected DeployTargetDto getDeployTargetDto(PersistentObject<Target> target) {
        Target target3_1 = (Target) target.getObject();
        return new DeployTargetDto(new PersistentObject<>(target.getId(), target3_1));
    }

    @Override
    protected Class<DeployTargetDto> getTargetDtoClass() {
        return DeployTargetDto.class;
    }

}
