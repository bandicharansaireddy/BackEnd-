package com.freshmart.api.commerce.cart;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface CartRepository extends JpaRepository<Cart,Long>{
 @EntityGraph(attributePaths={"items","items.product","items.product.category"})
 @Query("select c from Cart c where c.user.id=:userId") Optional<Cart> findWithItemsByUserId(@Param("userId") Long userId);
 Optional<Cart> findByUser_Id(Long userId);
}
