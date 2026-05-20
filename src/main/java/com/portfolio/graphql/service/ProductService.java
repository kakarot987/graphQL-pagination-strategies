package com.portfolio.graphql.service;

import com.portfolio.graphql.config.CursorUtil;
import com.portfolio.graphql.dto.connection.*;
import com.portfolio.graphql.dto.filter.FilterTypes.*;
import com.portfolio.graphql.model.Product;
import com.portfolio.graphql.repository.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * Offset-based pagination — ideal for e-commerce catalogs.
     * Clients can jump to any page and see total page count.
     */
    public ProductConnection getProducts(int page, int size, ProductFilter filter, ProductSort sort) {
        log.info("Fetching products: page={}, size={}, filter={}", page, size, filter);

        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        Specification<Product> spec = buildSpecification(filter);

        Page<Product> productPage = productRepository.findAll(spec, pageable);
        List<Product> products = productPage.getContent();

        // Build edges with cursors
        List<ProductEdge> edges = products.stream()
            .map(p -> new ProductEdge(p, CursorUtil.encode("product", p.getId())))
            .toList();

        // Build offset-style PageInfo
        ProductPageInfo pageInfo = new ProductPageInfo(
            productPage.hasNext(),
            productPage.hasPrevious(),
            edges.isEmpty() ? null : edges.getFirst().cursor(),
            edges.isEmpty() ? null : edges.getLast().cursor(),
            productPage.getTotalPages(),
            page
        );

        // Compute facets & price range (incremental metadata — non-breaking additions)
        List<CategoryFacet> facets = buildCategoryFacets(filter);
        PriceRange priceRange = buildPriceRange();

        return new ProductConnection(
            edges,
            products,
            pageInfo,
            productPage.getTotalElements(),
            products.isEmpty(),         // isEmpty — easier UI empty state handling
            facets,
            priceRange
        );
    }

    public Optional<Product> getProduct(Long id) {
        return productRepository.findById(id);
    }

    // ---- Private helpers ----

    private Sort buildSort(ProductSort sort) {
        if (sort == null) return Sort.by(Sort.Direction.ASC, "name");
        String field = switch (sort.field()) {
            case PRICE -> "price";
            case RATING -> "rating";
            case NAME -> "name";
            case CREATED_AT -> "createdAt";
        };
        return sort.direction() == SortDirection.ASC
            ? Sort.by(Sort.Direction.ASC, field)
            : Sort.by(Sort.Direction.DESC, field);
    }

    private Specification<Product> buildSpecification(ProductFilter filter) {
        return (root, query, cb) -> {
            if (filter == null) return cb.conjunction();

            List<Predicate> predicates = new ArrayList<>();

            if (filter.category() != null && !filter.category().isBlank()) {
                predicates.add(cb.equal(root.get("category"), filter.category()));
            }
            if (filter.brand() != null && !filter.brand().isBlank()) {
                predicates.add(cb.equal(root.get("brand"), filter.brand()));
            }
            if (filter.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), filter.minPrice()));
            }
            if (filter.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), filter.maxPrice()));
            }
            if (filter.minRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), filter.minRating()));
            }
            if (Boolean.TRUE.equals(filter.inStockOnly())) {
                predicates.add(cb.greaterThan(root.get("stockCount"), 0));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private List<CategoryFacet> buildCategoryFacets(ProductFilter filter) {
        return productRepository.findDistinctCategories()
            .stream()
            .map(cat -> new CategoryFacet(cat, productRepository.countByCategory(cat)))
            .toList();
    }

    private PriceRange buildPriceRange() {
        Double min = productRepository.findMinPrice();
        Double max = productRepository.findMaxPrice();
        Double avg = productRepository.findAvgPrice();
        return new PriceRange(
            min != null ? min : 0.0,
            max != null ? max : 0.0,
            avg != null ? avg : 0.0
        );
    }
}
