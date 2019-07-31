package com.sap.cloud.lm.sl.cf.web.api.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

public class Metadata {

    private String id = null;
    private String version = null;

    /**
     **/
    public Metadata id(String id) {
        this.id = id;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     **/
    public Metadata version(String version) {
        this.version = version;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("version")
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Metadata metadata = (Metadata) o;
        return Objects.equals(id, metadata.id) && Objects.equals(version, metadata.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Metadata {\n");

        sb.append("    id: ")
          .append(toIndentedString(id))
          .append("\n");
        sb.append("    version: ")
          .append(toIndentedString(version))
          .append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces (except the first line).
     */
    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString()
                .replace("\n", "\n    ");
    }
}
