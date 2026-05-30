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
    @JoinTable(name = "parcels_parcel_operations", joinColumns = @JoinColumn(name = "parcel_operations_parcels"), inverseJoinColumns = @JoinColumn(name = "parcels_id"))
    private Set<Parcel> parcels = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcel_period_id")
    private ParcelPeriod parcelPeriod;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private User deletedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_id")
    private ImportRecord importRecord;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ParcelStatus status = ParcelStatus.LIVE;

    @Column(name = "source_parcel_guids", columnDefinition = "TEXT")
    private String sourceParcelGuids;

    @Column(name = "source_operation_name")
    private String sourceOperationName;

    @Column(name = "source_operation_category")
    private String sourceOperationCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_file_id")
    private ImportSourceFile sourceFile;

    @Column(name = "source_tool_guid")
    private String sourceToolGuid;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(LocalDateTime modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public OperationType getType() {
        return type;
    }

    public void setType(OperationType type) {
        this.type = type;
    }

    public Set<Parcel> getParcels() {
        return parcels;
    }

    public void setParcels(Set<Parcel> parcels) {
        this.parcels = parcels;
    }

    public ParcelPeriod getParcelPeriod() {
        return parcelPeriod;
    }

    public void setParcelPeriod(ParcelPeriod parcelPeriod) {
        this.parcelPeriod = parcelPeriod;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public User getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(User deletedBy) {
        this.deletedBy = deletedBy;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public User getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(User updatedBy) {
        this.updatedBy = updatedBy;
    }

    public ImportRecord getImportRecord() {
        return importRecord;
    }

    public void setImportRecord(ImportRecord importRecord) {
        this.importRecord = importRecord;
    }

    public ParcelStatus getStatus() {
        return status;
    }

    public void setStatus(ParcelStatus status) {
        this.status = status;
    }

    public String getSourceParcelGuids() {
        return sourceParcelGuids;
    }

    public void setSourceParcelGuids(String sourceParcelGuids) {
        this.sourceParcelGuids = sourceParcelGuids;
    }

    public String getSourceOperationName() {
        return sourceOperationName;
    }

    public void setSourceOperationName(String sourceOperationName) {
        this.sourceOperationName = sourceOperationName;
    }

    public String getSourceOperationCategory() {
        return sourceOperationCategory;
    }

    public void setSourceOperationCategory(String sourceOperationCategory) {
        this.sourceOperationCategory = sourceOperationCategory;
    }

    public ImportSourceFile getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(ImportSourceFile sourceFile) {
        this.sourceFile = sourceFile;
    }

    public String getSourceToolGuid() {
        return sourceToolGuid;
    }

    public void setSourceToolGuid(String sourceToolGuid) {
        this.sourceToolGuid = sourceToolGuid;
    }
}
