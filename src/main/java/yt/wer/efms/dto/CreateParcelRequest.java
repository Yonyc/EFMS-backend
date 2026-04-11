package yt.wer.efms.dto;

import java.time.LocalDateTime;

public class CreateParcelRequest {
    private String name;
    private Boolean active;
    private LocalDateTime startValidity;
    private LocalDateTime endValidity;
    private String geodata;
    private String color;
    private Long correspondingPacId;
    private Long periodId;
    private Long parentParcelId;

    public CreateParcelRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getStartValidity() { return startValidity; }
    public void setStartValidity(LocalDateTime startValidity) { this.startValidity = startValidity; }

    public LocalDateTime getEndValidity() { return endValidity; }
    public void setEndValidity(LocalDateTime endValidity) { this.endValidity = endValidity; }

    public String getGeodata() { return geodata; }
    public void setGeodata(String geodata) { this.geodata = geodata; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Long getCorrespondingPacId() { return correspondingPacId; }
    public void setCorrespondingPacId(Long correspondingPacId) { this.correspondingPacId = correspondingPacId; }

    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }

    public Long getParentParcelId() { return parentParcelId; }
    public void setParentParcelId(Long parentParcelId) { this.parentParcelId = parentParcelId; }
}
