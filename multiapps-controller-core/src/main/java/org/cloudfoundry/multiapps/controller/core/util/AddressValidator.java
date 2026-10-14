package org.cloudfoundry.multiapps.controller.core.util;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.Messages;

@Named
public class AddressValidator {

    private final boolean validationEnabled;
    private final List<CidrRange> denyList;

    @Inject
    public AddressValidator(ApplicationConfiguration applicationConfiguration) {
        this.validationEnabled = applicationConfiguration.isDeployFromUrlAddressValidationEnabled();
        this.denyList = parseCidrRanges(applicationConfiguration.getDeployFromUrlAddressDenyList());
    }

    public void validateTarget(String url, String jobId) {
        if (!validationEnabled) {
            return;
        }
        String host = extractHost(url);
        InetAddress[] addresses = resolveAddresses(host);
        for (InetAddress address : addresses) {
            if (isAddressDenied(address)) {
                throw new SLException(
                    MessageFormat.format(Messages.DEPLOY_FROM_URL_TARGET_ADDRESS_0_DENIED_FOR_JOB_WITH_ID_1, host, jobId));
            }
        }
    }

    private boolean isAddressDenied(InetAddress address) {
        if (isReservedAddress(address)) {
            return true;
        }
        byte[] candidate = address.getAddress();
        for (CidrRange range : denyList) {
            if (range.containsAddress(candidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean isReservedAddress(InetAddress address) {
        return address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isAnyLocalAddress() || address.isSiteLocalAddress();
    }

    private String extractHost(String url) {
        String host = URI.create(url)
                         .getHost();
        if (host == null) {
            throw new SLException(MessageFormat.format(Messages.INVALID_URL, url));
        }
        return host;
    }

    protected InetAddress[] resolveAddresses(String host) {
        try {
            return InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new SLException(e, MessageFormat.format(Messages.DEPLOY_FROM_URL_CANNOT_RESOLVE_HOST, host));
        }
    }

    private static List<CidrRange> parseCidrRanges(String denyList) {
        List<CidrRange> ranges = new ArrayList<>();
        if (denyList == null || denyList.isBlank()) {
            return ranges;
        }
        for (String entry : denyList.split(",")) {
            String trimmed = entry.trim();
            if (!trimmed.isEmpty()) {
                ranges.add(CidrRange.fromCidrNotation(trimmed));
            }
        }
        return ranges;
    }
}
