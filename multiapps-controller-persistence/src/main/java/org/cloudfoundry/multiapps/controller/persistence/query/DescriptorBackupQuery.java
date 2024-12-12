package org.cloudfoundry.multiapps.controller.persistence.query;

import java.time.LocalDateTime;
import java.util.List;

import org.cloudfoundry.multiapps.controller.persistence.dto.BackupDescriptor;

public interface DescriptorBackupQuery extends Query<BackupDescriptor, DescriptorBackupQuery> {

    DescriptorBackupQuery id(Long id);

    DescriptorBackupQuery mtaId(String mtaId);

    DescriptorBackupQuery spaceId(String spaceId);

    DescriptorBackupQuery namespace(String namespace);

    DescriptorBackupQuery checksum(String checksum);

    DescriptorBackupQuery checksumsNotMatch(List<String> checksum);

    DescriptorBackupQuery olderThan(LocalDateTime time);

}
