package yt.wer.efms.dto;

import java.time.LocalDateTime;

public class ProductDto {
    private Long id;
    private String name;
    private Long productTypeId;
    private Long unitId;
    private Long farmId;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;

    public ProductDto() {}

    public ProductDto(Long id, String name, Long productTypeId, Long unitId, Long farmId, LocalDateTime createdAt, LocalDateTime modifiedAt) {
        this.id = id;
        this.name = name;
        this.productTypeId = productTypeId;
        this.unitId = unitId;
        this.farmId = farmId;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getProductTypeId() { return productTypeId; }
    public void setProductTypeId(Long productTypeId) { this.productTypeId = productTypeId; }

    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }

    public Long getFarmId() { return farmId; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(LocalDateTime modifiedAt) { this.modifiedAt = modifiedAt; }
}
