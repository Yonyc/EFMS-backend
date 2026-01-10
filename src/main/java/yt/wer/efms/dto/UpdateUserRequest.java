package yt.wer.efms.dto;

public class UpdateUserRequest {
    private String email;
    private Boolean operationsPopupTopRight;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Boolean getOperationsPopupTopRight() { return operationsPopupTopRight; }
    public void setOperationsPopupTopRight(Boolean operationsPopupTopRight) { this.operationsPopupTopRight = operationsPopupTopRight; }
}
