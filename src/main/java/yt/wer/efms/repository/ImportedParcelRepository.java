package yt.wer.efms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.ImportedParcel;
import yt.wer.efms.model.ValidationStatus;

import java.util.List;

@Repository
public interface ImportedParcelRepository extends JpaRepository<ImportedParcel, Long> {
    List<ImportedParcel> findByImportRecordId(Long importRecordId);
    List<ImportedParcel> findByImportRecordIdAndValidationStatus(Long importRecordId, ValidationStatus status);
    boolean existsByImportRecordIdAndValidationStatus(Long importRecordId, ValidationStatus status);
    void deleteByImportRecordId(Long importRecordId);
}
