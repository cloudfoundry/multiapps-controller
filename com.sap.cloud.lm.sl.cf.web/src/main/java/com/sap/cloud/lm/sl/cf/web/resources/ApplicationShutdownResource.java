package com.sap.cloud.lm.sl.cf.web.resources;

import java.text.MessageFormat;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.shutdown.model.ApplicationShutdownDto;

@Component
@Produces(MediaType.APPLICATION_JSON)
public class ApplicationShutdownResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationShutdownResource.class);

    private static final long DEFAULT_COOLDOWN_TIMEOUT_IN_SECONDS = 300L; // 5 mins

    public static final String ACTION = "shutdown";

    @Inject
    private FlowableFacade flowableFacade;

    @POST
    public ApplicationShutdownDto shutdownFlowableJobExecutor(@HeaderParam("x-cf-applicationid") String appId,
        @HeaderParam("x-cf-instanceid") String appInstanceId, @HeaderParam("x-cf-instanceindex") String appInstanceIndex,
        @QueryParam("cooldownTimeoutInSeconds") String cooldownTimeoutInSecondsParam) {

        long cooldownTimeoutInSeconds = getCooldownTimeoutInSeconds(cooldownTimeoutInSecondsParam);

        CompletableFuture.runAsync(() -> {
            LOGGER.info(MessageFormat.format(Messages.APP_SHUTDOWN_REQUEST, appId, appInstanceId, appInstanceIndex,
                cooldownTimeoutInSeconds));
            flowableFacade.shutdownJobExecutor(cooldownTimeoutInSeconds);
        })
            .thenRun(() -> {
                LOGGER.info(MessageFormat.format(Messages.APP_SHUTDOWNED, appId, appInstanceId, appInstanceIndex,
                    cooldownTimeoutInSeconds));
            });

        return new ApplicationShutdownDto.Builder().isActive(flowableFacade.isJobExecutorActive())
            .appId(appId)
            .appInstanceId(appInstanceId)
            .appInstanceIndex(appInstanceIndex)
            .cooldownTimeoutInSeconds(cooldownTimeoutInSeconds)
            .build();
    }

    @GET
    public ApplicationShutdownDto getFlowableJobExecutorShutdownStatus(@HeaderParam("x-cf-applicationid") String appId,
        @HeaderParam("x-cf-instanceid") String appInstanceId, @HeaderParam("x-cf-instanceindex") String appInstanceIndex) {

        ApplicationShutdownDto appShutdownDto = new ApplicationShutdownDto.Builder().isActive(flowableFacade.isJobExecutorActive())
            .appId(appId)
            .appInstanceId(appInstanceId)
            .appInstanceIndex(appInstanceIndex)
            .build();

        LOGGER.info(
            MessageFormat.format(Messages.APP_SHUTDOWN_STATUS_MONITOR, appId, appInstanceId, appInstanceIndex, appShutdownDto.getStatus()));

        return appShutdownDto;
    }

    private long getCooldownTimeoutInSeconds(String cooldownTimeoutInSecondsParam) {
        if (cooldownTimeoutInSecondsParam == null) {
            return DEFAULT_COOLDOWN_TIMEOUT_IN_SECONDS;
        }
        return Long.parseLong(cooldownTimeoutInSecondsParam);
    }

}
