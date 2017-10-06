package com.sap.cloud.lm.sl.cf.core.dto.serialization;

import java.util.List;

import javax.xml.bind.annotation.XmlTransient;

import com.sap.cloud.lm.sl.cf.core.dto.persistence.PersistentObject;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;

@XmlTransient
public abstract class DeployTargetsDto<Tgt extends Target, Dto extends DeployTargetDto<Tgt>> {

    public abstract List<PersistentObject<Tgt>> getDeployTargets();

    @XmlTransient
    public abstract List<Dto> getDeployTargetDtos();
}
