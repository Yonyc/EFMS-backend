package yt.wer.efms.dto;

import java.time.LocalDateTime;
import java.util.List;

public class CreateParcelOperationRequest {
    private LocalDateTime date;
    private Integer durationSeconds;
    private Long typeId;
    private List<OperationProductInput> products;
    private List<Long> parcelIds;
    private Long parcelPeriodId;
    private Long cultureCodeId;

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public Long getTypeId() { return typeId; }
    public void setTypeId(Long typeId) { this.typeId = typeId; }

    public List<OperationProductInput> getProducts() { return products; }
    public void setProducts(List<OperationProductInput> products) { this.products = products; }

    public List<Long> getParcelIds() { return parcelIds; }
    public void setParcelIds(List<Long> parcelIds) { this.parcelIds = parcelIds; }

    public Long getParcelPeriodId() { return parcelPeriodId; }
    public void setParcelPeriodId(Long parcelPeriodId) { this.parcelPeriodId = parcelPeriodId; }

    public Long getCultureCodeId() { return cultureCodeId; }
    public void setCultureCodeId(Long cultureCodeId) { this.cultureCodeId = cultureCodeId; }
}
