package yt.wer.efms.dto;

import yt.wer.efms.model.ValidationStatus;

import java.time.LocalDateTime;

public class ImportedParcelDto {
    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime date;
    private String geodata;
    private ValidationStatus validationStatus;
    private String validationNotes;
    private Long importRecordId;
    private Long convertedParcelId;

    // Source DBF fields
    private String sourceName;
    private String sourceCode;
    private String sourceBlockCode;
    private String cultureCode;
    private String cultureLabel;
    private Double declaredAreaHa;
    private String sourceGuid;
    private Integer campaignYear;

    public ImportedParcelDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public String getGeodata() { return geodata; }
    public void setGeodata(String geodata) { this.geodata = geodata; }

    public ValidationStatus getValidationStatus() { return validationStatus; }
    public void setValidationStatus(ValidationStatus validationStatus) { this.validationStatus = validationStatus; }

    public String getValidationNotes() { return validationNotes; }
    public void setValidationNotes(String validationNotes) { this.validationNotes = validationNotes; }

    public Long getImportRecordId() { return importRecordId; }
    public void setImportRecordId(Long importRecordId) { this.importRecordId = importRecordId; }

    public Long getConvertedParcelId() { return convertedParcelId; }
    public void setConvertedParcelId(Long convertedParcelId) { this.convertedParcelId = convertedParcelId; }

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }

    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }

    public String getSourceBlockCode() { return sourceBlockCode; }
    public void setSourceBlockCode(String sourceBlockCode) { this.sourceBlockCode = sourceBlockCode; }

    public String getCultureCode() { return cultureCode; }
    public void setCultureCode(String cultureCode) { this.cultureCode = cultureCode; }

    public String getCultureLabel() { return cultureLabel; }
    public void setCultureLabel(String cultureLabel) { this.cultureLabel = cultureLabel; }

    public Double getDeclaredAreaHa() { return declaredAreaHa; }
    public void setDeclaredAreaHa(Double declaredAreaHa) { this.declaredAreaHa = declaredAreaHa; }

    public String getSourceGuid() { return sourceGuid; }
    public void setSourceGuid(String sourceGuid) { this.sourceGuid = sourceGuid; }

    public Integer getCampaignYear() { return campaignYear; }
    public void setCampaignYear(Integer campaignYear) { this.campaignYear = campaignYear; }
}
