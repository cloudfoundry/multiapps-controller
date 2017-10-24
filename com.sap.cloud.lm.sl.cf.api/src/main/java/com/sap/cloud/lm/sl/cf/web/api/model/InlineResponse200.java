package com.sap.cloud.lm.sl.cf.web.api.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

public class InlineResponse200 {

    private List<String> actionIds = new ArrayList<String>();

    /**
     **/
    public InlineResponse200 actionIds(List<String> actionIds) {
        this.actionIds = actionIds;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("actionIds")
    public List<String> getActionIds() {
        return actionIds;
    }

    public void setActionIds(List<String> actionIds) {
        this.actionIds = actionIds;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InlineResponse200 inlineResponse200 = (InlineResponse200) o;
        return Objects.equals(actionIds, inlineResponse200.actionIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actionIds);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class InlineResponse200 {\n");

        sb.append("    actionIds: ").append(toIndentedString(actionIds)).append("\n");
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
