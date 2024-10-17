package org.cloudfoundry.multiapps.controller.persistence.query;

import java.time.LocalDateTime;
import java.util.List;

import org.cloudfoundry.multiapps.controller.persistence.dto.PreservedDescriptor;

public interface DescriptorPreserverQuery extends Query<PreservedDescriptor, DescriptorPreserverQuery> {

    DescriptorPreserverQuery id(Long id);

    DescriptorPreserverQuery mtaId(String mtaId);

    DescriptorPreserverQuery spaceId(String spaceId);

    DescriptorPreserverQuery namespace(String namespace);

    DescriptorPreserverQuery checksum(String checksum);

    DescriptorPreserverQuery checksumsNotMatch(List<String> checksum);

    DescriptorPreserverQuery olderThan(LocalDateTime time);

}
