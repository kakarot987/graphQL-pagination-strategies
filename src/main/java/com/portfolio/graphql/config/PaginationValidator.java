package com.portfolio.graphql.config;

import com.portfolio.graphql.dto.filter.FilterTypes.ProductFilter;

/** Validate pagination and product filter arguments before hitting the database. */
public final class PaginationValidator {

    public static final int MAX_PAGE_SIZE = 100;

    private PaginationValidator() {
    }

    public static void validateOffset(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    public static void validateProductFilter(ProductFilter filter) {
        if (filter == null) {
            return;
        }

        validateNonNegativeMoney(filter.minPrice(), "minPrice");
        validateNonNegativeMoney(filter.maxPrice(), "maxPrice");

        if (filter.minPrice() != null && filter.maxPrice() != null
            && filter.minPrice() > filter.maxPrice()) {
            throw new IllegalArgumentException("minPrice must not exceed maxPrice");
        }
        if (filter.minRating() != null && (!Double.isFinite(filter.minRating())
            || filter.minRating() < 0 || filter.minRating() > 5)) {
            throw new IllegalArgumentException("minRating must be between 0 and 5");
        }
    }

    private static void validateNonNegativeMoney(Double value, String name) {
        if (value != null && (!Double.isFinite(value) || value < 0)) {
            throw new IllegalArgumentException(name + " must be a finite non-negative number");
        }
    }
}
