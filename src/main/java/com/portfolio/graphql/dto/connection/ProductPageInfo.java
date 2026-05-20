package com.portfolio.graphql.dto.connection;

public record ProductPageInfo(
    boolean hasNextPage,
    boolean hasPreviousPage,
    String startCursor,
    String endCursor,
    int totalPages,
    int currentPage
) {}
