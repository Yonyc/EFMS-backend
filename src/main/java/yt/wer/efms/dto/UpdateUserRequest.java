package yt.wer.efms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public class UpdateUserRequest {
    private String email;
    private Boolean operationsPopupTopRight;
    private String timeFormat;
    private String dateFormat;
    private Long defaultFarmId;
    private Boolean emailNotificationsEnabled;
    private String preferredLanguage;

    private transient boolean defaultFarmIdProvided;
    private transient boolean preferredLanguageProvided;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Boolean getOperationsPopupTopRight() { return operationsPopupTopRight; }
    public void setOperationsPopupTopRight(Boolean operationsPopupTopRight) { this.operationsPopupTopRight = operationsPopupTopRight; }

    public String getTimeFormat() { return timeFormat; }
    public void setTimeFormat(String timeFormat) { this.timeFormat = timeFormat; }

    public String getDateFormat() { return dateFormat; }
    public void setDateFormat(String dateFormat) { this.dateFormat = dateFormat; }

    public Long getDefaultFarmId() { return defaultFarmId; }
    public void setDefaultFarmId(Long defaultFarmId) { this.defaultFarmId = defaultFarmId; this.defaultFarmIdProvided = true; }
    public boolean isDefaultFarmIdProvided() { return defaultFarmIdProvided; }

    public Boolean getEmailNotificationsEnabled() { return emailNotificationsEnabled; }
    public void setEmailNotificationsEnabled(Boolean emailNotificationsEnabled) { this.emailNotificationsEnabled = emailNotificationsEnabled; }

    public String getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; this.preferredLanguageProvided = true; }
    public boolean isPreferredLanguageProvided() { return preferredLanguageProvided; }
}
