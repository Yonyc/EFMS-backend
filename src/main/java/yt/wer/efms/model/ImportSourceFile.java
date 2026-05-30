package yt.wer.efms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "import_source_files")
public class ImportSourceFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_id", nullable = false)
    private ImportRecord importRecord;

    private String filename;

    @Column(name = "imported_at")
    private LocalDateTime importedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ImportRecord getImportRecord() { return importRecord; }
    public void setImportRecord(ImportRecord importRecord) { this.importRecord = importRecord; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public LocalDateTime getImportedAt() { return importedAt; }
    public void setImportedAt(LocalDateTime importedAt) { this.importedAt = importedAt; }
}
