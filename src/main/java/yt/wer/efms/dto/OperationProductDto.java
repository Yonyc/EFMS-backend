package yt.wer.efms.dto;

import java.time.LocalDateTime;

public class OperationProductDto {
    private Long id;
    private Double quantity;
    private Long productId;
    private String productName;
    private Long unitId;
    private String unitValue;
    private Long toolId;
    private String toolName;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;

    public OperationProductDto() {}

    public OperationProductDto(Long id, Double quantity, Long productId, String productName, Long unitId, String unitValue,
                               Long toolId, String toolName, LocalDateTime createdAt, LocalDateTime modifiedAt) {
        this.id = id;
        this.quantity = quantity;
        this.productId = productId;
        this.productName = productName;
        this.unitId = unitId;
        this.unitValue = unitValue;
        this.toolId = toolId;
        this.toolName = toolName;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }

    public String getUnitValue() { return unitValue; }
    public void setUnitValue(String unitValue) { this.unitValue = unitValue; }

    public Long getToolId() { return toolId; }
    public void setToolId(Long toolId) { this.toolId = toolId; }

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(LocalDateTime modifiedAt) { this.modifiedAt = modifiedAt; }
}
