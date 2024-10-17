package org.cloudfoundry.multiapps.controller.persistence.query.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;

import org.cloudfoundry.multiapps.controller.persistence.dto.PreservedDescriptor;
import org.cloudfoundry.multiapps.controller.persistence.dto.PreservedDescriptorDto;
import org.cloudfoundry.multiapps.controller.persistence.dto.PreservedDescriptorDto.AttributeNames;
import org.cloudfoundry.multiapps.controller.persistence.query.DescriptorPreserverQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.criteria.ImmutableQueryAttributeRestriction;
import org.cloudfoundry.multiapps.controller.persistence.query.criteria.QueryCriteria;
import org.cloudfoundry.multiapps.controller.persistence.services.DescriptorPreserverService.DescriptorPreserverMapper;

public class DescriptorPreserverQueryImpl extends AbstractQueryImpl<PreservedDescriptor, DescriptorPreserverQuery>
    implements DescriptorPreserverQuery {

    private final QueryCriteria queryCriteria = new QueryCriteria();
    private final DescriptorPreserverMapper descriptorPreserverMapper;

    public DescriptorPreserverQueryImpl(EntityManager entityManager, DescriptorPreserverMapper descriptorPreserverMapper) {
        super(entityManager);
        this.descriptorPreserverMapper = descriptorPreserverMapper;
    }

    @Override
    public DescriptorPreserverQuery id(Long id) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(id)
                                                                       .build());
        return this;
    }

    @Override
    public DescriptorPreserverQuery mtaId(String mtaId) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.MTA_ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(mtaId)
                                                                       .build());
        return this;
    }

    @Override
    public DescriptorPreserverQuery spaceId(String spaceId) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.SPACE_ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(spaceId)
                                                                       .build());
        return this;
    }

    @Override
    public DescriptorPreserverQuery namespace(String namespace) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.NAMESPACE)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(namespace)
                                                                       .build());
        return this;
    }

    @Override
    public DescriptorPreserverQuery checksum(String checksum) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.CHECKSUM)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(checksum)
                                                                       .build());
        return this;
    }

    @Override
    public DescriptorPreserverQuery checksumsNotMatch(List<String> checksums) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.CHECKSUM)
                                                                       .condition((expression, value) -> expression.in(checksums)
                                                                                                                   .not())
                                                                       .value(checksums)
                                                                       .build());
        return this;
    }

    @Override
    public DescriptorPreserverQuery olderThan(LocalDateTime time) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.<LocalDateTime> builder()
                                                                       .attribute(AttributeNames.TIMESTAMP)
                                                                       .condition(getCriteriaBuilder()::lessThan)
                                                                       .value(time)
                                                                       .build());
        return this;
    }

    @Override
    public PreservedDescriptor singleResult() throws NoResultException, NonUniqueResultException {
        PreservedDescriptorDto dto = executeInTransaction(manager -> createQuery(manager, queryCriteria,
                                                                                 PreservedDescriptorDto.class).getSingleResult());
        return descriptorPreserverMapper.fromDto(dto);
    }

    @Override
    public List<PreservedDescriptor> list() {
        List<PreservedDescriptorDto> dtos = executeInTransaction(manager -> createQuery(manager, queryCriteria,
                                                                                        PreservedDescriptorDto.class).getResultList());

        return dtos.stream()
                   .map(descriptorPreserverMapper::fromDto)
                   .collect(Collectors.toList());
    }

    @Override
    public int delete() {
        return executeInTransaction(manager -> createDeleteQuery(manager, queryCriteria, PreservedDescriptorDto.class).executeUpdate());
    }

}
