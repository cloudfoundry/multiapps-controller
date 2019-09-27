package com.sap.cloud.lm.sl.cf.web.resources;

import java.text.MessageFormat;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.process.internal.RequestScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.shutdown.model.ApplicationShutdownDto;
import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;

@RequestScoped
@Path("/admin/shutdown")
@Produces(MediaType.APPLICATION_JSON)
public class AdminResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminResource.class);

    @Inject
    private FlowableFacade flowableFacade;

    @POST
    public ApplicationShutdownDto shutdownFlowableJobExecutor(@HeaderParam("x-cf-applicationid") String appId,
                                                              @HeaderParam("x-cf-instanceid") String appInstanceId,
                                                              @HeaderParam("x-cf-instanceindex") String appInstanceIndex) {

        CompletableFuture.runAsync(() -> {
            LOGGER.info(MessageFormat.format(Messages.APP_SHUTDOWN_REQUEST, appId, appInstanceId, appInstanceIndex));
            flowableFacade.shutdownJobExecutor();
        })
                         .thenRun(() -> {
                             LOGGER.info(MessageFormat.format(Messages.APP_SHUTDOWNED, appId, appInstanceId, appInstanceIndex));
                         });

        return new ApplicationShutdownDto.Builder().isActive(flowableFacade.isJobExecutorActive())
                                                   .appId(appId)
                                                   .appInstanceId(appInstanceId)
                                                   .appInstanceIndex(appInstanceIndex)
                                                   .build();
    }

    @GET
    public ApplicationShutdownDto getFlowableJobExecutorShutdownStatus(@HeaderParam("x-cf-applicationid") String appId,
                                                                       @HeaderParam("x-cf-instanceid") String appInstanceId,
                                                                       @HeaderParam("x-cf-instanceindex") String appInstanceIndex) {

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
