package com.portfolio.graphql.dto.connection;

import com.portfolio.graphql.model.Activity;
import com.portfolio.graphql.model.Product;
import com.portfolio.graphql.model.User;

import java.util.List;

// ---- Shared Edge/Connection records ----

public class ConnectionTypes {

    // Product Connection (Offset-based)
    public record ProductEdge(Product node, String cursor) {}

    public record ProductPageInfo(
        boolean hasNextPage,
        boolean hasPreviousPage,
        String startCursor,
        String endCursor,
        int totalPages,
        int currentPage
    ) {}

    public record CategoryFacet(String name, long count) {}

    public record PriceRange(double min, double max, double average) {}

    public record ProductConnection(
        List<ProductEdge> edges,
        List<Product> nodes,
        ProductPageInfo pageInfo,
        long totalCount,
        boolean isEmpty,
        List<CategoryFacet> categoryFacets,
        PriceRange priceRange
    ) {}

    // Activity Connection (Cursor-based)
    public record ActivityEdge(Activity node, String cursor, String relationshipNote) {}

    public record ActivityPageInfo(
        boolean hasNextPage,
        boolean hasPreviousPage,
        String startCursor,
        String endCursor
    ) {}

    public record ActivityFeedMeta(long unreadCount, String lastViewedTimestamp, long totalActivities) {}

    public record ActivityConnection(
        List<ActivityEdge> edges,
        List<Activity> nodes,
        ActivityPageInfo pageInfo,
        long totalCount,
        boolean isEmpty,
        ActivityFeedMeta feedMeta
    ) {}

    // User Connection (Offset-based)
    public record UserEdge(User node, String cursor) {}

    public record UserPageInfo(
        boolean hasNextPage,
        boolean hasPreviousPage,
        String startCursor,
        String endCursor,
        int totalPages,
        int currentPage
    ) {}

    public record AdminUserMetrics(
        long totalActive,
        long totalInactive,
        long newUsersThisWeek,
        double averageOrderValue
    ) {}

    public record UserConnection(
        List<UserEdge> edges,
        List<User> nodes,
        UserPageInfo pageInfo,
        long totalCount,
        boolean isEmpty,
        AdminUserMetrics adminMetrics
    ) {}
}
