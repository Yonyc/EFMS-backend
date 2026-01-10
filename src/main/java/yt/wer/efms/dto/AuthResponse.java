package yt.wer.efms.dto;

public class AuthResponse {
    private String token;
    private Long user_id;
    private yt.wer.efms.model.TutorialState tutorialState;
    private boolean operationsPopupTopRight;
    private String email;
    private String avatarUrl;

    public AuthResponse() {}
    public AuthResponse(String token, Long user_id, yt.wer.efms.model.TutorialState tutorialState, boolean operationsPopupTopRight, String email, String avatarUrl) { 
        this.token = token; 
        this.user_id = user_id;
        this.tutorialState = tutorialState;
        this.operationsPopupTopRight = operationsPopupTopRight;
        this.email = email;
        this.avatarUrl = avatarUrl;
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
}
