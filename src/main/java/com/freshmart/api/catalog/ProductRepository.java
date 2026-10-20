package com.freshmart.api.catalog;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
public interface ProductRepository extends JpaRepository<Product,Long>{
 @Query("select p from Product p join p.category c where p.active=true and (:category is null or c.slug=:category) and (:search is null or lower(p.name) like lower(concat('%',:search,'%')) or lower(p.description) like lower(concat('%',:search,'%'))) and (:minPrice is null or p.priceInr>=:minPrice) and (:maxPrice is null or p.priceInr<=:maxPrice)")
 Page<Product> search(@Param("category") String category,@Param("search") String search,@Param("minPrice") BigDecimal minPrice,@Param("maxPrice") BigDecimal maxPrice,Pageable pageable);
 Page<Product> findByActiveTrueAndCategory_Slug(String slug,Pageable pageable);
 java.util.Optional<Product> findByIdAndActiveTrue(Long id);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select p from Product p where p.id = :id and p.active = true")
 java.util.Optional<Product> findActiveByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
}
