package org.cloudfoundry.multiapps.controller.process.util;

import java.text.MessageFormat;
import java.util.List;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.util.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdditionalModuleParametersReporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdditionalModuleParametersReporter.class);

    private final ProcessContext context;

    public AdditionalModuleParametersReporter(ProcessContext context) {
        this.context = context;
    }

    public void reportUsageOfAdditionalParameters(Module module) {
        String mtaId = context.getRequiredVariable(Variables.DEPLOYMENT_DESCRIPTOR)
                              .getId();
        String correlationId = context.getRequiredVariable(Variables.CORRELATION_ID);
        String readinessHealthCheckType = (String) module.getParameters()
                                                         .get(SupportedParameters.READINESS_HEALTH_CHECK_TYPE);
        String readinessHealthCheckHttpEndpoint = (String) module.getParameters()
                                                                 .get(SupportedParameters.READINESS_HEALTH_CHECK_HTTP_ENDPOINT);
        Integer readinessHealthCheckInvocationTimeout = (Integer) module.getParameters()
                                                                        .get(SupportedParameters.READINESS_HEALTH_CHECK_INVOCATION_TIMEOUT);
        Integer readinessHealthCheckInterval = (Integer) module.getParameters()
                                                               .get(SupportedParameters.READINESS_HEALTH_CHECK_INTERVAL);
        List<String> buildpacks = PropertiesUtil.getPluralOrSingular(List.of(module.getParameters()), SupportedParameters.BUILDPACKS,
                                                                     SupportedParameters.BUILDPACK);
        if (readinessHealthCheckType != null) {
            reportUsageOfReadinessHealthCheckParameters(mtaId, correlationId, readinessHealthCheckType, readinessHealthCheckHttpEndpoint,
                                                        readinessHealthCheckInvocationTimeout, readinessHealthCheckInterval,
                                                        buildpacks.toString(), module.getType());
        }
    }

    // this method is being observed by Dynatrace, be careful if you change it
    private void reportUsageOfReadinessHealthCheckParameters(String mtaId, String correlationId, String readinessHealthCheckType,
                                                             String readinessHealthCheckHttpEndpoint,
                                                             Integer readinessHealthCheckInvocationTimeout,
                                                             Integer readinessHealthCheckInterval, String buildpacks, String moduleType) {
        LOGGER.info(MessageFormat.format("MTA with ID \"{0}\" associated with operation ID \"{1}\" uses readiness health check parameters: type=\"{2}\", httpEndpoint=\"{3}\", invocationTimeout=\"{4}\", interval=\"{5}\", buildpacks=\"{6}\", moduleType=\"{7}\"",
                                         mtaId, correlationId, readinessHealthCheckType, readinessHealthCheckHttpEndpoint,
                                         readinessHealthCheckInvocationTimeout, readinessHealthCheckInterval, buildpacks, moduleType));
    }
}
