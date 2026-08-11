package com.monitor.security;

import com.monitor.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * These tests deliberately avoid real DNS lookups for anything other than "localhost"
 * and literal IP addresses (neither of which involve an actual network query - a literal
 * IP is parsed locally by {@link InetAddress#getAllByName}, and "localhost" resolves via
 * the OS's local/loopback resolution, not an external DNS server) so the suite is
 * deterministic and runs the same with or without network access. The "valid public URL"
 * case therefore uses a well-known public IP address rather than a hostname.
 */
class UrlSecurityValidatorTest {

    private final UrlSecurityValidator validator = new UrlSecurityValidator();

    @Test
    void rejectsLocalhostByName() {
        assertThatThrownBy(() -> validator.assertSafeToRegister("http://localhost:8080/health"))
                .isInstanceOf(ValidationException.class);
        assertThat(validator.isCurrentlySafe("http://localhost:8080/health")).isFalse();
    }

    @Test
    void rejectsLoopbackIpv4Literal() {
        assertThatThrownBy(() -> validator.assertSafeToRegister("http://127.0.0.1/health"))
                .isInstanceOf(ValidationException.class);
        assertThat(validator.isCurrentlySafe("http://127.0.0.1/health")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://10.0.0.5/",     // 10.0.0.0/8
            "http://172.16.4.4/",   // 172.16.0.0/12
            "http://192.168.1.1/",  // 192.168.0.0/16
    })
    void rejectsPrivateIpv4Literals(String url) {
        assertThatThrownBy(() -> validator.assertSafeToRegister(url))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsLinkLocalIpv4() {
        // 169.254.169.254 is also the AWS/GCP/Azure instance metadata address - the
        // canonical real-world SSRF target this check exists to stop.
        assertThatThrownBy(() -> validator.assertSafeToRegister("http://169.254.169.254/latest/meta-data"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsIpv6Loopback() {
        assertThatThrownBy(() -> validator.assertSafeToRegister("http://[::1]/health"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsIpv6UniqueLocal() {
        // fc00::/7 - not classified as "site-local" by the JDK, which is why
        // UrlSecurityValidator has its own manual check for it.
        assertThatThrownBy(() -> validator.assertSafeToRegister("http://[fd12:3456:789a::1]/health"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void acceptsValidPublicHttpsUrl() {
        // 8.8.8.8 (Google public DNS) - a stable, well-known public IP used as a literal
        // so this test doesn't depend on live DNS resolution of a hostname.
        assertThat(validator.isCurrentlySafe("https://8.8.8.8/")).isTrue();
        // Should not throw.
        validator.assertSafeToRegister("https://8.8.8.8/");
    }

    @Test
    void rejectsNonHttpScheme() {
        assertThatThrownBy(() -> validator.assertSafeToRegister("ftp://8.8.8.8/"))
                .isInstanceOf(ValidationException.class);
    }

    // --- Pure classification logic, exercised directly without any DNS/name resolution ---

    @Test
    void isBlockedAddress_flagsLoopbackAndPrivateAndLinkLocal() throws UnknownHostException {
        assertThat(UrlSecurityValidator.isBlockedAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}))).isTrue();
        assertThat(UrlSecurityValidator.isBlockedAddress(InetAddress.getByAddress(new byte[]{10, 1, 2, 3}))).isTrue();
        assertThat(UrlSecurityValidator.isBlockedAddress(InetAddress.getByAddress(new byte[]{(byte) 192, (byte) 168, 0, 1}))).isTrue();
        assertThat(UrlSecurityValidator.isBlockedAddress(InetAddress.getByAddress(new byte[]{(byte) 169, (byte) 254, 1, 1}))).isTrue();
    }

    @Test
    void isBlockedAddress_flagsCarrierGradeNat() throws UnknownHostException {
        // 100.64.0.0/10
        assertThat(UrlSecurityValidator.isBlockedAddress(InetAddress.getByAddress(new byte[]{100, 64, 0, 1}))).isTrue();
        assertThat(UrlSecurityValidator.isBlockedAddress(InetAddress.getByAddress(new byte[]{100, 127, (byte) 255, (byte) 255}))).isTrue();
        // just outside the range should NOT be blocked by this rule
        assertThat(UrlSecurityValidator.isBlockedAddress(InetAddress.getByAddress(new byte[]{100, 63, 0, 1}))).isFalse();
    }

    @Test
    void isBlockedAddress_flagsIpv4MappedIpv6WrappingPrivateAddress() throws UnknownHostException {
        // ::ffff:10.0.0.1 - an IPv4-mapped IPv6 address wrapping a private IPv4 address.
        byte[] mapped = new byte[16];
        mapped[10] = (byte) 0xFF;
        mapped[11] = (byte) 0xFF;
        mapped[12] = 10;
        mapped[13] = 0;
        mapped[14] = 0;
        mapped[15] = 1;
        assertThat(UrlSecurityValidator.isBlockedAddress(InetAddress.getByAddress(mapped))).isTrue();
    }

    @Test
    void isBlockedAddress_allowsOrdinaryPublicAddress() throws UnknownHostException {
        assertThat(UrlSecurityValidator.isBlockedAddress(InetAddress.getByAddress(new byte[]{8, 8, 8, 8}))).isFalse();
    }
}
