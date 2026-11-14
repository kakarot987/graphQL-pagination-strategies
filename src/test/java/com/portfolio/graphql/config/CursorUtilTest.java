package com.portfolio.graphql.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CursorUtilTest {

    @Test
    void roundTripsTypedCursor() {
        String cursor = CursorUtil.encode("activity", 42L);
        assertThat(CursorUtil.decode(cursor, "activity")).isEqualTo(42L);
        assertThat(CursorUtil.isValid(cursor)).isTrue();
    }

    @Test
    void rejectsMalformedAndWrongTypeCursors() {
        assertThatThrownBy(() -> CursorUtil.decode("not-base64", "activity"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid cursor");

        assertThatThrownBy(() -> CursorUtil.decode(CursorUtil.encode("product", 5L), "activity"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid cursor");

        String missingPrefix = Base64.getEncoder().encodeToString("wrong:activity:7".getBytes(StandardCharsets.UTF_8));
        String negativeId = Base64.getEncoder().encodeToString("cursor:activity:-7".getBytes(StandardCharsets.UTF_8));
        String trailingFields = Base64.getEncoder().encodeToString("cursor:activity:7:ignored".getBytes(StandardCharsets.UTF_8));
        for (String cursor : new String[]{missingPrefix, negativeId, trailingFields}) {
            assertThatThrownBy(() -> CursorUtil.decode(cursor, "activity"))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
