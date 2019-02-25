package com.sap.cloud.lm.sl.cf.core.cf.util;

import java.util.List;

import com.sap.cloud.lm.sl.mta.model.v2.Module;

public interface ModulesContentValidator {

    void validate(List<Module> modules);
}
