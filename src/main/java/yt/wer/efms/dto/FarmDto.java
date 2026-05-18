package yt.wer.efms.dto;

import java.time.LocalDateTime;

public class FarmDto {
    private Long id;
    private String name;
    private String description;
    private String location;
    private Boolean isPublic;
    private Boolean showName;
    private Boolean showDescription;
    private Boolean showLocation;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private Boolean canEdit;
    private Boolean canManage;
    private Boolean enableMemberAlerts;
    private Boolean enableParcelAlerts;
    private Boolean enableOperationAlerts;
    private String alertRecipientEmail;

    public FarmDto() {}

    public FarmDto(Long id, String name, String description, String location, Boolean isPublic,
                   Boolean showName, Boolean showDescription, Boolean showLocation,
                   LocalDateTime createdAt, LocalDateTime modifiedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.location = location;
        this.isPublic = isPublic;
        this.showName = showName;
        this.showDescription = showDescription;
        this.showLocation = showLocation;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }

    public FarmDto(Long id, String name, String description, String location, Boolean isPublic,
                   Boolean showName, Boolean showDescription, Boolean showLocation,
                   LocalDateTime createdAt, LocalDateTime modifiedAt,
                   Boolean enableMemberAlerts, Boolean enableParcelAlerts, Boolean enableOperationAlerts,
                   String alertRecipientEmail) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.location = location;
        this.isPublic = isPublic;
        this.showName = showName;
        this.showDescription = showDescription;
        this.showLocation = showLocation;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
        this.enableMemberAlerts = enableMemberAlerts;
        this.enableParcelAlerts = enableParcelAlerts;
        this.enableOperationAlerts = enableOperationAlerts;
        this.alertRecipientEmail = alertRecipientEmail;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(LocalDateTime modifiedAt) { this.modifiedAt = modifiedAt; }

    public Boolean getCanEdit() { return canEdit; }
    public void setCanEdit(Boolean canEdit) { this.canEdit = canEdit; }

    public Boolean getCanManage() { return canManage; }
    public void setCanManage(Boolean canManage) { this.canManage = canManage; }

    public Boolean getEnableMemberAlerts() { return enableMemberAlerts; }
    public void setEnableMemberAlerts(Boolean enableMemberAlerts) { this.enableMemberAlerts = enableMemberAlerts; }

    public Boolean getEnableParcelAlerts() { return enableParcelAlerts; }
    public void setEnableParcelAlerts(Boolean enableParcelAlerts) { this.enableParcelAlerts = enableParcelAlerts; }

    public Boolean getEnableOperationAlerts() { return enableOperationAlerts; }
    public void setEnableOperationAlerts(Boolean enableOperationAlerts) { this.enableOperationAlerts = enableOperationAlerts; }

    public String getAlertRecipientEmail() { return alertRecipientEmail; }
    public void setAlertRecipientEmail(String alertRecipientEmail) { this.alertRecipientEmail = alertRecipientEmail; }
}
