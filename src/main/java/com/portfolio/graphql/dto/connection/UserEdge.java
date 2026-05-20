package com.portfolio.graphql.dto.connection;

import com.portfolio.graphql.model.User;

public record UserEdge(User node, String cursor) {}
