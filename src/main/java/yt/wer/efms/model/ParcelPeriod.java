package yt.wer.efms.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "parcel_periods", uniqueConstraints = @UniqueConstraint(columnNames = { "parcel_id", "period_id" }))
public class ParcelPeriod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcel_id", nullable = false)
    private Parcel parcel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id")
    private Period period;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "culture_code_id")
    private CultureCode cultureCode;
    @Column(name = "active")
    private Boolean active;

    @Column(name = "start_validity")
    private LocalDateTime startValidity;

    @Column(name = "end_validity")
    private LocalDateTime endValidity;

    @Column(name = "culture_label")
    private String cultureLabel;

    @Column(name = "variety")
    private String variety;

    @Column(name = "declared_area_ha")
    private Double declaredAreaHa;

    @Column(name = "measured_area_ha")
    private Double measuredAreaHa;

    @Column(name = "target_yield_tha")
    private Double targetYieldTha;

    @Column(name = "sowing_density_kgha")
    private Double sowingDensityKgha;

    @Column(name = "row_spacing_cm")
    private Double rowSpacingCm;

    @Column(name = "sowing_date")
    private LocalDate sowingDate;

    @Column(name = "harvest_date")
    private LocalDate harvestDate;

    @Column(name = "yield_realized_tha")
    private Double yieldRealizedTha;

    @Column(name = "campaign_year")
    private Integer campaignYear;

    @Column(name = "eligibility_status")
    private String eligibilityStatus;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forced_period_id")
    private Period forcedPeriod;

    @Column(name = "period_name_override")
    private String periodNameOverride;

    @Column(name = "period_start_override")
    private LocalDateTime periodStartOverride;

    @Column(name = "period_end_override")
    private LocalDateTime periodEndOverride;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Parcel getParcel() {
        return parcel;
    }

    public void setParcel(Parcel parcel) {
        this.parcel = parcel;
    }

    public Period getPeriod() {
        return period;
    }

    public void setPeriod(Period period) {
        this.period = period;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public CultureCode getCultureCode() {
        return cultureCode;
    }

    public void setCultureCode(CultureCode cultureCode) {
        this.cultureCode = cultureCode;
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

    public Period getForcedPeriod() {
        return forcedPeriod;
    }

    public void setForcedPeriod(Period forcedPeriod) {
        this.forcedPeriod = forcedPeriod;
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
