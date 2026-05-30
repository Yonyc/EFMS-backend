package yt.wer.efms.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ParcelDto {
    private Long id;
    private String name;
    private Boolean active;
    private LocalDateTime startValidity;
    private LocalDateTime endValidity;
    private String geodata;
    private String color;
    private String cultureColor;
    private Long farmId;
    private String status;
    private Long importRecordId;
    private Long sourceFileId;
    private Long periodId;
    private List<Long> periodIds;
    private List<ParcelPeriodSummaryDto> parcelPeriods;
    private Boolean canEdit;
    private Boolean canShare;
    private Long parentParcelId;

    private String sourceName;
    private String sourceCode;
    private String sourceBlockCode;
    private String exploitantCode;
    private String exploitantName;
    private String municipality;
    private String cadastralRef;
    private String sourceGuid;
    private String validationNotes;

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

    public String getCultureColor() { return cultureColor; }
    public void setCultureColor(String cultureColor) { this.cultureColor = cultureColor; }

    public Long getFarmId() { return farmId; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getImportRecordId() { return importRecordId; }
    public void setImportRecordId(Long importRecordId) { this.importRecordId = importRecordId; }

    public Long getSourceFileId() { return sourceFileId; }
    public void setSourceFileId(Long sourceFileId) { this.sourceFileId = sourceFileId; }

    public String getValidationNotes() { return validationNotes; }
    public void setValidationNotes(String validationNotes) { this.validationNotes = validationNotes; }

    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }

    public List<Long> getPeriodIds() { return periodIds; }
    public void setPeriodIds(List<Long> periodIds) { this.periodIds = periodIds; }

    public List<ParcelPeriodSummaryDto> getParcelPeriods() { return parcelPeriods; }
    public void setParcelPeriods(List<ParcelPeriodSummaryDto> parcelPeriods) { this.parcelPeriods = parcelPeriods; }

    public Boolean getCanEdit() { return canEdit; }
    public void setCanEdit(Boolean canEdit) { this.canEdit = canEdit; }

    public Boolean getCanShare() { return canShare; }
    public void setCanShare(Boolean canShare) { this.canShare = canShare; }

    public Long getParentParcelId() { return parentParcelId; }
    public void setParentParcelId(Long parentParcelId) { this.parentParcelId = parentParcelId; }

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }

    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }

    public String getSourceBlockCode() { return sourceBlockCode; }
    public void setSourceBlockCode(String sourceBlockCode) { this.sourceBlockCode = sourceBlockCode; }

    public String getExploitantCode() { return exploitantCode; }
    public void setExploitantCode(String exploitantCode) { this.exploitantCode = exploitantCode; }

    public String getExploitantName() { return exploitantName; }
    public void setExploitantName(String exploitantName) { this.exploitantName = exploitantName; }

    public String getMunicipality() { return municipality; }
    public void setMunicipality(String municipality) { this.municipality = municipality; }

    public String getCadastralRef() { return cadastralRef; }
    public void setCadastralRef(String cadastralRef) { this.cadastralRef = cadastralRef; }

    public String getSourceGuid() { return sourceGuid; }
    public void setSourceGuid(String sourceGuid) { this.sourceGuid = sourceGuid; }
}
