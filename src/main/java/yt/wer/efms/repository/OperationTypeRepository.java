package yt.wer.efms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.OperationType;

@Repository
public interface OperationTypeRepository extends JpaRepository<OperationType, Long> {
}
