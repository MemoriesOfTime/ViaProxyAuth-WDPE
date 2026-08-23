package cn.lanink.viaproxyauthwdpe;

import com.google.gson.JsonObject;
import dev.waterdog.waterdogpe.event.defaults.InitialServerConnectedEvent;
import dev.waterdog.waterdogpe.event.defaults.PlayerPreAuthEvent;
import dev.waterdog.waterdogpe.event.defaults.TransferCompleteEvent;
import dev.waterdog.waterdogpe.network.protocol.ProtocolCodecs;
import dev.waterdog.waterdogpe.plugin.Plugin;
import dev.waterdog.waterdogpe.scheduler.TaskHandler;
import org.cloudburstmc.protocol.bedrock.packet.ScriptMessagePacket;

public class ViaProxyAuthPlugin extends Plugin {

    private String authSecret;
    private int tokenTimeout;
    private boolean broadcastPlayerLatency;
    private TaskHandler<?> latencyTask;

    @Override
    public void onStartup() {
        // enableAllPlugins() runs before ProxyServer builds fast codecs, so this
        // retain must happen here or in onEnable — never after boot.
        ProtocolCodecs.addHandledPacket(ScriptMessagePacket.class);
    }

    @Override
    public void onEnable() {
        this.loadConfig();
        this.authSecret = getConfig().getString("auth-secret", "");
        this.tokenTimeout = getConfig().getInt("token-timeout", 30);
        this.broadcastPlayerLatency = Boolean.TRUE.equals(
                getConfig().getBoolean("broadcast-player-latency", true));

        getProxy().getEventManager().subscribe(PlayerPreAuthEvent.class, this::onPreAuth);
        if (this.broadcastPlayerLatency) {
            this.scheduleLatencyBroadcast();
        }
        getLogger().info("ViaProxyAuth enabled {}", authSecret.isEmpty() ? " (auth-secret not configured, validation skipped)" : "");
        if (this.broadcastPlayerLatency) {
            getLogger().info("Broadcasting player latency as {}", PlayerLatencyBroadcaster.MESSAGE_ID);
        }
    }

    @Override
    public void onDisable() {
        if (this.latencyTask != null) {
            this.latencyTask.cancel();
            this.latencyTask = null;
        }
    }

    private void scheduleLatencyBroadcast() {
        Integer intervalTicks = getConfig().getInt("player-latency-interval-ticks", 20);
        if (intervalTicks == null || intervalTicks < 1) {
            intervalTicks = 20;
        }
        this.latencyTask = getProxy().getScheduler().scheduleDelayedRepeating(
                () -> PlayerLatencyBroadcaster.tick(getProxy()), 1, intervalTicks);
        getProxy().getEventManager().subscribe(TransferCompleteEvent.class, event -> this.queueLatencyTick());
        getProxy().getEventManager().subscribe(InitialServerConnectedEvent.class, event -> this.queueLatencyTick());
    }

    private void queueLatencyTick() {
        getProxy().getScheduler().scheduleTask(() -> PlayerLatencyBroadcaster.tick(getProxy()), false);
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
