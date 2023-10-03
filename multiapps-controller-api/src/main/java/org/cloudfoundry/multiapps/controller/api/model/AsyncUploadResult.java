package org.cloudfoundry.multiapps.controller.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModelProperty;
import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableAsyncUploadResult.class)
@JsonDeserialize(as = ImmutableAsyncUploadResult.class)
public interface AsyncUploadResult {

    enum JobStatus {
        RUNNING, FINISHED, ERROR
    }

    @ApiModelProperty
    @JsonProperty("status")
    JobStatus getStatus();

    @Nullable
    @ApiModelProperty
    @JsonProperty("bytes_processed")
    Long getBytes();

    @Nullable
    @ApiModelProperty
    @JsonProperty("error")
    String getError();

    @Nullable
    @ApiModelProperty
    @JsonProperty("file")
    FileMetadata getFile();

    @Nullable
    @ApiModelProperty
    @JsonProperty("mta_id")
    String getMtaId();
}
