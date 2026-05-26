package yt.wer.efms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.ToolCategory;

@Repository
public interface ToolCategoryRepository extends JpaRepository<ToolCategory, Long> {
}
