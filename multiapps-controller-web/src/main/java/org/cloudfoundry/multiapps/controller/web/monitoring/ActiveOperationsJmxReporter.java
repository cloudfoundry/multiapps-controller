package org.cloudfoundry.multiapps.controller.web.monitoring;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.OperationsException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.web.util.RateLimitHashing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import static org.cloudfoundry.multiapps.controller.web.Messages.FAILED_TO_REFRESH_ACTIVE_OPERATIONS_JMX_METRICS;

@Named
public class ActiveOperationsJmxReporter {

    static final String DOMAIN = "org.cloudfoundry.multiapps.controller.web.monitoring";
    static final String USER_OBJECT_NAME_PATTERN = DOMAIN + ":type=Metrics,name=ActiveOps,user=%s";
    static final String SPACE_OBJECT_NAME_PATTERN = DOMAIN + ":type=Metrics,name=ActiveOps,space=%s";

    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveOperationsJmxReporter.class);

    private final OperationService operationService;
    private final MBeanServer mBeanServer;
    private final ApplicationConfiguration applicationConfiguration;
    private static final int SELECTED_INSTANCE_FOR_CLEAN_UP = 0;
    private final Map<ObjectName, ActiveOperationsCount> userBeans = new ConcurrentHashMap<>();
    private final Map<ObjectName, ActiveOperationsCount> spaceBeans = new ConcurrentHashMap<>();

    @Inject
    public ActiveOperationsJmxReporter(OperationService operationService, MBeanServer mBeanServer,
                                       ApplicationConfiguration applicationConfiguration) {
        this.operationService = operationService;
        this.mBeanServer = mBeanServer;
        this.applicationConfiguration = applicationConfiguration;
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    public void refresh() {
        if (!applicationConfiguration.isOperationRateLimitingEnabled()) {
            return;
        }
        if (applicationConfiguration.getApplicationInstanceIndex() != SELECTED_INSTANCE_FOR_CLEAN_UP) {
            return;
        }
        try {
            List<Operation> activeOperations = operationService.createQuery()
                                                               .inNonFinalState()
                                                               .list();
            syncMBeans(groupBy(activeOperations, op -> hashUser(op.getUser())), USER_OBJECT_NAME_PATTERN, userBeans);
            syncMBeans(groupBy(activeOperations, Operation::getSpaceId), SPACE_OBJECT_NAME_PATTERN, spaceBeans);
        } catch (MBeanRegistrationException | OperationsException e) {
            LOGGER.warn(FAILED_TO_REFRESH_ACTIVE_OPERATIONS_JMX_METRICS, e);
        }
    }

    private Map<String, Long> groupBy(List<Operation> operations, Function<Operation, String> keyExtractor) {
        return operations.stream()
                         .collect(Collectors.groupingBy(keyExtractor, Collectors.counting()));
    }

    private void syncMBeans(Map<String, Long> counts, String pattern, Map<ObjectName, ActiveOperationsCount> registry)
        throws MBeanRegistrationException, OperationsException {
        Set<ObjectName> stale = new HashSet<>(registry.keySet());
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            ObjectName name = new ObjectName(pattern.formatted(ObjectName.quote(entry.getKey())));
            stale.remove(name);
            upsertMBean(name, entry.getValue(), registry);
        }
        removeStaleMBeans(stale, registry);
    }

    private void upsertMBean(ObjectName name, long count, Map<ObjectName, ActiveOperationsCount> registry)
        throws NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
        ActiveOperationsCount bean = registry.get(name);
        if (bean == null) {
            bean = new ActiveOperationsCount(count);
            mBeanServer.registerMBean(bean, name);
            registry.put(name, bean);
        } else {
            bean.setCount(count);
        }
    }

    private void removeStaleMBeans(Set<ObjectName> stale, Map<ObjectName, ActiveOperationsCount> registry)
        throws InstanceNotFoundException, MBeanRegistrationException {
        for (ObjectName name : stale) {
            mBeanServer.unregisterMBean(name);
            registry.remove(name);
        }
    }

    static String hashUser(String user) {
        return RateLimitHashing.hashToHex(user);
    }

}
