package yt.wer.efms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @OneToMany(mappedBy = "user")
    private Set<FarmUser> farmUsers = new HashSet<>();

    // authentication fields
    @Column(unique = true)
    private String username;

    private String password;

    @OneToMany(mappedBy = "owner")
    private Set<Farm> ownedFarms = new HashSet<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(LocalDateTime modifiedAt) { this.modifiedAt = modifiedAt; }

    public Set<FarmUser> getFarmUsers() { return farmUsers; }
    public void setFarmUsers(Set<FarmUser> farmUsers) { this.farmUsers = farmUsers; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Set<Farm> getOwnedFarms() { return ownedFarms; }
    public void setOwnedFarms(Set<Farm> ownedFarms) { this.ownedFarms = ownedFarms; }
}
