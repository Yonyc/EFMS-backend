package yt.wer.efms.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ParcelPeriodSummaryDto {
    private Long id;
    private Long periodId;
    private String periodName;
    private Boolean active;
    private LocalDateTime startValidity;
    private LocalDateTime endValidity;
    private String cultureCode;
    private String cultureLabel;

    private String variety;
    private Double declaredAreaHa;
    private Double measuredAreaHa;
    private Double targetYieldTha;
    private Double sowingDensityKgha;
    private Double rowSpacingCm;
    private LocalDate sowingDate;
    private LocalDate harvestDate;
    private Double yieldRealizedTha;
    private Integer campaignYear;
    private String eligibilityStatus;
    private String comment;

    private Long forcedPeriodId;
    private String periodNameOverride;
    private LocalDateTime periodStartOverride;
    private LocalDateTime periodEndOverride;

    public ParcelPeriodSummaryDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPeriodId() {
        return periodId;
    }

    public void setPeriodId(Long periodId) {
        this.periodId = periodId;
    }

    public String getPeriodName() {
        return periodName;
    }

    public void setPeriodName(String periodName) {
        this.periodName = periodName;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getStartValidity() {
        return startValidity;
    }

    public void setStartValidity(LocalDateTime startValidity) {
        this.startValidity = startValidity;
    }

    public LocalDateTime getEndValidity() {
        return endValidity;
    }

    public void setEndValidity(LocalDateTime endValidity) {
        this.endValidity = endValidity;
    }

    public String getCultureCode() {
        return cultureCode;
    }

    public void setCultureCode(String cultureCode) {
        this.cultureCode = cultureCode;
    }

    public String getCultureLabel() {
        return cultureLabel;
    }

    public void setCultureLabel(String cultureLabel) {
        this.cultureLabel = cultureLabel;
    }

    public String getVariety() {
        return variety;
    }

    public void setVariety(String variety) {
        this.variety = variety;
    }

    public Double getDeclaredAreaHa() {
        return declaredAreaHa;
    }

    public void setDeclaredAreaHa(Double declaredAreaHa) {
        this.declaredAreaHa = declaredAreaHa;
    }

    public Double getMeasuredAreaHa() {
        return measuredAreaHa;
    }

    public void setMeasuredAreaHa(Double measuredAreaHa) {
        this.measuredAreaHa = measuredAreaHa;
    }

    public Double getTargetYieldTha() {
        return targetYieldTha;
    }

    public void setTargetYieldTha(Double targetYieldTha) {
        this.targetYieldTha = targetYieldTha;
    }

    public Double getSowingDensityKgha() {
        return sowingDensityKgha;
    }

    public void setSowingDensityKgha(Double sowingDensityKgha) {
        this.sowingDensityKgha = sowingDensityKgha;
    }

    public Double getRowSpacingCm() {
        return rowSpacingCm;
    }

    public void setRowSpacingCm(Double rowSpacingCm) {
        this.rowSpacingCm = rowSpacingCm;
    }

    public LocalDate getSowingDate() {
        return sowingDate;
    }

    public void setSowingDate(LocalDate sowingDate) {
        this.sowingDate = sowingDate;
    }

    public LocalDate getHarvestDate() {
        return harvestDate;
    }

    public void setHarvestDate(LocalDate harvestDate) {
        this.harvestDate = harvestDate;
    }

    public Double getYieldRealizedTha() {
        return yieldRealizedTha;
    }

    public void setYieldRealizedTha(Double yieldRealizedTha) {
        this.yieldRealizedTha = yieldRealizedTha;
    }

    public Integer getCampaignYear() {
        return campaignYear;
    }

    public void setCampaignYear(Integer campaignYear) {
        this.campaignYear = campaignYear;
    }

    public String getEligibilityStatus() {
        return eligibilityStatus;
    }

    public void setEligibilityStatus(String eligibilityStatus) {
        this.eligibilityStatus = eligibilityStatus;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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
