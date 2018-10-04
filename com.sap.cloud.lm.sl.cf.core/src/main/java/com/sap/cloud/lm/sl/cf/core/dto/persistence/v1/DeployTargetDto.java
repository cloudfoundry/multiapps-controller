package com.sap.cloud.lm.sl.cf.core.dto.persistence.v1;

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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.sap.cloud.lm.sl.cf.core.dto.persistence.PersistentObject;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.SequenceNames;
import com.sap.cloud.lm.sl.common.util.XmlUtil;
import com.sap.cloud.lm.sl.mta.model.v1.Target;

@Entity(name = "DeployTarget1")
@Access(AccessType.FIELD)
@Table(name = "deploy_target_v1")
@SequenceGenerator(name = SequenceNames.DEPLOY_TARGET_SEQUENCE, sequenceName = SequenceNames.DEPLOY_TARGET_SEQUENCE, initialValue = 1, allocationSize = 1)
@NamedQuery(name = "find_all_v1", query = "SELECT dt FROM DeployTarget1 dt")
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
        // Required by JPA
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
            .toXml(new com.sap.cloud.lm.sl.cf.core.dto.serialization.v1.DeployTargetDto(new PersistentObject<Target>(id, target)));
    }

    @Override
    public PersistentObject<Target> toDeployTarget() {
        PersistentObject<Target> persistentTarget = cast(
            XmlUtil.fromXml(xmlContent, com.sap.cloud.lm.sl.cf.core.dto.serialization.v1.DeployTargetDto.class)
                .toDeployTarget());
        persistentTarget.setId(id);
        return persistentTarget;
    }

    public long getId() {
        return id;
    }
}
