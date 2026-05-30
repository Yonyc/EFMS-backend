package yt.wer.efms.dto;

/**
 * A single parcel share enriched with its parcel id/name, so the whole set of shares for a farm can
 * be returned in one response instead of one request per parcel.
 */
public class FarmParcelShareDto {
    private Long parcelId;
    private String parcelName;
    private Long userId;
    private String username;
    private String role;
    private boolean includeChildren;

    public FarmParcelShareDto() {}

    public FarmParcelShareDto(Long parcelId, String parcelName, Long userId, String username, String role,
            boolean includeChildren) {
        this.parcelId = parcelId;
        this.parcelName = parcelName;
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.includeChildren = includeChildren;
    }

    public Long getParcelId() { return parcelId; }
    public void setParcelId(Long parcelId) { this.parcelId = parcelId; }

    public String getParcelName() { return parcelName; }
    public void setParcelName(String parcelName) { this.parcelName = parcelName; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isIncludeChildren() { return includeChildren; }
    public void setIncludeChildren(boolean includeChildren) { this.includeChildren = includeChildren; }
}
