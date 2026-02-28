package cn.lanink.viaproxyauthwdpe;

import com.google.gson.JsonObject;
import dev.waterdog.waterdogpe.event.defaults.PlayerPreAuthEvent;
import dev.waterdog.waterdogpe.plugin.Plugin;

public class ViaProxyAuthPlugin extends Plugin {

    private String authSecret;
    private int tokenTimeout;

    @Override
    public void onEnable() {
        this.loadConfig();
        this.authSecret = getConfig().getString("auth-secret", "");
        this.tokenTimeout = getConfig().getInt("token-timeout", 30);

        getProxy().getEventManager().subscribe(PlayerPreAuthEvent.class, this::onPreAuth);
        getLogger().info("ViaProxyAuth enabled {}", authSecret.isEmpty() ? " (auth-secret not configured, validation skipped)" : "");
    }

    private void onPreAuth(PlayerPreAuthEvent event) {
        JsonObject clientData = event.getClientData();

        if (!clientData.has("ViaProxyAuthToken")) {
            return; // Bedrock client, let Xbox auth handle it
        }

        if (authSecret.isEmpty()) {
            event.setAuthenticated(true); // No secret configured, allow ViaProxy clients
            return;
        }

        String token = clientData.get("ViaProxyAuthToken").getAsString();
        if (ViaProxyAuthValidator.validate(token, authSecret,
                event.getUuid(), event.getDisplayName(), tokenTimeout)) {
            event.setAuthenticated(true);
        } else {
            event.setKickMessage("ViaProxy authentication failed");
            getLogger().warn("ViaProxy auth failed for {} ({})", event.getDisplayName(), event.getUuid());
        }
    }
}
