package yt.wer.efms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "culture_types")
public class CultureType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Official Belgian PAC/SIGEC code, e.g. "100". */
    @Column(name = "code", nullable = false)
    private String code;

    /** Human-readable name. */
    @Column(name = "name", nullable = false)
    private String name;

    /** Optional secondary name. */
    @Column(name = "name_nl")
    private String nameNl;

    /** Grouping category for the UI. */
    @Column(name = "category")
    private String category;

    /** Default map colour for parcels of this culture. Null = no default. */
    @Column(name = "color", length = 32)
    private String color;

    /**
     * Farm-specific culture type when not null, global reference type when null.
     * Mirrors the farm/global scope pattern used by Product and OperationType.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id")
    private Farm farm;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNameNl() { return nameNl; }
    public void setNameNl(String nameNl) { this.nameNl = nameNl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Farm getFarm() { return farm; }
    public void setFarm(Farm farm) { this.farm = farm; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(LocalDateTime modifiedAt) { this.modifiedAt = modifiedAt; }
}
