package com.sap.cloud.lm.sl.cf.web.api.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

public class Mta {

    private Metadata metadata = null;
    private List<Module> modules;
    private Set<String> services;

    /**
     **/
    public Mta metadata(Metadata metadata) {
        this.metadata = metadata;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("metadata")
    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    /**
     **/
    public Mta modules(List<Module> modules) {
        this.modules = modules;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("modules")
    public List<Module> getModules() {
        return modules;
    }

    public void setModules(List<Module> modules) {
        this.modules = modules;
    }

    /**
     **/
    public Mta services(Set<String> services) {
        this.services = services;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("services")
    public Set<String> getServices() {
        return services;
    }

    public void setServices(Set<String> services) {
        this.services = services;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Mta mta = (Mta) o;
        return Objects.equals(metadata, mta.metadata) && Objects.equals(modules, mta.modules) && Objects.equals(services, mta.services);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metadata, modules, services);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Mta {\n");

        sb.append("    metadata: ").append(toIndentedString(metadata)).append("\n");
        sb.append("    modules: ").append(toIndentedString(modules)).append("\n");
        sb.append("    services: ").append(toIndentedString(services)).append("\n");
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
