package yt.wer.efms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "farms")
public class Farm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    private String name;

    @OneToMany(mappedBy = "farm")
    private Set<Parcel> parcels = new HashSet<>();

    @OneToMany(mappedBy = "farm")
    private Set<Tool> tools = new HashSet<>();

    @OneToMany(mappedBy = "farm")
    private Set<Product> products = new HashSet<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(LocalDateTime modifiedAt) { this.modifiedAt = modifiedAt; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Set<Parcel> getParcels() { return parcels; }
    public void setParcels(Set<Parcel> parcels) { this.parcels = parcels; }

    public Set<Tool> getTools() { return tools; }
    public void setTools(Set<Tool> tools) { this.tools = tools; }

    public Set<Product> getProducts() { return products; }
    public void setProducts(Set<Product> products) { this.products = products; }
}
