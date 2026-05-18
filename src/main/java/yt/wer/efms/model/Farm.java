package yt.wer.efms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "farms")
public class Farm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String location;

    @Column(name = "is_public")
    private Boolean isPublic = false;

    // field-level visibility preferences
    @Column(name = "show_name")
    private Boolean showName = true;

    @Column(name = "show_description")
    private Boolean showDescription = true;

    @Column(name = "show_location")
    private Boolean showLocation = true;

    @Column(name = "approved", nullable = false, columnDefinition = "boolean default true")
    private boolean approved = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    @JsonIgnore
    private User deletedBy;

    @OneToMany(mappedBy = "farm")
    @JsonIgnore
    private Set<Parcel> parcels = new HashSet<>();

    @OneToMany(mappedBy = "farm")
    @JsonIgnore
    private Set<Tool> tools = new HashSet<>();

    @OneToMany(mappedBy = "farm")
    @JsonIgnore
    private Set<Product> products = new HashSet<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(LocalDateTime modifiedAt) { this.modifiedAt = modifiedAt; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }

    public Boolean getShowName() { return showName; }
    public void setShowName(Boolean showName) { this.showName = showName; }

    public Boolean getShowDescription() { return showDescription; }
    public void setShowDescription(Boolean showDescription) { this.showDescription = showDescription; }

    public Boolean getShowLocation() { return showLocation; }
    public void setShowLocation(Boolean showLocation) { this.showLocation = showLocation; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public User getDeletedBy() { return deletedBy; }
    public void setDeletedBy(User deletedBy) { this.deletedBy = deletedBy; }

    public Set<Parcel> getParcels() { return parcels; }
    public void setParcels(Set<Parcel> parcels) { this.parcels = parcels; }

    public Set<Tool> getTools() { return tools; }
    public void setTools(Set<Tool> tools) { this.tools = tools; }

    public Set<Product> getProducts() { return products; }
    public void setProducts(Set<Product> products) { this.products = products; }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner")
    @JsonIgnore
    private User owner;

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    @Column(name = "enable_member_alerts", nullable = false, columnDefinition = "boolean default false")
    private boolean enableMemberAlerts = false;

    @Column(name = "enable_parcel_alerts", nullable = false, columnDefinition = "boolean default false")
    private boolean enableParcelAlerts = false;

    @Column(name = "enable_operation_alerts", nullable = false, columnDefinition = "boolean default false")
    private boolean enableOperationAlerts = false;

    @Column(name = "alert_recipient_email")
    private String alertRecipientEmail;

    public boolean isEnableMemberAlerts() { return enableMemberAlerts; }
    public void setEnableMemberAlerts(boolean enableMemberAlerts) { this.enableMemberAlerts = enableMemberAlerts; }

    public boolean isEnableParcelAlerts() { return enableParcelAlerts; }
    public void setEnableParcelAlerts(boolean enableParcelAlerts) { this.enableParcelAlerts = enableParcelAlerts; }

    public boolean isEnableOperationAlerts() { return enableOperationAlerts; }
    public void setEnableOperationAlerts(boolean enableOperationAlerts) { this.enableOperationAlerts = enableOperationAlerts; }

    public String getAlertRecipientEmail() { return alertRecipientEmail; }
    public void setAlertRecipientEmail(String alertRecipientEmail) { this.alertRecipientEmail = alertRecipientEmail; }
}
