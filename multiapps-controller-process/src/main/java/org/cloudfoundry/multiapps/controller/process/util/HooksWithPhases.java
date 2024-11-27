package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;

import org.cloudfoundry.multiapps.controller.core.model.HookPhase;
import org.cloudfoundry.multiapps.mta.model.Hook;
import org.immutables.value.Value;

@Value.Immutable
public interface HooksWithPhases {

    List<HookPhase> getHookPhases();

    List<Hook> getHooks();
}
