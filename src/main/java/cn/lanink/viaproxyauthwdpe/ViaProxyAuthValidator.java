package cn.lanink.viaproxyauthwdpe;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

public class ViaProxyAuthValidator {

    private ViaProxyAuthValidator() {
    }

    /**
     * Validate ViaProxy auth token.
     * Token format: Base64(HMAC-SHA256(secret, UUID:Name:Timestamp)):Timestamp
     */
    public static boolean validate(String token, String secret,
                                   UUID uuid, String username, int timeoutSeconds) {
        // 1. Parse token = hmac:timestamp
        int sep = token.lastIndexOf(':');
        if (sep < 0) {
            return false;
        }

        String receivedHmac = token.substring(0, sep);
        long timestamp;
        try {
            timestamp = Long.parseLong(token.substring(sep + 1));
        } catch (NumberFormatException e) {
            return false;
        }

        // 2. Time window check (anti-replay)
        long now = System.currentTimeMillis() / 1000;
        if (Math.abs(now - timestamp) > timeoutSeconds) {
            return false;
        }

        // 3. HMAC-SHA256 signature verification
        String payload = uuid + ":" + username + ":" + timestamp;
        String expectedHmac = computeHmacSha256(secret, payload);
        if (expectedHmac == null) {
            return false;
        }

        return MessageDigest.isEqual(
                expectedHmac.getBytes(StandardCharsets.UTF_8),
                receivedHmac.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Compute HMAC-SHA256, matching ViaProxy SkinProvider.computeHmacSha256
     */
    private static String computeHmacSha256(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return null;
        }
    }
}
