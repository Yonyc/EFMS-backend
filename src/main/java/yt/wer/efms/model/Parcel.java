package yt.wer.efms.model;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import org.locationtech.jts.geom.Geometry;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "parcels")
public class Parcel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    private String name;

    @Column(columnDefinition = "geometry")
    private Geometry geodata;

    private String color;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ParcelStatus status = ParcelStatus.LIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_parcel")
    private Parcel parentParcel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm")
    private Farm farm;

    /** Geofolia GUID_PARC stable UUID for cross-import deduplication. */
    @Column(name = "source_guid")
    private String sourceGuid;

    @OneToMany(mappedBy = "parcel")
    @BatchSize(size = 200)
    private Set<ParcelPeriod> parcelPeriods = new HashSet<>();

    @ManyToMany(mappedBy = "parcels")
    private Set<ParcelOperation> parcelOperations = new HashSet<>();

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_file_id")
    private ImportSourceFile sourceFile;

    @Column(name = "validation_notes")
    private String validationNotes;

    @Column(name = "source_name")
    private String sourceName;

    @Column(name = "source_code")
    private String sourceCode;

    /** Îlot / block code. */
    @Column(name = "source_block_code")
    private String sourceBlockCode;

    @Column(name = "exploitant_code")
    private String exploitantCode;

    @Column(name = "exploitant_name")
    private String exploitantName;

    @Column(name = "municipality")
    private String municipality;

    @Column(name = "cadastral_ref")
    private String cadastralRef;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Geometry getGeodata() {
        return geodata;
    }

    public void setGeodata(Geometry geodata) {
        this.geodata = geodata;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public ParcelStatus getStatus() {
        return status;
    }

    public void setStatus(ParcelStatus status) {
        this.status = status;
    }

    public Parcel getParentParcel() {
        return parentParcel;
    }

    public void setParentParcel(Parcel parentParcel) {
        this.parentParcel = parentParcel;
    }

    public Farm getFarm() {
        return farm;
    }

    public void setFarm(Farm farm) {
        this.farm = farm;
    }

    public String getSourceGuid() {
        return sourceGuid;
    }

    public void setSourceGuid(String sourceGuid) {
        this.sourceGuid = sourceGuid;
    }

    public Set<ParcelPeriod> getParcelPeriods() {
        return parcelPeriods;
    }

    public void setParcelPeriods(Set<ParcelPeriod> parcelPeriods) {
        this.parcelPeriods = parcelPeriods;
    }

    public Set<ParcelOperation> getParcelOperations() {
        return parcelOperations;
    }

    public void setParcelOperations(Set<ParcelOperation> parcelOperations) {
        this.parcelOperations = parcelOperations;
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

    public ImportSourceFile getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(ImportSourceFile sourceFile) {
        this.sourceFile = sourceFile;
    }

    public String getValidationNotes() {
        return validationNotes;
    }

    public void setValidationNotes(String validationNotes) {
        this.validationNotes = validationNotes;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public String getSourceBlockCode() {
        return sourceBlockCode;
    }

    public void setSourceBlockCode(String sourceBlockCode) {
        this.sourceBlockCode = sourceBlockCode;
    }

    public String getExploitantCode() {
        return exploitantCode;
    }

    public void setExploitantCode(String exploitantCode) {
        this.exploitantCode = exploitantCode;
    }

    public String getExploitantName() {
        return exploitantName;
    }

    public void setExploitantName(String exploitantName) {
        this.exploitantName = exploitantName;
    }

    public String getMunicipality() {
        return municipality;
    }

    public void setMunicipality(String municipality) {
        this.municipality = municipality;
    }

    public String getCadastralRef() {
        return cadastralRef;
    }

    public void setCadastralRef(String cadastralRef) {
        this.cadastralRef = cadastralRef;
    }
}
