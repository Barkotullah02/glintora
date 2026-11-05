package com.glintora.backend.repository;

import com.glintora.backend.model.Order;
import com.glintora.backend.model.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findAllByOrderByOrderDateDesc();

    List<Order> findByCustomerIdOrderByOrderDateDesc(Long customerId);

    List<Order> findByStatusOrderByOrderDateDesc(OrderStatus status);

    List<Order> findTop8ByOrderByOrderDateDesc();

    long countByStatus(OrderStatus status);

    /** Revenue across all non-cancelled orders. */
    @Query("select coalesce(sum(o.grandTotal), 0) from Order o where o.status <> 'cancelled'")
    BigDecimal sumRevenue();

    // ---------------------------------------------------- dashboard periods

    /** Revenue in [from, to), excluding cancellations. */
    @Query("""
           select coalesce(sum(o.grandTotal), 0)
           from Order o
           where o.status <> com.glintora.backend.model.enums.OrderStatus.cancelled
             and o.orderDate >= :from and o.orderDate < :to
           """)
    BigDecimal sumRevenueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Order count in [from, to). */
    @Query("select count(o) from Order o where o.orderDate >= :from and o.orderDate < :to")
    long countBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** One row per day: [date, revenue, order count] — powers the sales chart. */
    @Query("""
           select cast(o.orderDate as date) as day,
                  coalesce(sum(o.grandTotal), 0) as revenue,
                  count(o) as orders
           from Order o
           where o.status <> com.glintora.backend.model.enums.OrderStatus.cancelled
             and o.orderDate >= :from
           group by cast(o.orderDate as date)
           order by day asc
           """)
    List<Object[]> revenueByDaySince(@Param("from") LocalDateTime from);

    // ------------------------------------------------------- customer stats

    /** [customerId, order count, total spend, last order date] for all customers with orders. */
    @Query("""
           select o.customer.id, count(o), coalesce(sum(o.grandTotal), 0), max(o.orderDate)
           from Order o
           where o.status <> com.glintora.backend.model.enums.OrderStatus.cancelled
           group by o.customer.id
           """)
    List<Object[]> customerStats();
}
