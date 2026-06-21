package com.mahmutsalih.task_management.security;

import com.mahmutsalih.task_management.entity.Role;
import com.mahmutsalih.task_management.entity.User;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    public String generateToken(User user) {
        long now = Instant.now().toEpochMilli();
        long expiresAt = now + jwtExpiration;
        String roleName = getRoleName(user.getRole());

        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payload = "{"
                + "\"sub\":\"" + escapeJson(user.getEmail()) + "\","
                + "\"userId\":" + user.getId() + ","
                + "\"role\":\"" + escapeJson(roleName) + "\","
                + "\"iat\":" + (now / 1000) + ","
                + "\"exp\":" + (expiresAt / 1000)
                + "}";

        String encodedHeader = base64UrlEncode(header.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = base64UrlEncode(payload.getBytes(StandardCharsets.UTF_8));
        String signature = sign(encodedHeader + "." + encodedPayload);

        return encodedHeader + "." + encodedPayload + "." + signature;
    }

    public String extractEmail(String token) {
        String payload = decodePayload(token);
        return extractStringClaim(payload, "sub");
    }

    public boolean isTokenValid(String token, String email) {
        try {
            if (email == null || email.isBlank()) {
                return false;
            }

            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            String expectedSignature = sign(parts[0] + "." + parts[1]);
            if (!constantTimeEquals(expectedSignature, parts[2])) {
                return false;
            }

            String payload = decodePayload(token);
            long expiresAt = extractLongClaim(payload, "exp");
            String subject = extractStringClaim(payload, "sub");

            return email.equals(subject) && expiresAt > Instant.now().getEpochSecond();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private String decodePayload(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid token");
        }

        return new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
    }

    private String sign(String data) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET environment variable is required");
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            return base64UrlEncode(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not sign JWT token", exception);
        }
    }

    private String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private String extractStringClaim(String payload, String claimName) {
        String pattern = "\"" + claimName + "\":\"";
        int start = payload.indexOf(pattern);
        if (start < 0) {
            return null;
        }

        int valueStart = start + pattern.length();
        int valueEnd = payload.indexOf("\"", valueStart);
        if (valueEnd < 0) {
            return null;
        }

        return payload.substring(valueStart, valueEnd);
    }

    private long extractLongClaim(String payload, String claimName) {
        String pattern = "\"" + claimName + "\":";
        int start = payload.indexOf(pattern);
        if (start < 0) {
            throw new IllegalArgumentException("Missing claim: " + claimName);
        }

        int valueStart = start + pattern.length();
        int valueEnd = payload.indexOf(",", valueStart);
        if (valueEnd < 0) {
            valueEnd = payload.indexOf("}", valueStart);
        }

        return Long.parseLong(payload.substring(valueStart, valueEnd));
    }

    private boolean constantTimeEquals(String left, String right) {
        return MessageDigestUtil.isEqual(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }

    private String getRoleName(Role role) {
        if (role == null || role.getName() == null) {
            return "";
        }

        return role.getName();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
