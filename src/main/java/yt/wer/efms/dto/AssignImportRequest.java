package yt.wer.efms.dto;

public class AssignImportRequest {
    private Long farmId;
    private String parcelNamePrefix;
    private String defaultColor;
    private Boolean convertOnlyApproved = true;

    public Long getFarmId() {
        return farmId;
    }

    public void setFarmId(Long farmId) {
        this.farmId = farmId;
    }

    public String getParcelNamePrefix() {
        return parcelNamePrefix;
    }

    public void setParcelNamePrefix(String parcelNamePrefix) {
        this.parcelNamePrefix = parcelNamePrefix;
    }

    public String getDefaultColor() {
        return defaultColor;
    }

    public void setDefaultColor(String defaultColor) {
        this.defaultColor = defaultColor;
    }

    public Boolean getConvertOnlyApproved() {
        return convertOnlyApproved;
    }

    public void setConvertOnlyApproved(Boolean convertOnlyApproved) {
        this.convertOnlyApproved = convertOnlyApproved;
    }
}
