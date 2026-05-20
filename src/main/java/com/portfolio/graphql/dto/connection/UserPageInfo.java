package com.portfolio.graphql.dto.connection;

public record UserPageInfo(
    boolean hasNextPage,
    boolean hasPreviousPage,
    String startCursor,
    String endCursor,
    int totalPages,
    int currentPage
) {}
