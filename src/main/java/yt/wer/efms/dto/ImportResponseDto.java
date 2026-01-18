package yt.wer.efms.dto;

import java.time.LocalDateTime;

public class ImportResponseDto {
    private Long importId;
    private String filename;
    private String name;
    private int parcelsImported;
    private LocalDateTime createdAt;
    private String message;

    public ImportResponseDto() {}

    public ImportResponseDto(Long importId, String filename, String name, int parcelsImported, LocalDateTime createdAt, String message) {
        this.importId = importId;
        this.filename = filename;
        this.name = name;
        this.parcelsImported = parcelsImported;
        this.createdAt = createdAt;
        this.message = message;
    }

    public Long getImportId() { return importId; }
    public void setImportId(Long importId) { this.importId = importId; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getParcelsImported() { return parcelsImported; }
    public void setParcelsImported(int parcelsImported) { this.parcelsImported = parcelsImported; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
