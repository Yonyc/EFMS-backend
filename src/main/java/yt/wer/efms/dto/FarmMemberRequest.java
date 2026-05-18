package yt.wer.efms.dto;

public class FarmMemberRequest {
    private String username;
    private Long userId;
    private String role;

    public FarmMemberRequest() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
