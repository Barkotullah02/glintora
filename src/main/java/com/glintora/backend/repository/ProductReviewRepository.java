package com.glintora.backend.repository;

import com.glintora.backend.model.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    List<ProductReview> findByProductIdAndStatusOrderByCreatedAtDesc(Long productId, String status);

    @Query("select coalesce(avg(r.rating), 0) from ProductReview r where r.product.id = :productId and r.status = 'approved'")
    Double averageRating(@Param("productId") Long productId);

    long countByProductIdAndStatus(Long productId, String status);

    List<ProductReview> findAllByOrderByCreatedAtDesc();

    List<ProductReview> findByStatusOrderByCreatedAtDesc(String status);

    long countByStatus(String status);
}
