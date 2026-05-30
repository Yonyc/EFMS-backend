package yt.wer.efms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.ImportSourceFile;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImportSourceFileRepository extends JpaRepository<ImportSourceFile, Long> {
    List<ImportSourceFile> findByImportRecordIdOrderByImportedAtAsc(Long importRecordId);
    Optional<ImportSourceFile> findByIdAndImportRecordId(Long id, Long importRecordId);
    void deleteByImportRecordId(Long importRecordId);
}
