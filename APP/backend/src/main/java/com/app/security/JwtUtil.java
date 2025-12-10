package com.app.security;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Простая реализация JWT (HS256) без внешних библиотек.
 * Токен формата: header.payload.signature (Base64URL)
 */
public class JwtUtil {

    // В реальном проде вынести в ENV / конфиг
    private static final String SECRET = "SUPER_SECRET_CHANGE_ME";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] base64UrlDecode(String str) {
        return Base64.getUrlDecoder().decode(str);
    }

    private static String hmacSha256(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] sig = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return base64UrlEncode(sig);
    }

    /**
     * Создаёт JWT токен с полями:
     *  - sub: userId
     *  - email
     *  - exp: unix timestamp
     */
    public static String generateToken(int userId, String email, long ttlSeconds) throws Exception {
        Map<String, Object> header = new HashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        long now = Instant.now().getEpochSecond();
        long exp = now + ttlSeconds;

        Map<String, Object> payload = new HashMap<>();
        payload.put("sub", String.valueOf(userId));
        payload.put("email", email);
        payload.put("exp", exp);

        String headerJson = MAPPER.writeValueAsString(header);
        String payloadJson = MAPPER.writeValueAsString(payload);

        String headerPart = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
        String payloadPart = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));

        String unsignedToken = headerPart + "." + payloadPart;
        String signature = hmacSha256(unsignedToken);

        return unsignedToken + "." + signature;
    }

    /**
     * Проверяет подпись и срок жизни токена.
     * @return payload либо null, если токен недействителен
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> validateToken(String token) {
        try {
            if (token == null || token.isBlank()) {
                return null;
            }

            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            String unsigned = parts[0] + "." + parts[1];
            String signature = parts[2];

            String expectedSig = hmacSha256(unsigned);
            if (!expectedSig.equals(signature)) {
                return null;
            }

            String payloadJson = new String(base64UrlDecode(parts[1]), StandardCharsets.UTF_8);
            Map<String, Object> payload = MAPPER.readValue(payloadJson, Map.class);

            Object expObj = payload.get("exp");
            if (expObj instanceof Number) {
                long exp = ((Number) expObj).longValue();
                long now = Instant.now().getEpochSecond();
                if (now > exp) {
                    return null; // токен истёк
                }
            }

            return payload;
        } catch (Exception e) {
            return null;
        }
    }
}
