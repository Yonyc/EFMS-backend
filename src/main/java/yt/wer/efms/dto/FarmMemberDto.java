package yt.wer.efms.dto;

public class FarmMemberDto {
    private Long userId;
    private String username;
    private String role;
    private boolean owner;

    public FarmMemberDto() {}

    public FarmMemberDto(Long userId, String username, String role, boolean owner) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.owner = owner;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isOwner() { return owner; }
    public void setOwner(boolean owner) { this.owner = owner; }
}
