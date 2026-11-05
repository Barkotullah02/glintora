package com.glintora.backend.repository;

import com.glintora.backend.model.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SizeRepository extends JpaRepository<Size, Long> {
    Optional<Size> findBySizeNameIgnoreCase(String sizeName);
    List<Size> findAllByOrderByIdAsc();
}
