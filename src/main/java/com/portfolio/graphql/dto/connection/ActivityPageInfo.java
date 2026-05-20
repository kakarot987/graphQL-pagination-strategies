package com.portfolio.graphql.dto.connection;

public record ActivityPageInfo(
    boolean hasNextPage,
    boolean hasPreviousPage,
    String startCursor,
    String endCursor
) {}
