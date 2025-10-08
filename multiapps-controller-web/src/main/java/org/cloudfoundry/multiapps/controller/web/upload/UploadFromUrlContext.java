package org.cloudfoundry.multiapps.controller.web.upload;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.Nullable;
import org.cloudfoundry.multiapps.controller.api.model.UserCredentials;
import org.cloudfoundry.multiapps.controller.persistence.model.AsyncUploadJobEntry;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableUploadFromUrlContext.class)
@JsonDeserialize(as = ImmutableUploadFromUrlContext.class)
public interface UploadFromUrlContext {

    AsyncUploadJobEntry getJobEntry();

    String getFileUrl();

    @Nullable
    UserCredentials getUserCredentials();

    AtomicLong getCounterRef();

    LocalDateTime getStartTime();
}
