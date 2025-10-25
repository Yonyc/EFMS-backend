package yt.wer.efms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.ParcelOperation;

@Repository
public interface ParcelOperationRepository extends JpaRepository<ParcelOperation, Long> {
}
