package org.cloudfoundry.multiapps.controller.process.dynatrace;

import org.cloudfoundry.multiapps.common.Nullable;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.api.model.ProcessTypeDeserializer;
import org.cloudfoundry.multiapps.controller.api.model.ProcessTypeSerializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public interface DyntraceProcessEntity {

    // It is set to be nullable due to possible null values which will throw exception and fail some Flowable process listeners
    @Nullable
    public abstract String getProcessId();

    @JsonSerialize(using = ProcessTypeSerializer.class)
    @JsonDeserialize(using = ProcessTypeDeserializer.class)
    // It is set to be nullable due to possible null values which will throw exception and fail some Flowable process listeners
    @Nullable
    public abstract ProcessType getProcessType();

    // It is set to be nullable due to possible null values which will throw exception and fail some Flowable process listeners
    @Nullable
    public abstract String getSpaceId();

    // Returns null when used in StartProcessListener in case of SLP
    @Nullable
    public abstract String getMtaId();
}
