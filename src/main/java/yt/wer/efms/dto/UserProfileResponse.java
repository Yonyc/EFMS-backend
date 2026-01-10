package yt.wer.efms.dto;

import yt.wer.efms.model.TutorialState;

public class UserProfileResponse {
    private Long id;
    private String username;
    private TutorialState tutorialState;
    private boolean operationsPopupTopRight;

    public UserProfileResponse() {}

    public UserProfileResponse(Long id, String username, TutorialState tutorialState, boolean operationsPopupTopRight) {
        this.id = id;
        this.username = username;
        this.tutorialState = tutorialState;
        this.operationsPopupTopRight = operationsPopupTopRight;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public TutorialState getTutorialState() { return tutorialState; }
    public void setTutorialState(TutorialState tutorialState) { this.tutorialState = tutorialState; }

    public boolean isOperationsPopupTopRight() { return operationsPopupTopRight; }
    public void setOperationsPopupTopRight(boolean operationsPopupTopRight) { this.operationsPopupTopRight = operationsPopupTopRight; }
}
