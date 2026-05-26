package yt.wer.efms.dto;

public class ProductInput {
    private String name;
    private Long productTypeId;
    private Long unitId;
    private Long defaultOperationTypeId;
    private Long overrideToolId;
    private String description;
    private String pictureUrl;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getProductTypeId() { return productTypeId; }
    public void setProductTypeId(Long productTypeId) { this.productTypeId = productTypeId; }

    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }

    public Long getDefaultOperationTypeId() { return defaultOperationTypeId; }
    public void setDefaultOperationTypeId(Long defaultOperationTypeId) { this.defaultOperationTypeId = defaultOperationTypeId; }

    public Long getOverrideToolId() { return overrideToolId; }
    public void setOverrideToolId(Long overrideToolId) { this.overrideToolId = overrideToolId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPictureUrl() { return pictureUrl; }
    public void setPictureUrl(String pictureUrl) { this.pictureUrl = pictureUrl; }
}
