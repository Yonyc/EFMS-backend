package yt.wer.efms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tools")
public class Tool {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category")
    private ToolCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm")
    private Farm farm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(LocalDateTime modifiedAt) { this.modifiedAt = modifiedAt; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ToolCategory getCategory() { return category; }
    public void setCategory(ToolCategory category) { this.category = category; }

    public Farm getFarm() { return farm; }
    public void setFarm(Farm farm) { this.farm = farm; }
}
