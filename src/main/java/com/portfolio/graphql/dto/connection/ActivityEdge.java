package com.portfolio.graphql.dto.connection;

import com.portfolio.graphql.model.Activity;

public record ActivityEdge(Activity node, String cursor, String relationshipNote) {}
