package yt.wer.efms.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ParcelOperationDto {
    private Long id;
    private LocalDateTime date;
    private Integer durationSeconds;
    private Long typeId;
    private String typeName;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private List<OperationProductDto> products;
    private List<AttachmentDto> attachments;
    private Long parcelId;
    private String parcelName;
    private List<Long> parcelIds;
    private List<String> parcelNames;
    private Long periodId;
    private String periodName;

    public ParcelOperationDto() {}

    public ParcelOperationDto(Long id, LocalDateTime date, Integer durationSeconds, Long typeId, String typeName,
                              LocalDateTime createdAt, LocalDateTime modifiedAt, List<OperationProductDto> products) {
        this.id = id;
        this.date = date;
        this.durationSeconds = durationSeconds;
        this.typeId = typeId;
        this.typeName = typeName;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
        this.products = products;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public Long getTypeId() { return typeId; }
    public void setTypeId(Long typeId) { this.typeId = typeId; }

    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(LocalDateTime modifiedAt) { this.modifiedAt = modifiedAt; }

    public List<OperationProductDto> getProducts() { return products; }
    public void setProducts(List<OperationProductDto> products) { this.products = products; }

    public List<AttachmentDto> getAttachments() { return attachments; }
    public void setAttachments(List<AttachmentDto> attachments) { this.attachments = attachments; }

    public Long getParcelId() { return parcelId; }
    public void setParcelId(Long parcelId) { this.parcelId = parcelId; }

    public String getParcelName() { return parcelName; }
    public void setParcelName(String parcelName) { this.parcelName = parcelName; }

    public List<Long> getParcelIds() { return parcelIds; }
    public void setParcelIds(List<Long> parcelIds) { this.parcelIds = parcelIds; }

    public List<String> getParcelNames() { return parcelNames; }
    public void setParcelNames(List<String> parcelNames) { this.parcelNames = parcelNames; }

    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }

    public String getPeriodName() { return periodName; }
    public void setPeriodName(String periodName) { this.periodName = periodName; }

    private String cultureCode;
    private String cultureLabel;
    private Long cultureCodeId;

    public String getCultureCode() { return cultureCode; }
    public void setCultureCode(String cultureCode) { this.cultureCode = cultureCode; }

    public String getCultureLabel() { return cultureLabel; }
    public void setCultureLabel(String cultureLabel) { this.cultureLabel = cultureLabel; }

    public Long getCultureCodeId() { return cultureCodeId; }
    public void setCultureCodeId(Long cultureCodeId) { this.cultureCodeId = cultureCodeId; }
}
