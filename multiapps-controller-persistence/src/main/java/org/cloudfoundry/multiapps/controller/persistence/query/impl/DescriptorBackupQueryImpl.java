package org.cloudfoundry.multiapps.controller.persistence.query.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import org.cloudfoundry.multiapps.controller.persistence.dto.BackupDescriptor;
import org.cloudfoundry.multiapps.controller.persistence.dto.BackupDescriptorDto;
import org.cloudfoundry.multiapps.controller.persistence.dto.BackupDescriptorDto.AttributeNames;
import org.cloudfoundry.multiapps.controller.persistence.query.DescriptorBackupQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.criteria.ImmutableQueryAttributeRestriction;
import org.cloudfoundry.multiapps.controller.persistence.query.criteria.QueryCriteria;
import org.cloudfoundry.multiapps.controller.persistence.services.DescriptorBackupService.DescriptorBackupMapper;

public class DescriptorBackupQueryImpl extends AbstractQueryImpl<BackupDescriptor, DescriptorBackupQuery> implements DescriptorBackupQuery {

    private final QueryCriteria queryCriteria = new QueryCriteria();
    private final DescriptorBackupMapper descriptorBackupMapper;

    public DescriptorBackupQueryImpl(EntityManager entityManager, DescriptorBackupMapper descriptorBackupMapper) {
        super(entityManager);
        this.descriptorBackupMapper = descriptorBackupMapper;
    }

    @Override
    public DescriptorBackupQuery id(Long id) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(id)
                                                                       .build());
        return this;
    }

    @Override
    public DescriptorBackupQuery mtaId(String mtaId) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.MTA_ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(mtaId)
                                                                       .build());
        return this;
    }

    @Override
    public DescriptorBackupQuery spaceId(String spaceId) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.SPACE_ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(spaceId)
                                                                       .build());
        return this;
    }

    @Override
    public DescriptorBackupQuery namespace(String namespace) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.NAMESPACE)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(namespace)
                                                                       .build());
        return this;
    }

    @Override
    public DescriptorBackupQuery mtaVersion(String mtaVersion) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.MTA_VERSION)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(mtaVersion)
                                                                       .build());
        return this;
    }

    @Override
    public DescriptorBackupQuery mtaVersionsNotMatch(List<String> mtaVersions) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.MTA_VERSION)
                                                                       .condition((expression, value) -> expression.in(mtaVersions)
                                                                                                                   .not())
                                                                       .value(mtaVersions)
                                                                       .build());
        return this;
    }

    @Override
    public DescriptorBackupQuery olderThan(LocalDateTime time) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.<LocalDateTime> builder()
                                                                       .attribute(AttributeNames.TIMESTAMP)
                                                                       .condition(getCriteriaBuilder()::lessThan)
                                                                       .value(time)
                                                                       .build());
        return this;
    }

    @Override
    public BackupDescriptor singleResult() throws NoResultException, NonUniqueResultException {
        BackupDescriptorDto dto = executeInTransaction(manager -> createQuery(manager, queryCriteria,
                                                                              BackupDescriptorDto.class).getSingleResult());
        return descriptorBackupMapper.fromDto(dto);
    }

    @Override
    public List<BackupDescriptor> list() {
        List<BackupDescriptorDto> dtos = executeInTransaction(manager -> createQuery(manager, queryCriteria,
                                                                                     BackupDescriptorDto.class).getResultList());

        return dtos.stream()
                   .map(descriptorBackupMapper::fromDto)
                   .collect(Collectors.toList());
    }

    @Override
    public int delete() {
        return executeInTransaction(manager -> createDeleteQuery(manager, queryCriteria, BackupDescriptorDto.class).executeUpdate());
    }

}
