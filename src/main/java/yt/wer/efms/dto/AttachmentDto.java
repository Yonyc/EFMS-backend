package yt.wer.efms.dto;

import java.time.LocalDateTime;

public class AttachmentDto {
    private Long id;
    private String originalFilename;
    private String url;
    private String mimeType;
    private Long fileSize;
    private LocalDateTime createdAt;

    public AttachmentDto() {}

    public AttachmentDto(Long id, String originalFilename, String url, String mimeType, Long fileSize, LocalDateTime createdAt) {
        this.id = id;
        this.originalFilename = originalFilename;
        this.url = url;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
