package com.sap.cloud.lm.sl.cf.core.dto.persistence.v2;

import static com.sap.cloud.lm.sl.common.util.CommonUtil.cast;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import com.sap.cloud.lm.sl.cf.core.dto.persistence.PersistentObject;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.SequenceNames;
import com.sap.cloud.lm.sl.common.util.XmlUtil;
import com.sap.cloud.lm.sl.mta.model.v2_0.Target;

@Entity(name = "DeployTarget2")
@Access(AccessType.FIELD)
@NamedQuery(name = "find_all_v2", query = "SELECT dt FROM DeployTarget2 dt")
@Table(name = "deploy_target_v2")
public class DeployTargetDto extends com.sap.cloud.lm.sl.cf.core.dto.persistence.DeployTargetDto<Target> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = SequenceNames.DEPLOY_TARGET_SEQUENCE)
    @Column(name = "id", nullable = false)
    private long id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String type;
    @Lob
    @Column(name = "xml_content", nullable = false)
    protected String xmlContent;

    protected DeployTargetDto() {
        // required by JPA
    }

    public DeployTargetDto(Target target) {
        setDeployTarget(target);
    }

    public DeployTargetDto(long id, Target target) {
        this.id = id;
        setDeployTarget(target);
    }

    @Override
    public void setDeployTarget(Target target) {
        this.name = target.getName();
        this.type = target.getType();
        this.xmlContent = XmlUtil
            .toXml(new com.sap.cloud.lm.sl.cf.core.dto.serialization.v2.DeployTargetDto(new PersistentObject<Target>(id, target)));
    }

    @Override
    public PersistentObject<Target> toDeployTarget() {
        com.sap.cloud.lm.sl.cf.core.dto.serialization.v2.DeployTargetDto xmlDto = XmlUtil.fromXml(xmlContent,
            com.sap.cloud.lm.sl.cf.core.dto.serialization.v2.DeployTargetDto.class);
        PersistentObject<Target> persistentTarget = cast(xmlDto.toDeployTarget());
        persistentTarget.setId(id);
        return persistentTarget;
    }

}
