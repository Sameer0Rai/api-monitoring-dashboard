package com.monitor.security;

import com.monitor.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Guards against Server-Side Request Forgery (SSRF): without this, "monitor this URL"
 * is an open invitation for a user to make the server issue requests to
 * {@code http://169.254.169.254/latest/meta-data} (cloud instance metadata),
 * {@code http://localhost:5432} (the database container itself, from inside the Docker
 * network), or any other address the server can reach but the user couldn't otherwise.
 *
 * <p>Two call sites, two different failure behaviors:</p>
 * <ul>
 *   <li>{@link #assertSafeToRegister(String)} - called when a service is registered.
 *       Resolves the hostname and rejects the request outright (400) if any resolved
 *       address is internal. Fails closed on a DNS lookup failure too - an unresolvable
 *       host can't be monitored anyway.</li>
 *   <li>{@link #isCurrentlySafe(String)} - called by {@code HealthCheckService}
 *       immediately before every single check, not just once at registration. This is
 *       the DNS-rebinding mitigation: a hostname that resolved to a public IP when it was
 *       registered could later be repointed (via a very short DNS TTL) to an internal
 *       address, and without re-checking at request time the server would happily follow
 *       it. Re-resolving and re-validating right before each request narrows that window
 *       to "between this check and the actual TCP connect" rather than "forever after
 *       registration" - it does not eliminate the gap entirely (that would need pinning
 *       the validated IP and connecting to it directly at the Netty resolver level,
 *       which is a larger change than this fix warrants), but it closes the overwhelming
 *       majority of realistic exposure.</li>
 * </ul>
 */
@Component
public class UrlSecurityValidator {

    private static final Logger log = LoggerFactory.getLogger(UrlSecurityValidator.class);

    public void assertSafeToRegister(String url) {
        ValidationOutcome outcome = evaluate(url);
        if (!outcome.safe()) {
            throw new ValidationException("url", outcome.reason());
        }
    }

    /**
     * Non-throwing variant for use inside the scheduled health-check loop, where a blocked
     * URL should be recorded as a failed check (see HealthCheckService), not an exception
     * that would otherwise interrupt the whole sweep.
     */
    public boolean isCurrentlySafe(String url) {
        ValidationOutcome outcome = evaluate(url);
        if (!outcome.safe()) {
            log.warn("Blocked health check for potentially unsafe target: {}", outcome.reason());
        }
        return outcome.safe();
    }

    private ValidationOutcome evaluate(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            return ValidationOutcome.unsafe("url is not a valid URI");
        }

        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            return ValidationOutcome.unsafe("url must use http or https");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return ValidationOutcome.unsafe("url must include a host");
        }

        InetAddress[] addresses;
        try {
            // Resolves every A/AAAA record for the host - a hostname can round-robin
            // across multiple addresses, and all of them need to be safe, not just the
            // first one returned.
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            return ValidationOutcome.unsafe("url host could not be resolved");
        }

        for (InetAddress address : addresses) {
            if (isBlockedAddress(address)) {
                return ValidationOutcome.unsafe(
                        "url resolves to a private, loopback, link-local, or otherwise internal address and cannot be monitored");
            }
        }

        return ValidationOutcome.ok();
    }

    /**
     * True if the address is not something a remote monitoring target should ever be:
     * loopback, link-local, private ("site-local"), multicast, wildcard/any-local, or one
     * of the other reserved ranges the JDK's built-in classifiers don't cover
     * (carrier-grade NAT, documentation/benchmarking ranges, IPv6 unique-local, and
     * IPv4-mapped IPv6 addresses wrapping any of the above).
     */
    static boolean isBlockedAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }

        byte[] bytes = address.getAddress();

        if (bytes.length == 4) {
            return isBlockedIpv4(bytes);
        }

        if (bytes.length == 16) {
            if (isIpv4Mapped(bytes)) {
                return isBlockedIpv4(Arrays.copyOfRange(bytes, 12, 16));
            }
            return isBlockedIpv6Extra(bytes);
        }

        // Unrecognized address format - fail closed rather than allow it through.
        return true;
    }

    private static int unsigned(byte b) {
        return b & 0xFF;
    }

    private static boolean isBlockedIpv4(byte[] b) {
        int a = unsigned(b[0]);
        int c = unsigned(b[1]);
        int d = unsigned(b[2]);

        if (a == 0) return true;                          // 0.0.0.0/8 - "this network"
        if (a == 100 && c >= 64 && c <= 127) return true;  // 100.64.0.0/10 - carrier-grade NAT
        if (a == 192 && c == 0 && d == 0) return true;     // 192.0.0.0/24 - IETF protocol assignments
        if (a == 192 && c == 0 && d == 2) return true;     // 192.0.2.0/24 - TEST-NET-1
        if (a == 198 && (c == 18 || c == 19)) return true; // 198.18.0.0/15 - benchmarking
        if (a == 198 && c == 51 && d == 100) return true;  // 198.51.100.0/24 - TEST-NET-2
        if (a == 203 && c == 0 && d == 113) return true;   // 203.0.113.0/24 - TEST-NET-3
        return a >= 240;                                    // 240.0.0.0/4 reserved + 255.255.255.255
    }

    private static boolean isBlockedIpv6Extra(byte[] b) {
        // fc00::/7 - unique local addresses. Not classified as "site-local" by the JDK
        // (that predates ULA and is effectively IPv4-oriented in practice).
        return (b[0] & 0xFE) == 0xFC;
    }

    private static boolean isIpv4Mapped(byte[] b) {
        for (int i = 0; i < 10; i++) {
            if (b[i] != 0) return false;
        }
        return unsigned(b[10]) == 0xFF && unsigned(b[11]) == 0xFF;
    }

    private record ValidationOutcome(boolean safe, String reason) {
        static ValidationOutcome ok() {
            return new ValidationOutcome(true, null);
        }

        static ValidationOutcome unsafe(String reason) {
            return new ValidationOutcome(false, reason);
        }
    }
}
