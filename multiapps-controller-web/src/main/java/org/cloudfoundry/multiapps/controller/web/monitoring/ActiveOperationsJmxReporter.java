package org.cloudfoundry.multiapps.controller.web.monitoring;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

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
        } catch (Exception e) {
            LOGGER.warn("Failed to refresh active operations JMX metrics", e);
        }
    }

    private Map<String, Long> groupBy(List<Operation> operations, Function<Operation, String> keyExtractor) {
        return operations.stream()
                         .collect(Collectors.groupingBy(keyExtractor, Collectors.counting()));
    }

    private void syncMBeans(Map<String, Long> counts, String pattern,
                            Map<ObjectName, ActiveOperationsCount> registry) throws Exception {
        Set<ObjectName> stale = new HashSet<>(registry.keySet());
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            ObjectName name = new ObjectName(pattern.formatted(ObjectName.quote(entry.getKey())));
            stale.remove(name);
            upsertMBean(name, entry.getValue(), registry);
        }
        removeStaleMBeans(stale, registry);
    }

    private void upsertMBean(ObjectName name, long count,
                             Map<ObjectName, ActiveOperationsCount> registry) throws Exception {
        ActiveOperationsCount bean = registry.get(name);
        if (bean == null) {
            bean = new ActiveOperationsCount(count);
            mBeanServer.registerMBean(bean, name);
            registry.put(name, bean);
        } else {
            bean.setCount(count);
        }
    }

    private void removeStaleMBeans(Set<ObjectName> stale,
                                   Map<ObjectName, ActiveOperationsCount> registry) throws Exception {
        for (ObjectName name : stale) {
            mBeanServer.unregisterMBean(name);
            registry.remove(name);
        }
    }

    static String hashUser(String user) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-384")
                                         .digest(user.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of()
                            .formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

}
