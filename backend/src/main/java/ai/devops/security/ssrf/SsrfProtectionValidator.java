package ai.devops.security.ssrf;

import ai.devops.common.exception.SsrfProtectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.util.Set;

@Component
public class SsrfProtectionValidator {

    private static final Logger log = LoggerFactory.getLogger(SsrfProtectionValidator.class);

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https", "tcp", "unix", "npipe");
    private static final Set<String> BLOCKED_METADATA_HOSTS = Set.of(
            "169.254.169.254",
            "metadata.google.internal",
            "100.100.100.200",
            "169.254.170.2",
            "fd00:ec2::254"
    );

    private final boolean allowPrivateNetwork;

    public SsrfProtectionValidator(
            @Value("${app.integrations.allow-private-network:true}") boolean allowPrivateNetwork) {
        this.allowPrivateNetwork = allowPrivateNetwork;
    }

    public void validateEndpointUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new SsrfProtectionException(rawUrl, "Endpoint URL must not be empty.");
        }

        String trimmed = rawUrl.trim();

        // Allow Unix socket and Windows named pipes
        if (trimmed.startsWith("unix://") || trimmed.startsWith("npipe://")) {
            return;
        }

        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme();

            if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
                throw new SsrfProtectionException(rawUrl, "Unsupported URL scheme: " + scheme);
            }

            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                // If tcp://host:port
                if ("tcp".equalsIgnoreCase(scheme) && uri.getAuthority() != null) {
                    host = uri.getAuthority().split(":")[0];
                } else {
                    throw new SsrfProtectionException(rawUrl, "Host could not be extracted.");
                }
            }

            String lowerHost = host.toLowerCase();

            // Strict Cloud Metadata Protection (Always blocked!)
            if (BLOCKED_METADATA_HOSTS.contains(lowerHost)) {
                log.warn("CRITICAL: SSRF Attempt to Cloud Metadata Endpoint blocked: {}", rawUrl);
                throw new SsrfProtectionException(rawUrl, "Access to cloud instance metadata services is strictly forbidden.");
            }

            // In strict SaaS mode, validate resolved IP
            if (!allowPrivateNetwork) {
                InetAddress address = InetAddress.getByName(host);
                if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()) {
                    throw new SsrfProtectionException(rawUrl, "Access to local and private IP addresses is prohibited in strict production mode.");
                }
            }
        } catch (SsrfProtectionException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SsrfProtectionException(rawUrl, "Malformed URL: " + ex.getMessage());
        }
    }
}
