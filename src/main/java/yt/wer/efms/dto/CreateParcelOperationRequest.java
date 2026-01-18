package yt.wer.efms.dto;

import java.time.LocalDateTime;
import java.util.List;

public class CreateParcelOperationRequest {
    private LocalDateTime date;
    private Integer durationSeconds;
    private Long typeId;
    private List<OperationProductInput> products;

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public Long getTypeId() { return typeId; }
    public void setTypeId(Long typeId) { this.typeId = typeId; }

    public List<OperationProductInput> getProducts() { return products; }
    public void setProducts(List<OperationProductInput> products) { this.products = products; }
}
