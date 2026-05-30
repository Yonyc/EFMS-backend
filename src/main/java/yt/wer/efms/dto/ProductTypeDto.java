package yt.wer.efms.dto;

public class ProductTypeDto {
    private Long id;
    private String name;
    private Long unitId;
    private Long farmId;
    private boolean seedType;

    public ProductTypeDto() {}

    public ProductTypeDto(Long id, String name, Long unitId) {
        this.id = id;
        this.name = name;
        this.unitId = unitId;
    }

    public ProductTypeDto(Long id, String name, Long unitId, Long farmId) {
        this.id = id;
        this.name = name;
        this.unitId = unitId;
        this.farmId = farmId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }

    public Long getFarmId() { return farmId; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }

    public boolean isSeedType() { return seedType; }
    public void setSeedType(boolean seedType) { this.seedType = seedType; }
}
