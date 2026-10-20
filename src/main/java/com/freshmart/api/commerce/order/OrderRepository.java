package com.freshmart.api.commerce.order;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;
public interface OrderRepository extends JpaRepository<Order,Long>{
 @EntityGraph(attributePaths={"items","items.product"}) Page<Order> findAllByUser_Id(Long userId,Pageable pageable);
 @EntityGraph(attributePaths={"items","items.product"}) Optional<Order> findByIdAndUser_Id(Long id,Long userId);
 @org.springframework.data.jpa.repository.Query("select distinct o from Order o left join fetch o.items i left join fetch i.product where o.id=:id") Optional<Order> findByIdWithItems(@org.springframework.data.repository.query.Param("id") Long id);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select distinct o from Order o left join fetch o.items i left join fetch i.product where o.id=:id") Optional<Order> findByIdForUpdateWithItems(@org.springframework.data.repository.query.Param("id") Long id);
 long countByStatus(String status);
 @org.springframework.data.jpa.repository.Query("select sum(o.totalInr) from Order o") java.math.BigDecimal sumTotals();
}
