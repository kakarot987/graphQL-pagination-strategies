package com.portfolio.graphql;

import com.portfolio.graphql.config.CursorUtil;
import com.portfolio.graphql.model.Activity;
import com.portfolio.graphql.repository.ActivityItemRepository;
import com.portfolio.graphql.service.ActivityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureHttpGraphQlTester
class PaginationGraphQlIntegrationTest {

    @Autowired
    private HttpGraphQlTester tester;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private ActivityItemRepository activityRepository;

    @Test
    void connectionFieldsResolveInsteadOfReturningNull() {
        var products = tester.document("""
            { products(page: 0, size: 2) {
                edges { cursor node { id name } }
                nodes { id }
                pageInfo { hasNextPage endCursor }
                totalCount
            } }
            """).execute();
        products.path("products.edges").entityList(Map.class).hasSize(2);
        products.path("products.nodes").entityList(Map.class).hasSize(2);
        products.path("products.pageInfo.hasNextPage").entity(Boolean.class).isEqualTo(true);

        var users = tester.document("""
            { adminUsers(page: 0, size: 2) {
                edges { cursor node { id username } }
                nodes { id }
                pageInfo { hasNextPage endCursor }
            } }
            """).execute();
        users.path("adminUsers.edges").entityList(Map.class).hasSize(2);
        users.path("adminUsers.nodes").entityList(Map.class).hasSize(2);
        users.path("adminUsers.pageInfo.hasNextPage").entity(Boolean.class).isEqualTo(true);

        var activities = tester.document("""
            { activityFeed(first: 2) {
                edges { cursor node { id username } }
                nodes { id }
                pageInfo { hasNextPage endCursor }
            } }
            """).execute();
        activities.path("activityFeed.edges").entityList(Map.class).hasSize(2);
        activities.path("activityFeed.nodes").entityList(Map.class).hasSize(2);
        activities.path("activityFeed.pageInfo.hasNextPage").entity(Boolean.class).isEqualTo(true);
    }

    @Test
    void filteredFeedUsesUserAndTypeAcrossPages() {
        long userId = 99001L;
        for (int i = 0; i < 3; i++) {
            activityService.createActivity(userId, "filter-test", Activity.ActivityType.REVIEW,
                "review " + i, null, null, null);
        }
        activityService.createActivity(userId, "filter-test", Activity.ActivityType.PURCHASE,
            "purchase", null, null, null);

        String query = """
            query($userId: ID!, $after: String) {
                activityFeed(first: 2, after: $after,
                             filter: { userId: $userId, activityType: REVIEW }) {
                    edges { cursor node { id userId activityType } }
                    nodes { id }
                    totalCount
                    pageInfo { hasNextPage endCursor }
                }
            }
            """;

        var firstPage = tester.document(query).variable("userId", String.valueOf(userId)).execute();
        firstPage.path("activityFeed.totalCount").entity(Integer.class).isEqualTo(3);
        firstPage.path("activityFeed.edges").entityList(Map.class).hasSize(2);
        firstPage.path("activityFeed.pageInfo.hasNextPage").entity(Boolean.class).isEqualTo(true);
        String cursor = firstPage.path("activityFeed.pageInfo.endCursor").entity(String.class).get();
        List<String> firstIds = firstPage.path("activityFeed.nodes[*].id").entityList(String.class).get();

        var secondPage = tester.document(query)
            .variable("userId", String.valueOf(userId))
            .variable("after", cursor)
            .execute();
        secondPage.path("activityFeed.totalCount").entity(Integer.class).isEqualTo(3);
        secondPage.path("activityFeed.edges").entityList(Map.class).hasSize(1);
        secondPage.path("activityFeed.pageInfo.hasNextPage").entity(Boolean.class).isEqualTo(false);
        List<String> secondIds = secondPage.path("activityFeed.nodes[*].id").entityList(String.class).get();
        assertThat(secondIds).doesNotContainAnyElementsOf(firstIds);
    }

    @Test
    void savedCursorDoesNotShiftAfterAnInsert() {
        String query = """
            query($after: String) {
                activityFeed(first: 5, after: $after) {
                    nodes { id }
                    pageInfo { endCursor }
                }
            }
            """;
        var first = tester.document(query).execute();
        String cursor = first.path("activityFeed.pageInfo.endCursor").entity(String.class).get();
        List<String> firstIds = first.path("activityFeed.nodes[*].id").entityList(String.class).get();
        List<String> expectedSecondPage = tester.document(query)
            .variable("after", cursor).execute()
            .path("activityFeed.nodes[*].id").entityList(String.class).get();

        activityService.createActivity(99002L, "new-activity", Activity.ActivityType.VIEW,
            "Inserted between pages", null, null, null);

        List<String> actualSecondPage = tester.document(query)
            .variable("after", cursor).execute()
            .path("activityFeed.nodes[*].id").entityList(String.class).get();

        assertThat(actualSecondPage).containsExactlyElementsOf(expectedSecondPage);
        assertThat(actualSecondPage).doesNotContainAnyElementsOf(firstIds);
    }

    @Test
    void timestampFilterIsAppliedToDatabaseQuery() {
        long userId = 99003L;
        Activity old = Activity.builder()
            .userId(userId).username("timestamp-test").activityType(Activity.ActivityType.VIEW)
            .description("too old").timestamp(LocalDateTime.of(2024, 6, 1, 0, 0)).build();
        Activity included = Activity.builder()
            .userId(userId).username("timestamp-test").activityType(Activity.ActivityType.VIEW)
            .description("in range").timestamp(LocalDateTime.of(2025, 6, 1, 0, 0)).build();
        activityRepository.save(old);
        activityRepository.save(included);

        var result = tester.document("""
            query($userId: ID!) {
                activityFeed(first: 10, filter: {
                    userId: $userId,
                    fromTimestamp: "2025-01-01T00:00:00",
                    toTimestamp: "2025-12-31T23:59:59"
                }) {
                    nodes { id description }
                    totalCount
                }
            }
            """).variable("userId", String.valueOf(userId)).execute();

        result.path("activityFeed.totalCount").entity(Integer.class).isEqualTo(1);
        result.path("activityFeed.nodes[*].description")
            .entityList(String.class).containsExactly("in range");
    }

    @Test
    void invalidAndWrongTypeCursorsReturnClearGraphQlErrors() {
        String query = """
            query($after: String) { activityFeed(first: 2, after: $after) { totalCount } }
            """;
        tester.document(query).variable("after", "invalid-cursor").execute()
            .errors().expect(error -> error.getMessage().contains("Invalid cursor"));
        tester.document(query).variable("after", CursorUtil.encode("product", 10L)).execute()
            .errors().expect(error -> error.getMessage().contains("Invalid cursor"));
    }

    @Test
    void rejectsUnboundedPagination() {
        tester.document("{ activityFeed(first: 0) { totalCount } }").execute()
            .errors().expect(error -> error.getMessage().contains("first must be between 1 and 100"));
        tester.document("{ activityFeed(first: 101) { totalCount } }").execute()
            .errors().expect(error -> error.getMessage().contains("first must be between 1 and 100"));
    }
}
