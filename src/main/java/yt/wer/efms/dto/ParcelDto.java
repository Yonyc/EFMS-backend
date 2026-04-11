package yt.wer.efms.dto;

import java.time.LocalDateTime;

public class ParcelDto {
    private Long id;
    private String name;
    private Boolean active;
    private LocalDateTime startValidity;
    private LocalDateTime endValidity;
    private String geodata;
    private String color;
    private Long farmId;
    private Long correspondingPacId;
    private Long periodId;
    private Boolean canEdit;
    private Boolean canShare;
    private Long parentParcelId;

    public ParcelDto() {}

    public ParcelDto(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public ParcelDto(Long id, String name, Boolean active, LocalDateTime startValidity, 
                     LocalDateTime endValidity, String geodata, String color, Long farmId) {
        this.id = id;
        this.name = name;
        this.active = active;
        this.startValidity = startValidity;
        this.endValidity = endValidity;
        this.geodata = geodata;
        this.color = color;
        this.farmId = farmId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public Long getFarmId() { return farmId; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }

    public Long getCorrespondingPacId() { return correspondingPacId; }
    public void setCorrespondingPacId(Long correspondingPacId) { this.correspondingPacId = correspondingPacId; }

    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }

    public Boolean getCanEdit() { return canEdit; }
    public void setCanEdit(Boolean canEdit) { this.canEdit = canEdit; }

    public Boolean getCanShare() { return canShare; }
    public void setCanShare(Boolean canShare) { this.canShare = canShare; }

    public Long getParentParcelId() { return parentParcelId; }
    public void setParentParcelId(Long parentParcelId) { this.parentParcelId = parentParcelId; }
}
