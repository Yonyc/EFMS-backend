package yt.wer.efms.model;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Geometry;
import java.time.LocalDateTime;

@Entity
@Table(name = "imported_parcels")
public class ImportedParcel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    private LocalDateTime date;

    // PostGIS geometry column
    @Column(columnDefinition = "geometry")
    private Geometry geodata;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status")
    private ValidationStatus validationStatus = ValidationStatus.PENDING;

    @Column(name = "validation_notes")
    private String validationNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_id")
    private ImportRecord importRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcel")
    private Parcel parcel;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(LocalDateTime modifiedAt) { this.modifiedAt = modifiedAt; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public Geometry getGeodata() { return geodata; }
    public void setGeodata(Geometry geodata) { this.geodata = geodata; }

    public ValidationStatus getValidationStatus() { return validationStatus; }
    public void setValidationStatus(ValidationStatus validationStatus) { this.validationStatus = validationStatus; }

    public String getValidationNotes() { return validationNotes; }
    public void setValidationNotes(String validationNotes) { this.validationNotes = validationNotes; }

    public ImportRecord getImportRecord() { return importRecord; }
    public void setImportRecord(ImportRecord importRecord) { this.importRecord = importRecord; }

    public Parcel getParcel() { return parcel; }
    public void setParcel(Parcel parcel) { this.parcel = parcel; }
}
