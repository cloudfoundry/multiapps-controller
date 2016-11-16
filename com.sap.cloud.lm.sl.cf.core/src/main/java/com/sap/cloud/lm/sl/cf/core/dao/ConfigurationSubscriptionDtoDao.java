package com.sap.cloud.lm.sl.cf.core.dao;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.RollbackException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dto.persistence.ConfigurationSubscriptionDto;
import com.sap.cloud.lm.sl.cf.core.dto.persistence.ConfigurationSubscriptionDto.FieldNames;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata.NamedQueries;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.util.CommonUtil;

@Component
public class ConfigurationSubscriptionDtoDao {

    @Autowired
    @Qualifier("configurationSubscriptionEntityManagerFactory")
    protected EntityManagerFactory emf;

    @SuppressWarnings("unchecked")
    public List<ConfigurationSubscriptionDto> findAll() {
        return new Executor<List<ConfigurationSubscriptionDto>>(createEntityManager()).execute((manager) -> {

            return manager.createNamedQuery(NamedQueries.FIND_ALL_SUBSCRIPTIONS).getResultList();

        });
    }

    public List<ConfigurationSubscriptionDto> findAll(String mtaId, String appName, String spaceId, String resourceName) {
        return new Executor<List<ConfigurationSubscriptionDto>>(createEntityManager()).execute((manager) -> {

            return findAllInternal(mtaId, appName, spaceId, resourceName, manager);

        });
    }

    private List<ConfigurationSubscriptionDto> findAllInternal(String mtaId, String appName, String spaceId, String resourceName,
        EntityManager manager) {
        TypedQuery<ConfigurationSubscriptionDto> query = createQuery(mtaId, appName, spaceId, resourceName, manager);

        setQueryParameter(query, ConfigurationSubscriptionDto.FieldNames.SPACE_ID, spaceId);
        setQueryParameter(query, ConfigurationSubscriptionDto.FieldNames.APP_NAME, appName);
        setQueryParameter(query, ConfigurationSubscriptionDto.FieldNames.RESOURCE_NAME, resourceName);
        setQueryParameter(query, ConfigurationSubscriptionDto.FieldNames.MTA_ID, mtaId);

        return query.getResultList();
    }

    public ConfigurationSubscriptionDto find(long id) throws NotFoundException {
        return new Executor<ConfigurationSubscriptionDto>(createEntityManager()).execute((manager) -> {

            ConfigurationSubscriptionDto subscription = findInternal(id, manager);
            if (subscription == null) {
                throw new NotFoundException(Messages.CONFIGURATION_SUBSCRIPTION_NOT_FOUND, id);
            }
            return subscription;

        });
    }

    public ConfigurationSubscriptionDto add(ConfigurationSubscriptionDto subscription) throws ConflictException {
        try {
            return new TransactionalExecutor<ConfigurationSubscriptionDto>(createEntityManager()).execute((manager) -> {

                manager.persist(subscription);
                return subscription;

            });
        } catch (RollbackException e) {
            throw new ConflictException(e, Messages.CONFIGURATION_SUBSCRIPTION_ALREADY_EXISTS, subscription.getMtaId(),
                subscription.getAppName(), subscription.getResourceName(), subscription.getSpaceId());
        }
    }

    public ConfigurationSubscriptionDto update(long id, ConfigurationSubscriptionDto delta)
        throws ConflictException, NotFoundException {
        try {
            return new TransactionalExecutor<ConfigurationSubscriptionDto>(createEntityManager()).execute((manager) -> {

                ConfigurationSubscriptionDto existingSubscription = findInternal(id, manager);
                if (existingSubscription == null) {
                    throw new NotFoundException(Messages.CONFIGURATION_SUBSCRIPTION_NOT_FOUND, id);
                }
                ConfigurationSubscriptionDto result = merge(existingSubscription, delta);
                manager.merge(result);
                return result;

            });
        } catch (RollbackException e) {
            ConfigurationSubscriptionDto subscription = merge(find(id), delta);
            throw new ConflictException(e, Messages.CONFIGURATION_SUBSCRIPTION_ALREADY_EXISTS, subscription.getMtaId(),
                subscription.getAppName(), subscription.getResourceName(), subscription.getSpaceId());
        }
    }

    public ConfigurationSubscriptionDto remove(long id) throws NotFoundException {
        return new TransactionalExecutor<ConfigurationSubscriptionDto>(createEntityManager()).execute((manager) -> {

            ConfigurationSubscriptionDto subscription = findInternal(id, manager);
            if (subscription == null) {
                throw new NotFoundException(Messages.CONFIGURATION_SUBSCRIPTION_NOT_FOUND, id);
            }
            manager.remove(subscription);
            return subscription;

        });
    }

    private void setQueryParameter(TypedQuery<ConfigurationSubscriptionDto> query, String parameterName, String parameterValue) {
        if (parameterValue != null) {
            query.setParameter(parameterName, parameterValue);
        }
    }

    private TypedQuery<ConfigurationSubscriptionDto> createQuery(String mtaId, String appName, String spaceId, String resourceName,
        EntityManager manager) {
        CriteriaBuilder builder = manager.getCriteriaBuilder();
        CriteriaQuery<ConfigurationSubscriptionDto> query = builder.createQuery(ConfigurationSubscriptionDto.class);
        Root<ConfigurationSubscriptionDto> root = query.from(ConfigurationSubscriptionDto.class);

        List<Predicate> predicates = new ArrayList<>();
        if (spaceId != null) {
            predicates.add(builder.equal(root.get(FieldNames.SPACE_ID), builder.parameter(String.class, FieldNames.SPACE_ID)));
        }
        if (appName != null) {
            predicates.add(builder.equal(root.get(FieldNames.APP_NAME), builder.parameter(String.class, FieldNames.APP_NAME)));
        }
        if (resourceName != null) {
            predicates.add(builder.equal(root.get(FieldNames.RESOURCE_NAME), builder.parameter(String.class, FieldNames.RESOURCE_NAME)));
        }
        if (mtaId != null) {
            predicates.add(builder.equal(root.get(FieldNames.MTA_ID), builder.parameter(String.class, FieldNames.MTA_ID)));
        }

        return manager.createQuery(query.select(root).where(predicates.toArray(new Predicate[0])));
    }

    private ConfigurationSubscriptionDto merge(ConfigurationSubscriptionDto existingSubscription, ConfigurationSubscriptionDto delta) {
        long id = existingSubscription.getId();
        String mtaId = CommonUtil.merge(existingSubscription.getMtaId(), delta.getMtaId(), null);
        String appName = CommonUtil.merge(existingSubscription.getAppName(), delta.getAppName(), null);
        String spaceId = CommonUtil.merge(existingSubscription.getSpaceId(), delta.getSpaceId(), null);
        String filter = CommonUtil.merge(existingSubscription.getFilter(), delta.getFilter(), null);
        String moduleContent = CommonUtil.merge(existingSubscription.getModuleContent(), delta.getModuleContent(), null);
        String resourceProperties = CommonUtil.merge(existingSubscription.getResourceProperties(), delta.getResourceProperties(), null);
        String resourceName = CommonUtil.merge(existingSubscription.getResourceName(), delta.getResourceName(), null);
        return new ConfigurationSubscriptionDto(id, mtaId, spaceId, appName, filter, moduleContent, resourceName, resourceProperties);
    }

    private ConfigurationSubscriptionDto findInternal(long id, EntityManager manager) {
        return manager.find(ConfigurationSubscriptionDto.class, id);
    }

    private EntityManager createEntityManager() {
        return emf.createEntityManager();
    }

}
