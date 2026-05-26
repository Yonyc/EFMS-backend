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
    private String officialAuthNumber;
    private String officialDecisionCode;
    private String officialDateFrom;
    private String officialDateTo;
    private String officialUserGroupCode;
    private String officialFormulationTypeCode;
    private String officialProductTypeCodes;
    private String officialVersionTag;

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

    public String getOfficialAuthNumber() { return officialAuthNumber; }
    public void setOfficialAuthNumber(String officialAuthNumber) { this.officialAuthNumber = officialAuthNumber; }

    public String getOfficialDecisionCode() { return officialDecisionCode; }
    public void setOfficialDecisionCode(String officialDecisionCode) { this.officialDecisionCode = officialDecisionCode; }

    public String getOfficialDateFrom() { return officialDateFrom; }
    public void setOfficialDateFrom(String officialDateFrom) { this.officialDateFrom = officialDateFrom; }

    public String getOfficialDateTo() { return officialDateTo; }
    public void setOfficialDateTo(String officialDateTo) { this.officialDateTo = officialDateTo; }

    public String getOfficialUserGroupCode() { return officialUserGroupCode; }
    public void setOfficialUserGroupCode(String officialUserGroupCode) { this.officialUserGroupCode = officialUserGroupCode; }

    public String getOfficialFormulationTypeCode() { return officialFormulationTypeCode; }
    public void setOfficialFormulationTypeCode(String officialFormulationTypeCode) { this.officialFormulationTypeCode = officialFormulationTypeCode; }

    public String getOfficialProductTypeCodes() { return officialProductTypeCodes; }
    public void setOfficialProductTypeCodes(String officialProductTypeCodes) { this.officialProductTypeCodes = officialProductTypeCodes; }

    public String getOfficialVersionTag() { return officialVersionTag; }
    public void setOfficialVersionTag(String officialVersionTag) { this.officialVersionTag = officialVersionTag; }
}
