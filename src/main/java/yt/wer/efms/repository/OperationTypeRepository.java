package yt.wer.efms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.OperationType;

import java.util.List;

@Repository
public interface OperationTypeRepository extends JpaRepository<OperationType, Long> {
    List<OperationType> findByFarmIsNullOrderByIdAsc();
    List<OperationType> findByFarmIsNullOrFarmIdOrderByIdAsc(Long farmId);
    Page<OperationType> findByFarmIsNullOrFarmIdOrderByIdAsc(Long farmId, Pageable pageable);
    boolean existsByNameAndFarmIsNull(String name);
}
