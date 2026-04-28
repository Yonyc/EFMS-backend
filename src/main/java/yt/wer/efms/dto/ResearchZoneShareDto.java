package yt.wer.efms.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ResearchZoneShareDto {
    private Long id;
    private Long farmId;
    private Long userId;
    private String username;
    private String shareToken;
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
    private Long claimedUsers;
    private List<String> accessUsernames;
    private LocalDateTime createdAt;

    public ResearchZoneShareDto() {
    }

    public ResearchZoneShareDto(Long id,
                                Long farmId,
                                Long userId,
                                String username,
                                String shareToken,
                                String zoneWkt,
                                Long periodId,
                                List<Long> periodIds,
                                Long toolId,
                                List<Long> toolIds,
                                Long productId,
                                List<Long> productIds,
                                LocalDate filterStartDate,
                                LocalDate filterEndDate,
                                LocalDateTime shareStartAt,
                                LocalDateTime shareEndAt,
                                Integer maxUsers,
                                Long claimedUsers,
                                List<String> accessUsernames,
                                LocalDateTime createdAt) {
        this.id = id;
        this.farmId = farmId;
        this.userId = userId;
        this.username = username;
        this.shareToken = shareToken;
        this.zoneWkt = zoneWkt;
        this.periodId = periodId;
        this.periodIds = periodIds;
        this.toolId = toolId;
        this.toolIds = toolIds;
        this.productId = productId;
        this.productIds = productIds;
        this.filterStartDate = filterStartDate;
        this.filterEndDate = filterEndDate;
        this.shareStartAt = shareStartAt;
        this.shareEndAt = shareEndAt;
        this.maxUsers = maxUsers;
        this.claimedUsers = claimedUsers;
        this.accessUsernames = accessUsernames;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFarmId() {
        return farmId;
    }

    public void setFarmId(Long farmId) {
        this.farmId = farmId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getShareToken() {
        return shareToken;
    }

    public void setShareToken(String shareToken) {
        this.shareToken = shareToken;
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

    public Long getClaimedUsers() {
        return claimedUsers;
    }

    public void setClaimedUsers(Long claimedUsers) {
        this.claimedUsers = claimedUsers;
    }

    public List<String> getAccessUsernames() {
        return accessUsernames;
    }

    public void setAccessUsernames(List<String> accessUsernames) {
        this.accessUsernames = accessUsernames;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
