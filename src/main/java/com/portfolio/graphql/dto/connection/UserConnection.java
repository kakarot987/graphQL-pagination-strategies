package com.portfolio.graphql.dto.connection;

import com.portfolio.graphql.model.User;
import java.util.List;

public record UserConnection(
    List<UserEdge> edges,
    List<User> nodes,
    UserPageInfo pageInfo,
    long totalCount,
    boolean isEmpty,
    AdminUserMetrics adminMetrics
) {}
