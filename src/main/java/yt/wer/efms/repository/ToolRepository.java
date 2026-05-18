package yt.wer.efms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.Tool;

import java.util.List;

@Repository
public interface ToolRepository extends JpaRepository<Tool, Long> {
	List<Tool> findByFarmId(Long farmId);
	Page<Tool> findByFarmId(Long farmId, Pageable pageable);
}
