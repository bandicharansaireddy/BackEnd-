package com.freshmart.api.commerce.order;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;
public interface OrderRepository extends JpaRepository<Order,Long>{
 @EntityGraph(attributePaths={"items","items.product"}) Page<Order> findAllByUser_Id(Long userId,Pageable pageable);
 @EntityGraph(attributePaths={"items","items.product"}) Optional<Order> findByIdAndUser_Id(Long id,Long userId);
}
