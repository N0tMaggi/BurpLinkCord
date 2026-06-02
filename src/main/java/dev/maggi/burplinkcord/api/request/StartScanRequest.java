package dev.maggi.burplinkcord.api.request;

import java.util.List;

/**
 * Request payload for starting a new scan.
 *
 * @param target target URL or host
 * @param includedDomains explicitly allowed domains
 * @param profileName selected profile name
 * @param configurationName selected configuration name
 * @param crawl whether crawl should be enabled
 * @param audit whether audit should be enabled
 */
public record StartScanRequest(
        String target,
        List<String> includedDomains,
        String profileName,
        String configurationName,
        boolean crawl,
        boolean audit
) {
}
