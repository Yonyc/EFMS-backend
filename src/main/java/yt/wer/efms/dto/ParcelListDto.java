package yt.wer.efms.dto;

public class ParcelListDto {
    private Long id;
    private String name;
    private Boolean active;
    private String color;
    private Long farmId;
    private Long periodId;
    private String validationStatus;
    private Long convertedParcelId;

    public ParcelListDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Long getFarmId() { return farmId; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }

    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }

    public String getValidationStatus() { return validationStatus; }
    public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }

    public Long getConvertedParcelId() { return convertedParcelId; }
    public void setConvertedParcelId(Long convertedParcelId) { this.convertedParcelId = convertedParcelId; }
}
