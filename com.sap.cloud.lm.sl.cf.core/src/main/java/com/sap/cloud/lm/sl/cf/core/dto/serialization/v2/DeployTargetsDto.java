package com.sap.cloud.lm.sl.cf.core.dto.serialization.v2;

import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.sap.cloud.lm.sl.cf.core.dto.persistence.PersistentObject;
import com.sap.cloud.lm.sl.common.util.ListUtil;
import com.sap.cloud.lm.sl.mta.model.v2_0.Target;

@XmlAccessorType(value = javax.xml.bind.annotation.XmlAccessType.FIELD)
@XmlRootElement(name = "deployTargets")
public class DeployTargetsDto extends com.sap.cloud.lm.sl.cf.core.dto.serialization.DeployTargetsDto<Target, DeployTargetDto> {

    @XmlJavaTypeAdapter(TargetAdapter.class)
    @XmlElement(name = "deployTarget")
    private List<PersistentObject<? extends Target>> deployTargets;

    public DeployTargetsDto() {
        // Required by JAXB
    }

    public DeployTargetsDto(List<PersistentObject<? extends Target>> deployTargets) {
        this.deployTargets = deployTargets;
    }

    @Override
    public List<PersistentObject<Target>> getDeployTargets() {
        return ListUtil.cast(deployTargets);
    }

    @Override
    public List<DeployTargetDto> getDeployTargetDtos() {
        return deployTargets.stream()
            .map((target) -> new DeployTargetDto(target))
            .collect(Collectors.toList());
    }
}

class TargetAdapter extends XmlAdapter<DeployTargetDto, PersistentObject<Target>> {

    @Override
    public PersistentObject<Target> unmarshal(DeployTargetDto dto) throws Exception {
        return dto.toDeployTarget();
    }

    @Override
    public DeployTargetDto marshal(PersistentObject<Target> target) {
        return new DeployTargetDto((PersistentObject<? extends Target>) target);
    }
}
