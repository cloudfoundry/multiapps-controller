package com.sap.cloud.lm.sl.cf.web.api.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

public class Module {

    private String moduleName = null;
    private String appName = null;
    private Date createdOn = null;
    private Date updatedOn = null;
    private List<String> providedDendencyNames = new ArrayList<String>();
    private List<String> services = new ArrayList<String>();
    private List<String> uris = new ArrayList<String>();
    private Map<String, Object> deployAttributes = new HashMap<String, Object>();

    /**
     **/
    public Module moduleName(String moduleName) {
        this.moduleName = moduleName;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("moduleName")
    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    /**
     **/
    public Module appName(String appName) {
        this.appName = appName;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("appName")
    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    /**
     **/
    public Module createdOn(Date createdOn) {
        this.createdOn = createdOn;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("createdOn")
    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    /**
     **/
    public Module updatedOn(Date updatedOn) {
        this.updatedOn = updatedOn;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("updatedOn")
    public Date getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(Date updatedOn) {
        this.updatedOn = updatedOn;
    }

    /**
     **/
    public Module providedDendencyNames(List<String> providedDendencyNames) {
        this.providedDendencyNames = providedDendencyNames;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("providedDendencyNames")
    public List<String> getProvidedDendencyNames() {
        return providedDendencyNames;
    }

    public void setProvidedDendencyNames(List<String> providedDendencyNames) {
        this.providedDendencyNames = providedDendencyNames;
    }

    /**
     **/
    public Module services(List<String> services) {
        this.services = services;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("services")
    public List<String> getServices() {
        return services;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }

    /**
     **/
    public Module uris(List<String> uris) {
        this.uris = uris;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("uris")
    public List<String> getUris() {
        return uris;
    }

    public void setUris(List<String> uris) {
        this.uris = uris;
    }

    /**
     **/
    public Module deployAttributes(Map<String, Object> deployAttributes) {
        this.deployAttributes = deployAttributes;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("deployAttributes")
    public Map<String, Object> getDeployAttributes() {
        return deployAttributes;
    }

    public void setDeployAttributes(Map<String, Object> deployAttributes) {
        this.deployAttributes = deployAttributes;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Module module = (Module) o;
        return Objects.equals(moduleName, module.moduleName) && Objects.equals(appName, module.appName)
            && Objects.equals(createdOn, module.createdOn) && Objects.equals(updatedOn, module.updatedOn)
            && Objects.equals(providedDendencyNames, module.providedDendencyNames) && Objects.equals(services, module.services)
            && Objects.equals(uris, module.uris) && Objects.equals(deployAttributes, module.deployAttributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moduleName, appName, createdOn, updatedOn, providedDendencyNames, services, uris, deployAttributes);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Module {\n");

        sb.append("    moduleName: ").append(toIndentedString(moduleName)).append("\n");
        sb.append("    appName: ").append(toIndentedString(appName)).append("\n");
        sb.append("    createdOn: ").append(toIndentedString(createdOn)).append("\n");
        sb.append("    updatedOn: ").append(toIndentedString(updatedOn)).append("\n");
        sb.append("    providedDendencyNames: ").append(toIndentedString(providedDendencyNames)).append("\n");
        sb.append("    services: ").append(toIndentedString(services)).append("\n");
        sb.append("    uris: ").append(toIndentedString(uris)).append("\n");
        sb.append("    deployAttributes: ").append(toIndentedString(deployAttributes)).append("\n");
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
