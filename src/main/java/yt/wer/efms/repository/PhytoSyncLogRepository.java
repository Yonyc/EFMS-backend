package yt.wer.efms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.PhytoSyncLog;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PhytoSyncLogRepository extends JpaRepository<PhytoSyncLog, Long> {
    Optional<PhytoSyncLog> findByFileName(String fileName);
    List<PhytoSyncLog> findAllByOrderByProcessedAtDesc();
    boolean existsByFileNameAndStatus(String fileName, String status);
}
