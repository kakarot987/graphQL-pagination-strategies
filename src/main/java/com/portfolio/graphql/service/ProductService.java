package com.portfolio.graphql.service;

import com.portfolio.graphql.config.CursorUtil;
import com.portfolio.graphql.config.PaginationValidator;
import com.portfolio.graphql.dto.connection.*;
import com.portfolio.graphql.dto.filter.FilterTypes.*;
import com.portfolio.graphql.model.Product;
import com.portfolio.graphql.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
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
    private final EntityManager entityManager;

    /**
     * Offset-based pagination — ideal for e-commerce catalogs.
     * Clients can jump to any page and see total page count.
     */
    public ProductConnection getProducts(int page, int size, ProductFilter filter, ProductSort sort) {
        PaginationValidator.validateOffset(page, size);
        PaginationValidator.validateProductFilter(filter);
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
        PriceRange priceRange = buildPriceRange(filter);

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
        if (sort == null) {
            return Sort.by(Sort.Direction.ASC, "name").and(Sort.by(Sort.Direction.ASC, "id"));
        }

        String field = switch (sort.field()) {
            case PRICE -> "price";
            case RATING -> "rating";
            case NAME -> "name";
            case CREATED_AT -> "createdAt";
        };
        Sort.Direction direction = sort.direction() == SortDirection.ASC
            ? Sort.Direction.ASC : Sort.Direction.DESC;

        // A unique tie-breaker prevents equal sort values from changing page boundaries.
        return Sort.by(direction, field).and(Sort.by(Sort.Direction.ASC, "id"));
    }

    private Specification<Product> buildSpecification(ProductFilter filter) {
        return (root, query, cb) -> productPredicate(root, cb, filter);
    }

    /** Reused for the result page, category facets and price aggregates. */
    private Predicate productPredicate(Root<Product> root, CriteriaBuilder cb, ProductFilter filter) {
        if (filter == null) {
            return cb.conjunction();
        }

        List<Predicate> predicates = new ArrayList<>();
        if (filter.category() != null && !filter.category().isBlank()) {
            predicates.add(cb.equal(root.get("category"), filter.category()));
        }
        if (filter.brand() != null && !filter.brand().isBlank()) {
            predicates.add(cb.equal(root.get("brand"), filter.brand()));
        }
        if (filter.minPrice() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("price"),
                BigDecimal.valueOf(filter.minPrice())));
        }
        if (filter.maxPrice() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("price"),
                BigDecimal.valueOf(filter.maxPrice())));
        }
        if (filter.minRating() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.<Double>get("rating"), filter.minRating()));
        }
        if (Boolean.TRUE.equals(filter.inStockOnly())) {
            predicates.add(cb.greaterThan(root.<Integer>get("stockCount"), 0));
        }
        return cb.and(predicates.toArray(new Predicate[0]));
    }

    /**
     * One grouped aggregation query instead of one count query per category.
     * Facets reflect ALL current product filters, including category.
     */
    private List<CategoryFacet> buildCategoryFacets(ProductFilter filter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Product> root = query.from(Product.class);
        Path<String> category = root.get("category");

        query.multiselect(category.alias("category"), cb.count(root).alias("count"))
            .where(productPredicate(root, cb, filter))
            .groupBy(category)
            .orderBy(cb.asc(category));

        return entityManager.createQuery(query).getResultList().stream()
            .map(row -> new CategoryFacet(
                row.get("category", String.class),
                row.get("count", Long.class)))
            .toList();
    }

    /** Price metadata is aggregated over the filtered result set as well. */
    private PriceRange buildPriceRange(ProductFilter filter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Product> root = query.from(Product.class);

        query.multiselect(
                cb.min(root.<BigDecimal>get("price")).alias("min"),
                cb.max(root.<BigDecimal>get("price")).alias("max"),
                cb.avg(root.<BigDecimal>get("price")).alias("avg"))
            .where(productPredicate(root, cb, filter));

        Tuple row = entityManager.createQuery(query).getSingleResult();
        BigDecimal min = row.get("min", BigDecimal.class);
        BigDecimal max = row.get("max", BigDecimal.class);
        Double avg = row.get("avg", Double.class);
        return new PriceRange(
            min == null ? 0.0 : min.doubleValue(),
            max == null ? 0.0 : max.doubleValue(),
            avg == null ? 0.0 : avg
        );
    }
}
