package com.portfolio.graphql.dto.connection;

import com.portfolio.graphql.model.Product;
import java.util.List;

public record ProductConnection(
    List<ProductEdge> edges,
    List<Product> nodes,
    ProductPageInfo pageInfo,
    long totalCount,
    boolean isEmpty,
    List<CategoryFacet> categoryFacets,
    PriceRange priceRange
) {}
