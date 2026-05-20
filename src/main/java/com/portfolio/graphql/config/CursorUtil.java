package com.portfolio.graphql.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Cursor utility for GraphQL pagination.
 *
 * Cursors are opaque Base64-encoded strings that encode the item ID.
 * Format: "cursor:TYPE:ID" -> Base64 encoded
 *
 * This makes cursors:
 * - Opaque to clients (they can't guess next cursor)
 * - Stable (tied to item ID, not position)
 * - Type-safe (embeds the entity type)
 */
public class CursorUtil {

    private static final String CURSOR_PREFIX = "cursor:";

    public static String encode(String type, Long id) {
        String raw = CURSOR_PREFIX + type + ":" + id;
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static Long decode(String cursor) {
        try {
            String decoded = new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            return Long.parseLong(parts[parts.length - 1]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cursor: " + cursor);
        }
    }

    public static boolean isValid(String cursor) {
        try {
            decode(cursor);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
