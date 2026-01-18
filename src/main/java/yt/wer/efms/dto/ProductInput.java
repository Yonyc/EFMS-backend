package yt.wer.efms.dto;

public class ProductInput {
    private String name;
    private Long productTypeId;
    private Long unitId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getProductTypeId() { return productTypeId; }
    public void setProductTypeId(Long productTypeId) { this.productTypeId = productTypeId; }

    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }
}
