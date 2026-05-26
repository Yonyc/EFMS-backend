package yt.wer.efms.dto;

import yt.wer.efms.model.User;

public class AuthResponse {
    private String token;
    private Long user_id;
    private yt.wer.efms.model.TutorialState tutorialState;
    private boolean operationsPopupTopRight;
    private String email;
    private String avatarUrl;
    private boolean admin;
    private String timeFormat;
    private String dateFormat;
    private Long defaultFarmId;
    private boolean emailNotificationsEnabled;
    private String preferredLanguage;

    public AuthResponse() {}

    public AuthResponse(String token, Long user_id, yt.wer.efms.model.TutorialState tutorialState, boolean operationsPopupTopRight, String email, String avatarUrl, boolean admin) {
        this.token = token;
        this.user_id = user_id;
        this.tutorialState = tutorialState;
        this.operationsPopupTopRight = operationsPopupTopRight;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.admin = admin;
    }

    public static AuthResponse fromUser(String token, User user) {
        AuthResponse dto = new AuthResponse(
                token,
                user.getId(),
                user.getTutorialState(),
                user.isOperationsPopupTopRight(),
                user.getEmail(),
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

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Long getUser_id() { return user_id; }
    public void setUser_id(Long user_id) { this.user_id = user_id; }
    public yt.wer.efms.model.TutorialState getTutorialState() { return tutorialState; }
    public void setTutorialState(yt.wer.efms.model.TutorialState tutorialState) { this.tutorialState = tutorialState; }
    public boolean isOperationsPopupTopRight() { return operationsPopupTopRight; }
    public void setOperationsPopupTopRight(boolean operationsPopupTopRight) { this.operationsPopupTopRight = operationsPopupTopRight; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

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
