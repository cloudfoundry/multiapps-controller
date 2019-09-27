package com.sap.cloud.lm.sl.cf.core.model;

import static com.sap.cloud.lm.sl.cf.core.model.ResourceMetadata.RequestParameters.ORG;
import static com.sap.cloud.lm.sl.cf.core.model.ResourceMetadata.RequestParameters.SPACE;

import java.util.Objects;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.sap.cloud.lm.sl.cf.core.filters.TargetWildcardFilter;

@XmlRootElement(name = "target")
@XmlAccessorType(value = XmlAccessType.FIELD)
public class CloudTarget {

    @XmlElement(name = "org")
    @QueryParam(ORG)
    @DefaultValue(TargetWildcardFilter.ANY_TARGET_WILDCARD)
    private String organizationName;
    @XmlElement(name = "space")
    @QueryParam(SPACE)
    @DefaultValue(TargetWildcardFilter.ANY_TARGET_WILDCARD)
    private String spaceName;

    public CloudTarget() {
        // Required by JAXB.
    }

    public CloudTarget(String organizationName, String spaceName) {
        this.organizationName = organizationName;
        this.spaceName = spaceName;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public String getSpaceName() {
        return spaceName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public void setSpaceName(String spaceName) {
        this.spaceName = spaceName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(organizationName, spaceName);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null) {
            return false;
        }
        if (getClass() != object.getClass()) {
            return false;
        }
        CloudTarget other = (CloudTarget) object;
        return Objects.equals(organizationName, other.organizationName) && Objects.equals(spaceName, other.spaceName);
    }

    @Override
    public String toString() {
        return "CloudTarget [organizationName=" + organizationName + ", spaceName=" + spaceName + "]";
    }

}
