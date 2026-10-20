package com.freshmart.api.commerce.address;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface AddressRepository extends JpaRepository<Address,Long>{List<Address> findAllByUser_IdOrderByIdDesc(Long userId);Optional<Address> findByIdAndUser_Id(Long id,Long userId);}
