package com.sap.cloud.lm.sl.cf.core.dao;

import static com.sap.cloud.lm.sl.cf.core.dao.filters.ConfigurationFilter.CONTENT_FILTER;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dto.persistence.ConfigurationEntryDto;
import com.sap.cloud.lm.sl.cf.core.dto.persistence.ConfigurationEntryDto.FieldNames;
import com.sap.cloud.lm.sl.cf.core.filters.TargetWildcardFilter;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.NamedQueries;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.NotFoundException;

@Component
public class ConfigurationEntryDtoDao extends AbstractDtoDao<ConfigurationEntryDto, Long> {

    public static final BiFunction<CloudTarget, CloudTarget, Boolean> TARGET_WILDCARD_FILTER = new TargetWildcardFilter();

    @Inject
    public ConfigurationEntryDtoDao(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    public List<ConfigurationEntryDto> find(String providerNid, String providerId, CloudTarget targetSpace,
        Map<String, Object> requiredProperties, String mtaId) {
        return execute(manager -> findInternal(providerNid, providerId, targetSpace, requiredProperties, mtaId, manager));
    }

    private List<ConfigurationEntryDto> findInternal(String providerNid, String providerId, CloudTarget targetSpace,
        Map<String, Object> requiredProperties, String mtaId, EntityManager manager) {
        TypedQuery<ConfigurationEntryDto> query = createQuery(providerNid, providerId, targetSpace, mtaId, manager);
        return filter(query.getResultList(), requiredProperties, targetSpace);
    }

    private TypedQuery<ConfigurationEntryDto> createQuery(String providerNid, String providerId, CloudTarget targetSpace, String mtaId,
        EntityManager manager) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<ConfigurationEntryDto> query = builder.createQuery(ConfigurationEntryDto.class);
        Root<ConfigurationEntryDto> root = query.from(ConfigurationEntryDto.class);

        List<Predicate> predicates = new ArrayList<>();
        if (providerNid != null) {
            predicates.add(builder.equal(root.get(FieldNames.PROVIDER_NID), providerNid));
        }
        if (targetSpace != null) {
            if (!StringUtils.isEmpty(targetSpace.getSpace())) {
                predicates.add(builder.equal(root.get(FieldNames.TARGET_SPACE), targetSpace.getSpace()));
            }
            if (!StringUtils.isEmpty(targetSpace.getOrg())) {
                predicates.add(builder.equal(root.get(FieldNames.TARGET_ORG), targetSpace.getOrg()));
            }
        }

        if (providerId != null) {
            predicates.add(builder.equal(root.get(FieldNames.PROVIDER_ID), providerId));
        } else if (mtaId != null) {
            predicates.add(builder.like(root.get(FieldNames.PROVIDER_ID), mtaId + ":%"));
        }

        return manager.createQuery(query.select(root)
            .where(predicates.toArray(new Predicate[0])));
    }

    private List<ConfigurationEntryDto> filter(List<ConfigurationEntryDto> entries, Map<String, Object> requiredProperties,
        CloudTarget requestedSpace) {

        Stream<ConfigurationEntryDto> stream = entries.stream();
        stream = stream.filter(entry -> CONTENT_FILTER.apply(entry.getContent(), requiredProperties));

        stream = stream
            .filter(entry -> TARGET_WILDCARD_FILTER.apply(new CloudTarget(entry.getTargetOrg(), entry.getTargetSpace()), requestedSpace));
        return stream.collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public List<ConfigurationEntryDto> find(String spaceGuid) {
        return execute(manager -> manager.createNamedQuery(NamedQueries.FIND_ALL_ENTRIES_BY_SPACE_ID)
            .setParameter(ConfigurationEntryDto.FieldNames.SPACE_ID, spaceGuid)
            .getResultList());
    }

    public boolean exists(long id) {
        return execute(manager -> findInternal(manager, id) != null);
    }

    @Override
    protected ConfigurationEntryDto merge(ConfigurationEntryDto existingEntry, ConfigurationEntryDto entry) {
        long id = existingEntry.getPrimaryKey();
        String providerNid = ObjectUtils.firstNonNull(removeDefault(entry.getProviderNid()), existingEntry.getProviderNid());
        String providerId = ObjectUtils.firstNonNull(entry.getProviderId(), existingEntry.getProviderId());
        String targetOrg = ObjectUtils.firstNonNull(entry.getTargetOrg(), existingEntry.getTargetOrg());
        String targetSpace = ObjectUtils.firstNonNull(entry.getTargetSpace(), existingEntry.getTargetSpace());
        String providerVersion = ObjectUtils.firstNonNull(removeDefault(entry.getProviderVersion()), existingEntry.getProviderVersion());
        String content = ObjectUtils.firstNonNull(entry.getContent(), existingEntry.getContent());
        String visibility = ObjectUtils.firstNonNull(entry.getVisibility(), existingEntry.getVisibility());
        String spaceId = ObjectUtils.firstNonNull(entry.getSpaceId(), existingEntry.getSpaceId());
        return new ConfigurationEntryDto(id, providerNid, providerId, providerVersion, targetOrg, targetSpace, content, visibility,
            spaceId);
    }

    private String removeDefault(String value) {
        return value.equals(PersistenceMetadata.NOT_AVAILABLE) ? null : value;
    }

    @Override
    protected void onEntityNotFound(Long id) {
        throw new NotFoundException(Messages.CONFIGURATION_ENTRY_NOT_FOUND, id);
    }

    @Override
    protected void onEntityConflict(ConfigurationEntryDto entry, Throwable t) {
        throw (ConflictException) new ConflictException(Messages.CONFIGURATION_ENTRY_ALREADY_EXISTS, entry.getProviderNid(),
            entry.getProviderId(), entry.getProviderVersion(), entry.getTargetOrg(), entry.getTargetSpace()).initCause(t);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<ConfigurationEntryDto> getDtoClass() {
        return ConfigurationEntryDto.class;
    }

    @Override
    protected String getFindAllNamedQuery() {
        return NamedQueries.FIND_ALL_ENTRIES;
    }

}
