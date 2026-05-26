package yt.wer.efms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.FarmOperationTypeDefaultTool;

import java.util.List;
import java.util.Optional;

@Repository
public interface FarmOperationTypeDefaultToolRepository extends JpaRepository<FarmOperationTypeDefaultTool, Long> {
    Optional<FarmOperationTypeDefaultTool> findByFarmIdAndOperationTypeId(Long farmId, Long operationTypeId);
    List<FarmOperationTypeDefaultTool> findByFarmId(Long farmId);
    void deleteByFarmIdAndOperationTypeId(Long farmId, Long operationTypeId);
}
