package org.cloudfoundry.multiapps.controller.web.configuration.bean;

import java.util.UUID;

import org.quartz.spi.InstanceIdGenerator;

public class RandomInstanceIdGenerator implements InstanceIdGenerator {

    @Override
    public String generateInstanceId() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

}
