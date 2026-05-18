package yt.wer.efms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "parcel_operations")
public class ParcelOperation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    private LocalDateTime date;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type")
    private OperationType type;

    @ManyToMany
    @JoinTable(
        name = "parcels_parcel_operations",
        joinColumns = @JoinColumn(name = "parcel_operations_parcels"),
        inverseJoinColumns = @JoinColumn(name = "parcels_id")
    )
    private Set<Parcel> parcels = new HashSet<>();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private yt.wer.efms.model.User deletedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private yt.wer.efms.model.User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private yt.wer.efms.model.User updatedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(LocalDateTime modifiedAt) { this.modifiedAt = modifiedAt; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public OperationType getType() { return type; }
    public void setType(OperationType type) { this.type = type; }

    public Set<Parcel> getParcels() { return parcels; }
    public void setParcels(Set<Parcel> parcels) { this.parcels = parcels; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public yt.wer.efms.model.User getDeletedBy() { return deletedBy; }
    public void setDeletedBy(yt.wer.efms.model.User deletedBy) { this.deletedBy = deletedBy; }

    public yt.wer.efms.model.User getCreatedBy() { return createdBy; }
    public void setCreatedBy(yt.wer.efms.model.User createdBy) { this.createdBy = createdBy; }

    public yt.wer.efms.model.User getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(yt.wer.efms.model.User updatedBy) { this.updatedBy = updatedBy; }
}
