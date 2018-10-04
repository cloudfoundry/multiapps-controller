package com.sap.cloud.lm.sl.cf.core.dto.serialization;

import javax.xml.bind.annotation.XmlTransient;

import com.sap.cloud.lm.sl.cf.core.dto.persistence.PersistentObject;
import com.sap.cloud.lm.sl.mta.model.v1.Target;

@XmlTransient
public abstract class DeployTargetDto<Tgt extends Target> {

    public abstract PersistentObject<Tgt> toDeployTarget();

}
