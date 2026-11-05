package com.glintora.backend.repository;

import com.glintora.backend.model.SalesChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SalesChannelRepository extends JpaRepository<SalesChannel, Long> {
    Optional<SalesChannel> findByNameIgnoreCase(String name);
}
