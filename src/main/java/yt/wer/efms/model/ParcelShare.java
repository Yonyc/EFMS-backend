package yt.wer.efms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "parcel_shares")
public class ParcelShare {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcel")
    private Parcel parcel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "\"user\"")
    private User user;

    @Convert(converter = ParcelShareRoleConverter.class)
    @Column(name = "role")
    private ParcelShareRole role;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "include_children", nullable = false, columnDefinition = "boolean default true")
    private boolean includeChildren = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Parcel getParcel() { return parcel; }
    public void setParcel(Parcel parcel) { this.parcel = parcel; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public ParcelShareRole getRole() { return role; }
    public void setRole(ParcelShareRole role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isIncludeChildren() { return includeChildren; }
    public void setIncludeChildren(boolean includeChildren) { this.includeChildren = includeChildren; }
}
