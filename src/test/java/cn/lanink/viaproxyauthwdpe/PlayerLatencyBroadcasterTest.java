package cn.lanink.viaproxyauthwdpe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import org.cloudburstmc.protocol.bedrock.packet.ScriptMessagePacket;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlayerLatencyBroadcasterTest {

    private static final UUID BEDROCK = UUID.fromString("12345678-1234-5678-9abc-def012345678");
    private static final UUID JAVA = UUID.fromString("87654321-4321-8765-cba9-876543210fed");

    @Test
    void snapshotUsesLoginUuidAndClampsNegativePing() {
        final ProxiedPlayer bedrock = player(BEDROCK, 87L);
        final ProxiedPlayer java = player(JAVA, -1L);

        final JsonObject snapshot = JsonParser.parseString(
                PlayerLatencyBroadcaster.encodeSnapshot(List.of(bedrock, java))).getAsJsonObject();

        assertEquals(87, snapshot.get(BEDROCK.toString()).getAsInt());
        assertEquals(0, snapshot.get(JAVA.toString()).getAsInt());
        assertEquals(2, snapshot.size());
    }

    @Test
    void emptySnapshotIsValidJsonObject() {
        assertEquals("{}", PlayerLatencyBroadcaster.encodeSnapshot(List.of()));
    }

    @Test
    void packetUsesNeutralWaterdogMessageId() {
        final ScriptMessagePacket packet = PlayerLatencyBroadcaster.createPacket("{\"" + BEDROCK + "\":42}");
        assertEquals("waterdog:player_latency_v1", packet.getChannel());
        assertEquals(PlayerLatencyBroadcaster.MESSAGE_ID, packet.getChannel());
        assertTrue(packet.getMessage().contains(BEDROCK.toString()));
    }

    private static ProxiedPlayer player(UUID uuid, long ping) {
        final ProxiedPlayer player = mock(ProxiedPlayer.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getPing()).thenReturn(ping);
        return player;
    }
}
