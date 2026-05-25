package yt.wer.efms.dto;

public class ParcelShareRequest {
    private String username;
    private String role;
    private Boolean includeChildren;

    public ParcelShareRequest() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Boolean getIncludeChildren() { return includeChildren; }
    public void setIncludeChildren(Boolean includeChildren) { this.includeChildren = includeChildren; }
}
