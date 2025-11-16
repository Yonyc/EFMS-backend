package yt.wer.efms.dto;

import yt.wer.efms.model.ValidationStatus;

import java.time.LocalDateTime;

public class ImportedParcelDto {
    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime date;
    private String geodataWkt; // WKT representation for easy consumption
    private ValidationStatus validationStatus;
    private String validationNotes;
    private Long importRecordId;
    private Long convertedParcelId;

    public ImportedParcelDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public String getGeodataWkt() { return geodataWkt; }
    public void setGeodataWkt(String geodataWkt) { this.geodataWkt = geodataWkt; }

    public ValidationStatus getValidationStatus() { return validationStatus; }
    public void setValidationStatus(ValidationStatus validationStatus) { this.validationStatus = validationStatus; }

    public String getValidationNotes() { return validationNotes; }
    public void setValidationNotes(String validationNotes) { this.validationNotes = validationNotes; }

    public Long getImportRecordId() { return importRecordId; }
    public void setImportRecordId(Long importRecordId) { this.importRecordId = importRecordId; }

    public Long getConvertedParcelId() { return convertedParcelId; }
    public void setConvertedParcelId(Long convertedParcelId) { this.convertedParcelId = convertedParcelId; }
}
