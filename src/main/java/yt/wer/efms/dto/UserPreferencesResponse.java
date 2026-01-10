package yt.wer.efms.dto;

public class UserPreferencesResponse {
    private boolean operationsPopupTopRight;

    public UserPreferencesResponse() {}

    public UserPreferencesResponse(boolean operationsPopupTopRight) {
        this.operationsPopupTopRight = operationsPopupTopRight;
    }

    public boolean isOperationsPopupTopRight() { return operationsPopupTopRight; }
    public void setOperationsPopupTopRight(boolean operationsPopupTopRight) { this.operationsPopupTopRight = operationsPopupTopRight; }
}
