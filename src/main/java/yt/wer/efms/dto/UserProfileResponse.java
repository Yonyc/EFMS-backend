package yt.wer.efms.dto;

import yt.wer.efms.model.TutorialState;
import yt.wer.efms.model.User;

public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private TutorialState tutorialState;
    private boolean operationsPopupTopRight;
    private String avatarUrl;
    private boolean admin;
    private String timeFormat;
    private String dateFormat;
    private Long defaultFarmId;
    private boolean emailNotificationsEnabled;
    private String preferredLanguage;

    public UserProfileResponse() {}

    public UserProfileResponse(Long id, String username, String email, TutorialState tutorialState, boolean operationsPopupTopRight, String avatarUrl, boolean admin) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.tutorialState = tutorialState;
        this.operationsPopupTopRight = operationsPopupTopRight;
        this.avatarUrl = avatarUrl;
        this.admin = admin;
    }

    public static UserProfileResponse fromUser(User user) {
        UserProfileResponse dto = new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getTutorialState(),
                user.isOperationsPopupTopRight(),
                user.getAvatarUrl(),
                user.isAdmin()
        );
        dto.timeFormat = user.getTimeFormat();
        dto.dateFormat = user.getDateFormat();
        dto.defaultFarmId = user.getDefaultFarmId();
        dto.emailNotificationsEnabled = user.isEmailNotificationsEnabled();
        dto.preferredLanguage = user.getPreferredLanguage();
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public TutorialState getTutorialState() { return tutorialState; }
    public void setTutorialState(TutorialState tutorialState) { this.tutorialState = tutorialState; }

    public boolean isOperationsPopupTopRight() { return operationsPopupTopRight; }
    public void setOperationsPopupTopRight(boolean operationsPopupTopRight) { this.operationsPopupTopRight = operationsPopupTopRight; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }

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
}
