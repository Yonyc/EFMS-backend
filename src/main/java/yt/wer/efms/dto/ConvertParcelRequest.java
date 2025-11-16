package yt.wer.efms.dto;

public class ConvertParcelRequest {
    private Long farmId;
    private String parcelName;
    private String color;

    public ConvertParcelRequest() {}

    public Long getFarmId() { return farmId; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }

    public String getParcelName() { return parcelName; }
    public void setParcelName(String parcelName) { this.parcelName = parcelName; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
