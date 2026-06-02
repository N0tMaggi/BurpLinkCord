package dev.maggi.burplinkcord.burp;

import burp.api.montoya.MontoyaApi;
import dev.maggi.burplinkcord.bootstrap.ApplicationBootstrap;

/**
 * Burp Suite extension entrypoint for BurpLinkCord.
 */
public class BurpExtension implements burp.api.montoya.BurpExtension {

    /**
     * Initializes the extension.
     *
     * @param api Montoya API instance
     */
    @Override
    public void initialize(MontoyaApi api) {
        BurpLifecycleManager lifecycleManager = new ApplicationBootstrap().initialize(api);
        lifecycleManager.start();
    }
}
