package com.sap.cloud.lm.sl.cf.web.api.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

public class Log {

    private String id = null;
    private java.util.Date lastModified = null;
    private String content = null;
    private Long size = null;
    private String displayName = null;
    private String description = null;
    private String externalInfo = null;

    /**
     **/
    public Log id(String id) {
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
    public Log lastModified(java.util.Date lastModified) {
        this.lastModified = lastModified;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("lastModified")
    public java.util.Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(java.util.Date lastModified) {
        this.lastModified = lastModified;
    }

    /**
     **/
    public Log content(String content) {
        this.content = content;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("content")
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    /**
     **/
    public Log size(Long size) {
        this.size = size;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("size")
    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    /**
     **/
    public Log displayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("displayName")
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     **/
    public Log description(String description) {
        this.description = description;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     **/
    public Log externalInfo(String externalInfo) {
        this.externalInfo = externalInfo;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("externalInfo")
    public String getExternalInfo() {
        return externalInfo;
    }

    public void setExternalInfo(String externalInfo) {
        this.externalInfo = externalInfo;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Log log = (Log) o;
        return Objects.equals(id, log.id) && Objects.equals(lastModified, log.lastModified) && Objects.equals(content, log.content)
            && Objects.equals(size, log.size) && Objects.equals(displayName, log.displayName)
            && Objects.equals(description, log.description) && Objects.equals(externalInfo, log.externalInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, lastModified, content, size, displayName, description, externalInfo);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Log {\n");

        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    lastModified: ").append(toIndentedString(lastModified)).append("\n");
        sb.append("    content: ").append(toIndentedString(content)).append("\n");
        sb.append("    size: ").append(toIndentedString(size)).append("\n");
        sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    externalInfo: ").append(toIndentedString(externalInfo)).append("\n");
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
        return o.toString().replace("\n", "\n    ");
    }
}
