package com.glintora.backend.repository;

import com.glintora.backend.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByOrderByNameAsc();
    List<Category> findByParentCategoryIsNullOrderByNameAsc();
    Optional<Category> findByName(String name);
}
