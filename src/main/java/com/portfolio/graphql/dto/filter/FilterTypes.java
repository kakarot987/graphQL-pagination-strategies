package com.portfolio.graphql.dto.filter;

import com.portfolio.graphql.model.Activity;
import com.portfolio.graphql.model.User;

public class FilterTypes {

    public record ProductFilter(
        String category,
        Double minPrice,
        Double maxPrice,
        Double minRating,
        String brand,
        Boolean inStockOnly
    ) {}

    public record ProductSort(ProductSortField field, SortDirection direction) {}

    public enum ProductSortField { PRICE, RATING, NAME, CREATED_AT }

    public record ActivityFilter(
        Activity.ActivityType activityType,
        Long userId,
        String fromTimestamp,
        String toTimestamp
    ) {}

    public record UserFilter(
        User.UserStatus status,
        String role,
        String searchTerm
    ) {}

    public record UserSort(UserSortField field, SortDirection direction) {}

    public enum UserSortField { USERNAME, CREATED_AT, LAST_LOGIN, ORDER_COUNT, TOTAL_SPENT }

    public enum SortDirection { ASC, DESC }
}
