package org.cloudfoundry.multiapps.controller.persistence.query.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.dto.ConfigurationSubscriptionDto;
import org.cloudfoundry.multiapps.controller.persistence.dto.ConfigurationSubscriptionDto.AttributeNames;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationSubscription;
import org.cloudfoundry.multiapps.controller.persistence.query.ConfigurationSubscriptionQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.criteria.ImmutableQueryAttributeRestriction;
import org.cloudfoundry.multiapps.controller.persistence.query.criteria.QueryCriteria;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationSubscriptionService.ConfigurationSubscriptionMapper;

public class ConfigurationSubscriptionQueryImpl extends AbstractQueryImpl<ConfigurationSubscription, ConfigurationSubscriptionQuery>
    implements ConfigurationSubscriptionQuery {

    protected final QueryCriteria queryCriteria = new QueryCriteria();
    private final ConfigurationSubscriptionMapper subscriptionMapper;
    private List<ConfigurationEntry> matchingEntries;

    public ConfigurationSubscriptionQueryImpl(EntityManager entityManager, ConfigurationSubscriptionMapper subscriptionMapper) {
        super(entityManager);
        this.subscriptionMapper = subscriptionMapper;
    }

    @Override
    public ConfigurationSubscriptionQuery id(Long id) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(id)
                                                                       .build());
        return this;
    }

    @Override
    public ConfigurationSubscriptionQuery mtaId(String mtaId) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.MTA_ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(mtaId)
                                                                       .build());
        return this;
    }

    @Override
    public ConfigurationSubscriptionQuery spaceId(String spaceId) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.SPACE_ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(spaceId)
                                                                       .build());
        return this;
    }

    @Override
    public ConfigurationSubscriptionQuery appName(String appName) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.APP_NAME)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(appName)
                                                                       .build());
        return this;
    }

    @Override
    public ConfigurationSubscriptionQuery resourceName(String resourceName) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.RESOURCE_NAME)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(resourceName)
                                                                       .build());
        return this;
    }

    @Override
    public ConfigurationSubscriptionQuery onSelectMatching(List<ConfigurationEntry> entries) {
        this.matchingEntries = entries;
        return this;
    }

    @Override
    public ConfigurationSubscription singleResult() {
        ConfigurationSubscriptionDto dto = executeInTransaction(manager -> createQuery(manager, queryCriteria,
                                                                                       ConfigurationSubscriptionDto.class).getSingleResult());
        ConfigurationSubscription subscription = subscriptionMapper.fromDto(dto);
        if (!matchesEntries(subscription)) {
            throw new NoResultException(Messages.CONFIGURATION_SUBSCRIPTION_MATCHING_ENTRIES_NOT_FOUND_BY_QUERY);
        }
        return subscription;
    }

    private boolean matchesEntries(ConfigurationSubscription subscription) {
        if (matchingEntries == null) {
            return true;
        }
        return subscription.matches(matchingEntries);
    }

    @Override
    public List<ConfigurationSubscription> list() {
        List<ConfigurationSubscriptionDto> dtos = executeInTransaction(manager -> createQuery(manager, queryCriteria,
                                                                                              ConfigurationSubscriptionDto.class).getResultList());
        return dtos.stream()
                   .map(subscriptionMapper::fromDto)
                   .filter(this::matchesEntries)
                   .collect(Collectors.toList());
    }

    @Override
    public int delete() {
        return executeInTransaction(manager -> createDeleteQuery(manager, queryCriteria,
                                                                 ConfigurationSubscriptionDto.class).executeUpdate());
    }

    @Override
    public int deleteAll(String spaceId) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.SPACE_ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(spaceId)
                                                                       .build());
        return executeInTransaction(manager -> createDeleteQuery(manager, queryCriteria,
                                                                 ConfigurationSubscriptionDto.class).executeUpdate());
    }

}