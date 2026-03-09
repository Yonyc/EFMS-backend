package yt.wer.efms.dto;

import java.time.LocalDateTime;

public class ImportRecordDto {
    private Long id;
    private String filename;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private String username;
    private Integer totalParcels;
    private Integer pendingParcels;
    private Integer approvedParcels;
    private Integer rejectedParcels;
    private Integer convertedParcels;

    public ImportRecordDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Integer getTotalParcels() { return totalParcels; }
    public void setTotalParcels(Integer totalParcels) { this.totalParcels = totalParcels; }

    public Integer getPendingParcels() { return pendingParcels; }
    public void setPendingParcels(Integer pendingParcels) { this.pendingParcels = pendingParcels; }

    public Integer getApprovedParcels() { return approvedParcels; }
    public void setApprovedParcels(Integer approvedParcels) { this.approvedParcels = approvedParcels; }

    public Integer getRejectedParcels() { return rejectedParcels; }
    public void setRejectedParcels(Integer rejectedParcels) { this.rejectedParcels = rejectedParcels; }

    public Integer getConvertedParcels() { return convertedParcels; }
    public void setConvertedParcels(Integer convertedParcels) { this.convertedParcels = convertedParcels; }
}
