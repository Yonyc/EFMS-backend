package yt.wer.efms.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ResearchZoneShareRequest {
    private String username;
    private String zoneWkt;
    private Long periodId;
    private List<Long> periodIds;
    private Long toolId;
    private List<Long> toolIds;
    private Long productId;
    private List<Long> productIds;
    private LocalDate filterStartDate;
    private LocalDate filterEndDate;
    private LocalDateTime shareStartAt;
    private LocalDateTime shareEndAt;
    private Integer maxUsers;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getZoneWkt() {
        return zoneWkt;
    }

    public void setZoneWkt(String zoneWkt) {
        this.zoneWkt = zoneWkt;
    }

    public Long getPeriodId() {
        return periodId;
    }

    public void setPeriodId(Long periodId) {
        this.periodId = periodId;
    }

    public List<Long> getPeriodIds() {
        return periodIds;
    }

    public void setPeriodIds(List<Long> periodIds) {
        this.periodIds = periodIds;
    }

    public Long getToolId() {
        return toolId;
    }

    public void setToolId(Long toolId) {
        this.toolId = toolId;
    }

    public List<Long> getToolIds() {
        return toolIds;
    }

    public void setToolIds(List<Long> toolIds) {
        this.toolIds = toolIds;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public List<Long> getProductIds() {
        return productIds;
    }

    public void setProductIds(List<Long> productIds) {
        this.productIds = productIds;
    }

    public LocalDate getFilterStartDate() {
        return filterStartDate;
    }

    public void setFilterStartDate(LocalDate filterStartDate) {
        this.filterStartDate = filterStartDate;
    }

    public LocalDate getFilterEndDate() {
        return filterEndDate;
    }

    public void setFilterEndDate(LocalDate filterEndDate) {
        this.filterEndDate = filterEndDate;
    }

    public LocalDateTime getShareStartAt() {
        return shareStartAt;
    }

    public void setShareStartAt(LocalDateTime shareStartAt) {
        this.shareStartAt = shareStartAt;
    }

    public LocalDateTime getShareEndAt() {
        return shareEndAt;
    }

    public void setShareEndAt(LocalDateTime shareEndAt) {
        this.shareEndAt = shareEndAt;
    }

    public Integer getMaxUsers() {
        return maxUsers;
    }

    public void setMaxUsers(Integer maxUsers) {
        this.maxUsers = maxUsers;
    }
}
