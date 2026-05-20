package com.portfolio.graphql.repository;

import com.portfolio.graphql.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Query("SELECT DISTINCT p.category FROM Product p ORDER BY p.category")
    List<String> findDistinctCategories();

    @Query("SELECT DISTINCT p.brand FROM Product p ORDER BY p.brand")
    List<String> findDistinctBrands();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.category = :category")
    long countByCategory(String category);

    @Query("SELECT MIN(p.price) FROM Product p")
    Double findMinPrice();

    @Query("SELECT MAX(p.price) FROM Product p")
    Double findMaxPrice();

    @Query("SELECT AVG(p.price) FROM Product p")
    Double findAvgPrice();
}
