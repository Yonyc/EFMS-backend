package yt.wer.efms.dto;

import java.time.LocalDateTime;

public class PhytoImportResult {
    private String versionTag;
    private String filePath;
    private int created;
    private int updated;
    private int skipped;
    private int total;
    private LocalDateTime importedAt;

    public PhytoImportResult() {}

    public PhytoImportResult(String versionTag, String filePath, int created, int updated, int skipped, int total, LocalDateTime importedAt) {
        this.versionTag = versionTag;
        this.filePath = filePath;
        this.created = created;
        this.updated = updated;
        this.skipped = skipped;
        this.total = total;
        this.importedAt = importedAt;
    }

    public String getVersionTag() { return versionTag; }
    public void setVersionTag(String versionTag) { this.versionTag = versionTag; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public int getCreated() { return created; }
    public void setCreated(int created) { this.created = created; }

    public int getUpdated() { return updated; }
    public void setUpdated(int updated) { this.updated = updated; }

    public int getSkipped() { return skipped; }
    public void setSkipped(int skipped) { this.skipped = skipped; }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public LocalDateTime getImportedAt() { return importedAt; }
    public void setImportedAt(LocalDateTime importedAt) { this.importedAt = importedAt; }
}
