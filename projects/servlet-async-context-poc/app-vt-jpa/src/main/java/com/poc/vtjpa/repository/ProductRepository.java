package com.poc.vtjpa.repository;

import com.poc.vtjpa.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query(value = "SELECT p.* FROM products p, (SELECT pg_sleep(:seconds) AS s) AS sub ORDER BY p.id LIMIT 1",
            nativeQuery = true)
    Product findFirstSlow(@Param("seconds") double seconds);

    default Product findFirstSlow() {
        return findFirstSlow(1.0d);
    }
}
