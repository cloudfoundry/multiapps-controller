package com.sap.cloud.lm.sl.cf.web.api.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

public class Info {

    private Integer apiVersion = null;

    /**
     **/
    public Info apiVersion(Integer apiVersion) {
        this.apiVersion = apiVersion;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("api_version")
    public Integer getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(Integer apiVersion) {
        this.apiVersion = apiVersion;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Info info = (Info) o;
        return Objects.equals(apiVersion, info.apiVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiVersion);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Info {\n");

        sb.append("    apiVersion: ")
          .append(toIndentedString(apiVersion))
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
