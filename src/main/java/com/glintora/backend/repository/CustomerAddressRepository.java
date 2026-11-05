package com.glintora.backend.repository;

import com.glintora.backend.model.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {
    List<CustomerAddress> findByCustomerIdOrderByIdDesc(Long customerId);
}
