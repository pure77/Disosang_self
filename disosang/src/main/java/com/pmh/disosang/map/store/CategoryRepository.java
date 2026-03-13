package com.pmh.disosang.map.store;

import com.pmh.disosang.map.store.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("""
        SELECT c.id
        FROM Category c
        WHERE c.name = :keyword
    """)
    List<Long> findCategoryIdsByExactName(@Param("keyword") String keyword);

    @Query("""
        SELECT c.id
        FROM Category c
        WHERE c.name LIKE %:keyword%
    """)
    List<Long> findCategoryIdsByKeyword(@Param("keyword") String keyword);

    @Query("""
        SELECT c.id
        FROM Category c
        WHERE c.id = :rootId
           OR c.parent.id = :rootId
    """)
    List<Long> findCategoryAndChildren(@Param("rootId") Long rootId);

    @Query(value = """
    WITH RECURSIVE CategoryHierarchy AS (
        SELECT id, parent_id
        FROM category
        WHERE id = :rootId

        UNION ALL

        SELECT c.id, c.parent_id
        FROM category c
        INNER JOIN CategoryHierarchy ch ON c.parent_id = ch.id
    )
    SELECT id FROM CategoryHierarchy
    """, nativeQuery = true)
    List<Long> findAllDescendantIds(@Param("rootId") Long rootId);
}
