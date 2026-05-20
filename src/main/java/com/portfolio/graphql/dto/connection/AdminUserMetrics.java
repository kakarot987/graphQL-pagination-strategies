package com.portfolio.graphql.dto.connection;

public record AdminUserMetrics(
    long totalActive,
    long totalInactive,
    long newUsersThisWeek,
    double averageOrderValue
) {}
