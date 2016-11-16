package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudInfoExtended;
import com.sap.cloud.lm.sl.cf.core.helpers.ClientHelper;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("undeployAppsStep")
public class UndeployAppsStep extends AbstractXS2ProcessStep {

    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(UndeployAppsStep.class);

    public static StepMetadata getMetadata() {
        return new StepMetadata("undeployAppsTask", "Undeploy Discontinued Apps", "Undeploy Discontinued Apps");
    }

    protected Function<DelegateExecution, CloudFoundryOperations> clientSupplier = (context) -> getCloudFoundryClient(context, LOGGER);

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {

        logActivitiTask(context, LOGGER);
        try {
            info(context, Messages.UNDEPLOYING_APPS, LOGGER);

            // Get the list of cloud applications from the context
            final List<CloudApplication> apps = StepsUtil.getAppsToUndeploy(context);

            CloudFoundryOperations client = clientSupplier.apply(context);

            // Stop and delete the applications
            stopApplications(apps, client, context);
            deleteApplications(apps, client, context);

            debug(context, Messages.APPS_UNDEPLOYED, LOGGER);
            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            error(context, Messages.ERROR_UNDEPLOYING_APPS, e, LOGGER);
            throw e;
        } catch (CloudFoundryException e) {
            SLException ex = StepsUtil.createException(e);
            error(context, Messages.ERROR_UNDEPLOYING_APPS, ex, LOGGER);
            throw ex;
        }
    }

    private void stopApplications(List<CloudApplication> apps, CloudFoundryOperations client, DelegateExecution context) {
        apps.stream().forEach(app -> stopApplication(app, client, context));
    }

    private void deleteApplications(List<CloudApplication> apps, CloudFoundryOperations client, DelegateExecution context) {
        apps.stream().forEach(app -> {
            deleteApplicationRoutes(app, client, context);
            deleteApplication(app, client, context);
        });
    }

    private void stopApplication(CloudApplication app, CloudFoundryOperations client, DelegateExecution context) {
        info(context, format(Messages.STOPPING_APP, app.getName()), LOGGER);
        client.stopApplication(app.getName());
        debug(context, format(Messages.APP_STOPPED, app.getName()), LOGGER);
    }

    private void deleteApplication(CloudApplication app, CloudFoundryOperations client, DelegateExecution context) {
        info(context, format(Messages.DELETING_APP, app.getName()), LOGGER);
        client.deleteApplication(app.getName());
        debug(context, format(Messages.APP_DELETED, app.getName()), LOGGER);
    }

    private void deleteApplicationRoutes(CloudApplication app, CloudFoundryOperations client, DelegateExecution context) {
        info(context, format(Messages.DELETING_APP_ROUTES, app.getName()), LOGGER);
        app.getUris().forEach(uri -> {
            info(context, format(Messages.DELETING_ROUTE, uri), LOGGER);
            client.updateApplicationUris(app.getName(), Collections.emptyList());
            CloudInfo info = client.getCloudInfo();
            boolean portBasedRouting = (info instanceof CloudInfoExtended) ? ((CloudInfoExtended) info).isPortBasedRouting() : false;
            try {
                new ClientHelper(client).deleteRoute(uri, portBasedRouting);
                debug(context, format(Messages.ROUTE_DELETED, uri), LOGGER);
            } catch (CloudFoundryException e) {
                if (!e.getStatusCode().equals(HttpStatus.CONFLICT))
                    throw e;
                info(context, format(Messages.ROUTE_NOT_DELETED, uri), LOGGER);
            }
        });
    }

}
