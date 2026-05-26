package yt.wer.efms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private Set<FarmUser> farmUsers = new HashSet<>();

    // authentication fields
    @Column(unique = true)
    private String username;

    @JsonIgnore
    private String password;

    @OneToMany(mappedBy = "owner")
    @JsonIgnore
    private Set<Farm> ownedFarms = new HashSet<>();

    @Column(name = "email")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "tutorial_state", nullable = false)
    private TutorialState tutorialState = TutorialState.NOT_STARTED;

    @Column(name = "operations_popup_top_right", nullable = false)
    private boolean operationsPopupTopRight = false;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "is_admin", nullable = false, columnDefinition = "boolean default false")
    private boolean isAdmin = false;

    @Column(name = "verified", nullable = false, columnDefinition = "boolean default false")
    private boolean verified = false;

    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "time_format", length = 8)
    private String timeFormat;

    @Column(name = "date_format", length = 16)
    private String dateFormat;

    @Column(name = "default_farm_id")
    private Long defaultFarmId;

    @Column(name = "email_notifications_enabled", nullable = false, columnDefinition = "boolean default true")
    private boolean emailNotificationsEnabled = true;

    @Column(name = "preferred_language", length = 8)
    private String preferredLanguage;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(LocalDateTime modifiedAt) { this.modifiedAt = modifiedAt; }

    public Set<FarmUser> getFarmUsers() { return farmUsers; }
    public void setFarmUsers(Set<FarmUser> farmUsers) { this.farmUsers = farmUsers; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Set<Farm> getOwnedFarms() { return ownedFarms; }
    public void setOwnedFarms(Set<Farm> ownedFarms) { this.ownedFarms = ownedFarms; }

    public TutorialState getTutorialState() { return tutorialState; }
    public void setTutorialState(TutorialState tutorialState) { this.tutorialState = tutorialState; }

    public boolean isOperationsPopupTopRight() { return operationsPopupTopRight; }
    public void setOperationsPopupTopRight(boolean operationsPopupTopRight) { this.operationsPopupTopRight = operationsPopupTopRight; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { isAdmin = admin; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public String getVerificationToken() { return verificationToken; }
    public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }

    public String getTimeFormat() { return timeFormat; }
    public void setTimeFormat(String timeFormat) { this.timeFormat = timeFormat; }

    public String getDateFormat() { return dateFormat; }
    public void setDateFormat(String dateFormat) { this.dateFormat = dateFormat; }

    public Long getDefaultFarmId() { return defaultFarmId; }
    public void setDefaultFarmId(Long defaultFarmId) { this.defaultFarmId = defaultFarmId; }

    public boolean isEmailNotificationsEnabled() { return emailNotificationsEnabled; }
    public void setEmailNotificationsEnabled(boolean emailNotificationsEnabled) { this.emailNotificationsEnabled = emailNotificationsEnabled; }

    public String getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }

    @PrePersist
    public void prePersist() {
        if (tutorialState == null) {
            tutorialState = TutorialState.NOT_STARTED;
        }
    }
}
