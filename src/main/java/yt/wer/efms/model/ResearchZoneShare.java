package yt.wer.efms.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "research_zone_shares")
public class ResearchZoneShare {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm")
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "\"user\"")
    private User user;

    @Column(name = "zone_wkt", nullable = false, columnDefinition = "TEXT")
    private String zoneWkt;

    @Column(name = "share_token", nullable = false, unique = true, length = 96)
    private String shareToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period")
    private Period period;

    @Column(name = "period_ids", columnDefinition = "TEXT")
    private String periodIds;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tool")
    private Tool tool;

    @Column(name = "tool_ids", columnDefinition = "TEXT")
    private String toolIds;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product")
    private Product product;

    @Column(name = "product_ids", columnDefinition = "TEXT")
    private String productIds;

    @Column(name = "filter_start_date")
    private LocalDate filterStartDate;

    @Column(name = "filter_end_date")
    private LocalDate filterEndDate;

    @Column(name = "share_start_at")
    private LocalDateTime shareStartAt;

    @Column(name = "share_end_at")
    private LocalDateTime shareEndAt;

    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Farm getFarm() {
        return farm;
    }

    public void setFarm(Farm farm) {
        this.farm = farm;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getZoneWkt() {
        return zoneWkt;
    }

    public void setZoneWkt(String zoneWkt) {
        this.zoneWkt = zoneWkt;
    }

    public String getShareToken() {
        return shareToken;
    }

    public void setShareToken(String shareToken) {
        this.shareToken = shareToken;
    }

    public Period getPeriod() {
        return period;
    }

    public void setPeriod(Period period) {
        this.period = period;
    }

    public String getPeriodIds() {
        return periodIds;
    }

    public void setPeriodIds(String periodIds) {
        this.periodIds = periodIds;
    }

    public Tool getTool() {
        return tool;
    }

    public void setTool(Tool tool) {
        this.tool = tool;
    }

    public String getToolIds() {
        return toolIds;
    }

    public void setToolIds(String toolIds) {
        this.toolIds = toolIds;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getProductIds() {
        return productIds;
    }

    public void setProductIds(String productIds) {
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(LocalDateTime modifiedAt) {
        this.modifiedAt = modifiedAt;
    }
}
