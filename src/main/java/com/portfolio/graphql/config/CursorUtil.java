package com.portfolio.graphql.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Encodes cursors as Base64("cursor:TYPE:ID").
 *
 * Base64 provides an opaque API token by convention, but is NOT encryption,
 * authentication, or protection against cursor tampering.
 */
public final class CursorUtil {

    private CursorUtil() {
    }

    public static String encode(String type, Long id) {
        if (type == null || type.isBlank() || type.contains(":") || id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid cursor components");
        }
        String value = "cursor:" + type + ":" + id;
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public static Long decode(String cursor) {
        return parse(cursor).id();
    }

    /** Reject cursors for another connection, even if their ID is valid. */
    public static Long decode(String cursor, String expectedType) {
        CursorValue value = parse(cursor);
        if (!value.type().equals(expectedType)) {
            throw new IllegalArgumentException("Invalid cursor: expected " + expectedType + " cursor");
        }
        return value.id();
    }

    public static boolean isValid(String cursor) {
        try {
            parse(cursor);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static CursorValue parse(String cursor) {
        try {
            if (cursor == null || cursor.isBlank()) {
                throw new IllegalArgumentException("Missing cursor");
            }
            String decoded = new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":", -1);
            if (parts.length != 3 || !parts[0].equals("cursor") || parts[1].isBlank()) {
                throw new IllegalArgumentException("Unexpected cursor format");
            }
            long id = Long.parseLong(parts[2]);
            if (id <= 0) {
                throw new IllegalArgumentException("Cursor ID must be positive");
            }
            return new CursorValue(parts[1], id);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid cursor", ex);
        }
    }

    private record CursorValue(String type, long id) {
    }
}
