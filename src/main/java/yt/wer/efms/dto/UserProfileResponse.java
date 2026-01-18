package yt.wer.efms.dto;

import yt.wer.efms.model.TutorialState;

public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private TutorialState tutorialState;
    private boolean operationsPopupTopRight;
    private String avatarUrl;

    public UserProfileResponse() {}

    public UserProfileResponse(Long id, String username, String email, TutorialState tutorialState, boolean operationsPopupTopRight, String avatarUrl) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.tutorialState = tutorialState;
        this.operationsPopupTopRight = operationsPopupTopRight;
        this.avatarUrl = avatarUrl;
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
}
