package com.portfolio.graphql.dto.connection;

import com.portfolio.graphql.model.Product;

public record ProductEdge(Product node, String cursor) {}
