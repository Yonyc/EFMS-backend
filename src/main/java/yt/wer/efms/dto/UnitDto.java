package yt.wer.efms.dto;

public class UnitDto {
    private Long id;
    private String value;
    private Long farmId;

    public UnitDto() {}

    public UnitDto(Long id, String value) {
        this.id = id;
        this.value = value;
    }

    public UnitDto(Long id, String value, Long farmId) {
        this.id = id;
        this.value = value;
        this.farmId = farmId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public Long getFarmId() { return farmId; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }
}
