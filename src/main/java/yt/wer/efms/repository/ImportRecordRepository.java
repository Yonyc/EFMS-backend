package yt.wer.efms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.ImportRecord;

import java.util.List;

@Repository
public interface ImportRecordRepository extends JpaRepository<ImportRecord, Long> {
    List<ImportRecord> findByUserUsernameOrderByCreatedAtDesc(String username);
}
