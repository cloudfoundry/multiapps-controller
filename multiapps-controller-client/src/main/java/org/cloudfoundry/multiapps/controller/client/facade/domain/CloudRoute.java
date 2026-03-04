package org.cloudfoundry.multiapps.controller.client.facade.domain;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.common.AllowNulls;
import org.cloudfoundry.multiapps.controller.client.facade.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudRoute.class)
@JsonDeserialize(as = ImmutableCloudRoute.class)
public abstract class CloudRoute extends CloudEntity implements Derivable<CloudRoute> {

    @Value.Default
    public int getAppsUsingRoute() {
        return 0;
    }

    public abstract CloudDomain getDomain();

    @Nullable
    public abstract String getHost();

    @Nullable
    public abstract String getPath();

    @Nullable
    public abstract Integer getPort();

    @Nullable
    public abstract String getRequestedProtocol();

    @AllowNulls
    public abstract Map<String, Object> getOptions();

    @Nullable
    public abstract List<RouteDestination> getDestinations();

    public abstract String getUrl();

    @Override
    public CloudRoute derive() {
        return this;
    }

    @Override
    public String toString() {
        return getUrl();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDomain().getName(), getHost(), getPath(), getPort(), getOptions());
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof CloudRoute)) {
            return false;
        }
        var otherRoute = (CloudRoute) object;
        var thisDomain = getDomain().getName();
        var otherDomain = otherRoute.getDomain()
                                    .getName();
        // @formatter:off
        return thisDomain.equals(otherDomain)
                && areEmptyOrEqual(getHost(), otherRoute.getHost())
                && areEmptyOrEqual(getPath(), otherRoute.getPath())
                && Objects.equals(getPort(), otherRoute.getPort())
                && Objects.equals(getOptions(), otherRoute.getOptions());
        // @formatter:on
    }

    private static boolean areEmptyOrEqual(String lhs, String rhs) {
        if (lhs == null || lhs.isEmpty()) {
            return rhs == null || rhs.isEmpty();
        }
        return lhs.equals(rhs);
    }

}
