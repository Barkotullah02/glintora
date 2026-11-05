package com.glintora.backend.repository;

import com.glintora.backend.model.Product;
import com.glintora.backend.model.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByProductCode(String productCode);

    List<Product> findAllByOrderByCreatedAtDesc();

    List<Product> findByStatus(ProductStatus status);

    /** Products eligible for the public storefront. */
    @Query("""
           select p from Product p
           where p.status = :status
           order by p.createdAt desc
           """)
    List<Product> findPublished(@Param("status") ProductStatus status);

    /** Newest published products first — powers the New Arrivals rail. */
    @Query("""
           select p from Product p
           where p.status = com.glintora.backend.model.enums.ProductStatus.published
           order by p.createdAt desc, p.id desc
           """)
    List<Product> findNewArrivals();

    /** Units sold per published product, for the Bestsellers rail. */
    @Query("""
           select p.id, coalesce(sum(oi.quantity), 0) as sold
           from OrderItem oi
           join oi.variant v
           join v.product p
           where p.status = com.glintora.backend.model.enums.ProductStatus.published
             and oi.order.status <> com.glintora.backend.model.enums.OrderStatus.cancelled
           group by p.id
           order by sold desc
           """)
    List<Object[]> bestsellersByUnitsSold();

    /** Filtered catalogue query used by the storefront shop page. */
    @Query("""
           select distinct p from Product p
           left join p.variants v
           where p.status = com.glintora.backend.model.enums.ProductStatus.published
             and (:q is null or lower(p.name) like lower(concat('%', cast(:q as string), '%'))
                             or lower(p.productCode) like lower(concat('%', cast(:q as string), '%')))
             and (:categoryId is null or p.category.id = :categoryId)
             and (:brandId is null or p.brand.id = :brandId)
             and (:sizeId is null or v.size.id = :sizeId)
           order by p.createdAt desc
           """)
    List<Product> searchPublished(@Param("q") String q,
                                  @Param("categoryId") Long categoryId,
                                  @Param("brandId") Long brandId,
                                  @Param("sizeId") Long sizeId);

    long countByStatus(ProductStatus status);

    List<Product> findTop8ByStatusOrderByNameAsc(ProductStatus status);

    List<Product> findTop4ByCategoryIdAndIdNot(Long categoryId, Long id);

    /** Admin catalogue search: any publication status, name/code/brand matching. */
    @Query("""
           select distinct p from Product p
           left join p.brand b
           where (:q is null or lower(p.name) like lower(concat('%', cast(:q as string), '%'))
                               or lower(p.productCode) like lower(concat('%', cast(:q as string), '%'))
                               or lower(b.name) like lower(concat('%', cast(:q as string), '%')))
             and (:categoryId is null or p.category.id = :categoryId)
             and (:brandId is null or p.brand.id = :brandId)
             and (:status is null or p.status = :status)
           order by p.createdAt desc
           """)
    List<Product> searchAll(@Param("q") String q,
                            @Param("categoryId") Long categoryId,
                            @Param("brandId") Long brandId,
                            @Param("status") ProductStatus status);
}
