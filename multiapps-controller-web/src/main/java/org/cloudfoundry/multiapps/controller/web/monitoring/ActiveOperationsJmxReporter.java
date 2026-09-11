package org.cloudfoundry.multiapps.controller.web.monitoring;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import javax.management.MBeanServer;
import javax.management.ObjectName;

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
    private final Map<ObjectName, ActiveOperationsCount> userBeans = new ConcurrentHashMap<>();
    private final Map<ObjectName, ActiveOperationsCount> spaceBeans = new ConcurrentHashMap<>();

    @Inject
    public ActiveOperationsJmxReporter(OperationService operationService, MBeanServer mBeanServer) {
        this.operationService = operationService;
        this.mBeanServer = mBeanServer;
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    public void refresh() {
        try {
            Map<String, Long> byUser = new HashMap<>();
            Map<String, Long> bySpace = new HashMap<>();
            operationService.createQuery()
                            .inNonFinalState()
                            .list()
                            .forEach(op -> {
                                byUser.merge(hashUser(op.getUser()), 1L, Long::sum);
                                bySpace.merge(op.getSpaceId(), 1L, Long::sum);
                            });
            syncMBeans(byUser, USER_OBJECT_NAME_PATTERN, userBeans);
            syncMBeans(bySpace, SPACE_OBJECT_NAME_PATTERN, spaceBeans);
        } catch (Exception e) {
            LOGGER.warn("Failed to refresh active operations JMX metrics", e);
        }
    }

    private void syncMBeans(Map<String, Long> counts, String pattern,
                             Map<ObjectName, ActiveOperationsCount> registry) throws Exception {
        Set<ObjectName> toRemove = new HashSet<>(registry.keySet());

        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            ObjectName name = new ObjectName(pattern.formatted(ObjectName.quote(entry.getKey())));
            toRemove.remove(name);
            ActiveOperationsCount bean = registry.get(name);
            if (bean == null) {
                bean = new ActiveOperationsCount(entry.getValue());
                mBeanServer.registerMBean(bean, name);
                registry.put(name, bean);
            } else {
                bean.setCount(entry.getValue());
            }
        }

        for (ObjectName name : toRemove) {
            mBeanServer.unregisterMBean(name);
            registry.remove(name);
        }
    }

    static String hashUser(String user) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                                         .digest(user.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

}
