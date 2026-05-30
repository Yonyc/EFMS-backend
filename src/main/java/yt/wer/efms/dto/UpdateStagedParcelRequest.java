package yt.wer.efms.dto;

import java.time.LocalDateTime;

public class UpdateStagedParcelRequest {
    private String geodata;
    private String validationNotes;
    private Integer campaignYear;
    private Long parentParcelId;
    private Long forcedPeriodId;
    private String periodNameOverride;
    private LocalDateTime periodStartOverride;
    private LocalDateTime periodEndOverride;

    public String getGeodata() {
        return geodata;
    }

    public void setGeodata(String geodata) {
        this.geodata = geodata;
    }

    public String getValidationNotes() {
        return validationNotes;
    }

    public void setValidationNotes(String validationNotes) {
        this.validationNotes = validationNotes;
    }

    public Integer getCampaignYear() {
        return campaignYear;
    }

    public void setCampaignYear(Integer campaignYear) {
        this.campaignYear = campaignYear;
    }

    public Long getParentParcelId() {
        return parentParcelId;
    }

    public void setParentParcelId(Long parentParcelId) {
        this.parentParcelId = parentParcelId;
    }

    public Long getForcedPeriodId() {
        return forcedPeriodId;
    }

    public void setForcedPeriodId(Long forcedPeriodId) {
        this.forcedPeriodId = forcedPeriodId;
    }

    public String getPeriodNameOverride() {
        return periodNameOverride;
    }

    public void setPeriodNameOverride(String periodNameOverride) {
        this.periodNameOverride = periodNameOverride;
    }

    public LocalDateTime getPeriodStartOverride() {
        return periodStartOverride;
    }

    public void setPeriodStartOverride(LocalDateTime periodStartOverride) {
        this.periodStartOverride = periodStartOverride;
    }

    public LocalDateTime getPeriodEndOverride() {
        return periodEndOverride;
    }

    public void setPeriodEndOverride(LocalDateTime periodEndOverride) {
        this.periodEndOverride = periodEndOverride;
    }
}
