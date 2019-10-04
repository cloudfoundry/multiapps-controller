package com.sap.cloud.lm.sl.cf.web.resources;

import java.text.MessageFormat;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.shutdown.model.ApplicationShutdownDto;
import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;

@RestController
@RequestMapping("/admin/shutdown")
public class ApplicationShutdownResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationShutdownResource.class);

    @Inject
    private FlowableFacade flowableFacade;

    @PostMapping(produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE })
    public ApplicationShutdownDto
           shutdownFlowableJobExecutor(HttpServletRequest request,
                                       @RequestHeader(name = "x-cf-applicationid", required = false) String appId,
                                       @RequestHeader(name = "x-cf-instanceid", required = false) String appInstanceId,
                                       @RequestHeader(name = "x-cf-instanceindex", required = false) String appInstanceIndex) {

        CompletableFuture.runAsync(() -> {
            LOGGER.info(MessageFormat.format(Messages.APP_SHUTDOWN_REQUEST, appId, appInstanceId, appInstanceIndex));
            flowableFacade.shutdownJobExecutor();
        })
                         .thenRun(() -> LOGGER.info(MessageFormat.format(Messages.APP_SHUTDOWNED, appId, appInstanceId, appInstanceIndex)));

        return new ApplicationShutdownDto.Builder().isActive(flowableFacade.isJobExecutorActive())
                                                   .appId(appId)
                                                   .appInstanceId(appInstanceId)
                                                   .appInstanceIndex(appInstanceIndex)
                                                   .build();
    }

    @GetMapping(produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE })
    public ApplicationShutdownDto
           getFlowableJobExecutorShutdownStatus(HttpServletRequest request,
                                                @RequestHeader(name = "x-cf-applicationid", required = false) String appId,
                                                @RequestHeader(name = "x-cf-instanceid", required = false) String appInstanceId,
                                                @RequestHeader(name = "x-cf-instanceindex", required = false) String appInstanceIndex) {

        ApplicationShutdownDto appShutdownDto = new ApplicationShutdownDto.Builder().isActive(flowableFacade.isJobExecutorActive())
                                                                                    .appId(appId)
                                                                                    .appInstanceId(appInstanceId)
                                                                                    .appInstanceIndex(appInstanceIndex)
                                                                                    .build();

        LOGGER.info(MessageFormat.format(Messages.APP_SHUTDOWN_STATUS_MONITOR, appId, appInstanceId, appInstanceIndex,
                                         appShutdownDto.getStatus()));

        return appShutdownDto;
    }

}
