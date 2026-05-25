package yt.wer.efms.dto;

public class ParcelShareDto {
    private Long userId;
    private String username;
    private String role;
    private boolean includeChildren;

    public ParcelShareDto() {}

    public ParcelShareDto(Long userId, String username, String role) {
        this(userId, username, role, true);
    }

    public ParcelShareDto(Long userId, String username, String role, boolean includeChildren) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.includeChildren = includeChildren;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isIncludeChildren() { return includeChildren; }
    public void setIncludeChildren(boolean includeChildren) { this.includeChildren = includeChildren; }
}
