package com.sap.cloud.lm.sl.cf.web.resources.v2;

import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dao.v2.DeployTargetDao;
import com.sap.cloud.lm.sl.cf.core.dto.persistence.PersistentObject;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.v2.DeployTargetDto;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.v2.DeployTargetsDto;
import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.ListUtil;
import com.sap.cloud.lm.sl.common.util.XmlUtil;
import com.sap.cloud.lm.sl.mta.model.v2_0.Target;

@Component("deployTargetsResourceV2")
@Path("/v2/targets")
@Produces(MediaType.APPLICATION_XML)
public class DeployTargetsResource extends
    com.sap.cloud.lm.sl.cf.web.resources.DeployTargetsResource<Target, DeployTargetDto, com.sap.cloud.lm.sl.cf.core.dto.persistence.v2.DeployTargetDto, DeployTargetDao> {

    private static final URL TARGET_SCHEMA = DeployTargetsResource.class.getResource("/deploy-target-schema-v2.xsd");

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
    protected Target parseTargetXml(String xml, URL schemaLocation) throws ParsingException {
        return XmlUtil.fromXml(xml, getTargetDtoClass(), schemaLocation).toDeployTarget().getObject();
    }

    @Override
    protected void validateRequestPayload(Target target) throws ParsingException {
        Map<String, Object> parameters = target.getParameters();
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
        Target target2_0 = (Target) target.getObject();
        return new DeployTargetDto(new PersistentObject<Target>(target.getId(), target2_0));
    }

    @Override
    protected Class<DeployTargetDto> getTargetDtoClass() {
        return DeployTargetDto.class;
    }
}
