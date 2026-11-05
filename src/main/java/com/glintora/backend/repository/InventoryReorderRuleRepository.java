package com.glintora.backend.repository;

import com.glintora.backend.model.InventoryReorderRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InventoryReorderRuleRepository extends JpaRepository<InventoryReorderRule, Long> {
    List<InventoryReorderRule> findAllByOrderByVariantIdAsc();
}
