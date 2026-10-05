package org.cloudfoundry.multiapps.controller.core.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.Messages;

public class CidrRange {

    private final byte[] networkAddressBytes;
    private final int prefixLengthBits;

    private CidrRange(byte[] networkAddressBytes, int prefixLengthBits) {
        this.networkAddressBytes = networkAddressBytes;
        this.prefixLengthBits = prefixLengthBits;
    }

    public static CidrRange fromCidrNotation(String cidr) {
        String[] addressAndMaskParts = cidr.split("/", 2);
        byte[] networkAddressBytes = resolveNetworkAddressBytes(addressAndMaskParts[0], cidr);
        boolean hasMask = addressAndMaskParts.length == 2;
        int maskBits;
        if (hasMask) {
            maskBits = Integer.parseInt(addressAndMaskParts[1].trim());
        } else {
            maskBits = networkAddressBytes.length * 8;
        }
        return new CidrRange(networkAddressBytes, maskBits);
    }

    private static byte[] resolveNetworkAddressBytes(String networkAddress, String cidr) {
        try {
            return InetAddress.getByName(networkAddress)
                              .getAddress();
        } catch (UnknownHostException e) {
            throw new SLException(e, MessageFormat.format(Messages.DEPLOY_FROM_URL_INVALID_DENY_LIST_ENTRY, cidr));
        }
    }

    public boolean containsAddress(byte[] candidateAddressBytes) {
        if (candidateAddressBytes.length != networkAddressBytes.length) {
            return false;
        }
        return matchesFullOctets(candidateAddressBytes) && matchesPartialOctet(candidateAddressBytes);
    }

    private boolean matchesFullOctets(byte[] candidateAddressBytes) {
        int fullPrefixBytes = prefixLengthBits / 8;
        for (int i = 0; i < fullPrefixBytes; i++) {
            if (candidateAddressBytes[i] != networkAddressBytes[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesPartialOctet(byte[] candidateAddressBytes) {
        int remainingBitsInPartialOctet = prefixLengthBits % 8;
        if (remainingBitsInPartialOctet == 0) {
            return true;
        }
        int partialOctetBitMask = 0xFF << (8 - remainingBitsInPartialOctet) & 0xFF;
        int fullPrefixBytes = prefixLengthBits / 8;
        return (candidateAddressBytes[fullPrefixBytes] & partialOctetBitMask) == (networkAddressBytes[fullPrefixBytes] & partialOctetBitMask);
    }
}
