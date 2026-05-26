package yt.wer.efms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.Tool;

import java.util.List;

@Repository
public interface ToolRepository extends JpaRepository<Tool, Long> {
	List<Tool> findByFarmId(Long farmId);
	Page<Tool> findByFarmId(Long farmId, Pageable pageable);

	@Query("SELECT t FROM Tool t WHERE t.farm.id = :farmId " +
			"AND (:query IS NULL OR :query = '' OR lower(t.name) LIKE lower(concat('%', :query, '%'))) " +
			"AND (:categoryId IS NULL OR t.category.id = :categoryId)")
	Page<Tool> searchByFarm(@Param("farmId") Long farmId,
							@Param("query") String query,
							@Param("categoryId") Long categoryId,
							Pageable pageable);
}
