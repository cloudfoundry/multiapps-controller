package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.process.security.SecretTokenSerializer;
import org.cloudfoundry.multiapps.controller.process.security.store.SecretTokenStore;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.Serializer;
import org.cloudfoundry.multiapps.controller.process.variables.Variable;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.controller.process.variables.WrappedVariable;
import org.flowable.engine.delegate.DelegateExecution;

public class SecureProcessContext extends ProcessContext {

    private SecretTokenStore secretTokenStore;
    //    private SecretTransformationStrategy secretTransformationStrategy;

    public SecureProcessContext(DelegateExecution execution, StepLogger stepLogger, CloudControllerClientProvider clientProvider,
                                SecretTokenStore secretTokenStore) {
        super(execution, stepLogger, clientProvider);
        this.secretTokenStore = secretTokenStore;
        //        this.secretTransformationStrategy = secretTransformationStrategy;
    }

    private String getPidOfCurrentExecution() {
        return getExecution().getRootProcessInstanceId();
    }

    private <T> Variable<T> wrap(Variable<T> variable) {
        if (variable.getName()
                    .equals(Variables.SECURE_EXTENSION_DESCRIPTOR_PARAMETER_NAMES.getName())) {
            return variable;
        }

        DelegateExecution currentExecution = getExecution();
        String processInstanceId = getPidOfCurrentExecution();

        byte[] secureParameterNamesRaw = (byte[]) currentExecution.getVariable(
            Variables.SECURE_EXTENSION_DESCRIPTOR_PARAMETER_NAMES.getName());

        Set<String> secureParameterNames = extractSecureParameterNames(secureParameterNamesRaw);

        //        SecretTransformationStrategy secretTransformationStrategyContext = new SecretTransformationStrategyContextImpl(
        //            secureParameterNames);
        //        SecureSerializerConfiguration secureSerializerConfiguration = new SecureSerializerConfiguration();
        //        secureSerializerConfiguration.setAdditionalSensitiveElementNames(secureParameterNames);
        List<String> secureParametersNamesList = new ArrayList<>(secureParameterNames);

        Serializer<T> wrappedSerializer = new SecretTokenSerializer<>(variable.getSerializer(), secretTokenStore,
                                                                      secureParametersNamesList,
                                                                      processInstanceId, variable.getName());

        return new WrappedVariable<>(variable, wrappedSerializer);
    }

    private Set<String> extractSecureParameterNames(byte[] secureParameterNamesRaw) {
        if (secureParameterNamesRaw == null) {
            return Set.of();
        } else {
            return Variables.SECURE_EXTENSION_DESCRIPTOR_PARAMETER_NAMES.getSerializer()
                                                                        .deserialize(secureParameterNamesRaw);
        }
    }

    @Override
    public <T> void setVariable(Variable<T> variable, T value) {
        VariableHandling.set(getExecution(), wrap(variable), value);
    }

    @Override
    public <T> T getVariable(Variable<T> variable) {
        return VariableHandling.get(getExecution(), wrap(variable));
    }

    @Override
    public <T> T getVariableIfSet(Variable<T> variable) {
        return VariableHandling.getIfSet(getExecution(), wrap(variable));
    }

    @Override
    public <T> T getVariableBackwardsCompatible(Variable<T> variable) {
        return VariableHandling.getBackwardsCompatible(getExecution(), wrap(variable));
    }

}
