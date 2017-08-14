package com.sap.cloud.lm.sl.cf.core.dto.serialization;

import java.util.List;

import javax.xml.bind.annotation.XmlTransient;

import com.sap.cloud.lm.sl.mta.model.v1_0.Target;

@XmlTransient
@Deprecated
public abstract class TargetPlatformsDto {

    public abstract List<Target> getTargetPlatforms();

}
