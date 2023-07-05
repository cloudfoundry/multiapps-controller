package org.cloudfoundry.multiapps.controller.persistence.query;

import org.cloudfoundry.multiapps.controller.persistence.model.AsyncUploadJobEntry;

import java.time.LocalDateTime;

public interface AsyncUploadJobsQuery extends Query<AsyncUploadJobEntry, AsyncUploadJobsQuery> {

    AsyncUploadJobsQuery id(String id);

    AsyncUploadJobsQuery spaceGuid(String spaceGuid);

    AsyncUploadJobsQuery state(AsyncUploadJobEntry.State state);

    AsyncUploadJobsQuery namespace(String namespace);

    AsyncUploadJobsQuery user(String user);

    AsyncUploadJobsQuery url(String url);

    AsyncUploadJobsQuery withoutFinishedAt();

    AsyncUploadJobsQuery withStateAnyOf(AsyncUploadJobEntry.State... states);

    AsyncUploadJobsQuery startedBefore(LocalDateTime startedBefore);
}
