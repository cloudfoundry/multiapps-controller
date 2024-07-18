package org.cloudfoundry.multiapps.controller.persistence.model;

import java.util.Objects;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;

@XmlRootElement(name = "target")
@XmlAccessorType(value = XmlAccessType.FIELD)
public class CloudTarget {

    @XmlElement(name = "org")
    @JsonProperty("org")
    private String organizationName;
    @XmlElement(name = "space")
    @JsonProperty("space")
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
