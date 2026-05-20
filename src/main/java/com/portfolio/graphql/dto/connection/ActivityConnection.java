package com.portfolio.graphql.dto.connection;

import com.portfolio.graphql.model.Activity;
import java.util.List;

public record ActivityConnection(
    List<ActivityEdge> edges,
    List<Activity> nodes,
    ActivityPageInfo pageInfo,
    long totalCount,
    boolean isEmpty,
    ActivityFeedMeta feedMeta
) {}
