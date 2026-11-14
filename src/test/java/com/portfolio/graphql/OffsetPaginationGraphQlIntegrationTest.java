package com.portfolio.graphql;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureHttpGraphQlTester
class OffsetPaginationGraphQlIntegrationTest {

    @Autowired
    private HttpGraphQlTester tester;

    @Test
    void rejectsInvalidOffsetPages() {
        assertBadRequest("{ products(page: -1) { totalCount } }", "page must be >= 0");
        assertBadRequest("{ adminUsers(page: -1) { totalCount } }", "page must be >= 0");
    }

    @Test
    void rejectsInvalidSizes() {
        for (int size : new int[]{0, -2, 101}) {
            assertBadRequest("{ products(size: " + size + ") { totalCount } }",
                "size must be between 1 and 100");
            assertBadRequest("{ adminUsers(size: " + size + ") { totalCount } }",
                "size must be between 1 and 100");
        }
    }

    @Test
    void rejectsInvalidProductPriceAndRatingFilters() {
        assertBadRequest(
            "{ products(filter: { minPrice: 100, maxPrice: 10 }) { totalCount } }",
            "minPrice must not exceed maxPrice");
        assertBadRequest(
            "{ products(filter: { minPrice: -1 }) { totalCount } }",
            "minPrice must be a finite non-negative number");
        assertBadRequest(
            "{ products(filter: { maxPrice: -1 }) { totalCount } }",
            "maxPrice must be a finite non-negative number");
        assertBadRequest(
            "{ products(filter: { minRating: 5.1 }) { totalCount } }",
            "minRating must be between 0 and 5");
        assertBadRequest(
            "{ products(filter: { minRating: -0.5 }) { totalCount } }",
            "minRating must be between 0 and 5");
    }

    @Test
    void acceptsPageSizeBoundary() {
        tester.document("{ products(size: 100) { totalCount pageInfo { currentPage } } }")
            .execute()
            .path("products.pageInfo.currentPage").entity(Integer.class).isEqualTo(0);
        tester.document("{ adminUsers(size: 1) { nodes { id } } }")
            .execute()
            .path("adminUsers.nodes").entityList(Map.class).hasSize(1);
    }

    @Test
    void ratingSortBreaksTiesByAscendingId() {
        List<Map> products = tester.document("""
            { products(page: 0, size: 100, sort: { field: RATING, direction: DESC }) {
                nodes { id rating }
            } }
            """).execute().path("products.nodes").entityList(Map.class).get();

        assertThat(products).isNotEmpty();
        boolean foundTie = false;
        for (int i = 1; i < products.size(); i++) {
            double previous = ((Number) products.get(i - 1).get("rating")).doubleValue();
            double current = ((Number) products.get(i).get("rating")).doubleValue();
            assertThat(previous).isGreaterThanOrEqualTo(current);
            if (Double.compare(previous, current) == 0) {
                foundTie = true;
                long previousId = Long.parseLong(String.valueOf(products.get(i - 1).get("id")));
                long currentId = Long.parseLong(String.valueOf(products.get(i).get("id")));
                assertThat(previousId).isLessThan(currentId);
            }
        }
        assertThat(foundTie).isTrue();
    }

    @Test
    void orderCountSortBreaksTiesByAscendingId() {
        List<Map> users = tester.document("""
            { adminUsers(page: 0, size: 100, sort: { field: ORDER_COUNT, direction: ASC }) {
                nodes { id orderCount }
            } }
            """).execute().path("adminUsers.nodes").entityList(Map.class).get();

        assertThat(users).isNotEmpty();
        for (int i = 1; i < users.size(); i++) {
            int previous = ((Number) users.get(i - 1).get("orderCount")).intValue();
            int current = ((Number) users.get(i).get("orderCount")).intValue();
            assertThat(previous).isLessThanOrEqualTo(current);
            if (previous == current) {
                long previousId = Long.parseLong(String.valueOf(users.get(i - 1).get("id")));
                long currentId = Long.parseLong(String.valueOf(users.get(i).get("id")));
                assertThat(previousId).isLessThan(currentId);
            }
        }
    }

    private void assertBadRequest(String query, String expectedMessage) {
        tester.document(query).execute()
            .errors().expect(error -> error.getMessage().contains(expectedMessage));
    }
}
