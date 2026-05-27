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

    // Fields extracted from the source shapefile DBF
    @Column(name = "source_name")
    private String sourceName;

    @Column(name = "source_code")
    private String sourceCode;

    /** Îlot / block code (NUM_ILOT from Geofolia). Null when not provided. */
    @Column(name = "source_block_code")
    private String sourceBlockCode;

    @Column(name = "culture_code")
    private String cultureCode;

    @Column(name = "culture_label")
    private String cultureLabel;

    @Column(name = "declared_area_ha")
    private Double declaredAreaHa;

    /** Geofolia GUID_PARC — stable UUID for re-import matching. */
    @Column(name = "source_guid")
    private String sourceGuid;

    @Column(name = "campaign_year")
    private Integer campaignYear;

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

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }

    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }

    public String getSourceBlockCode() { return sourceBlockCode; }
    public void setSourceBlockCode(String sourceBlockCode) { this.sourceBlockCode = sourceBlockCode; }

    public String getCultureCode() { return cultureCode; }
    public void setCultureCode(String cultureCode) { this.cultureCode = cultureCode; }

    public String getCultureLabel() { return cultureLabel; }
    public void setCultureLabel(String cultureLabel) { this.cultureLabel = cultureLabel; }

    public Double getDeclaredAreaHa() { return declaredAreaHa; }
    public void setDeclaredAreaHa(Double declaredAreaHa) { this.declaredAreaHa = declaredAreaHa; }

    public String getSourceGuid() { return sourceGuid; }
    public void setSourceGuid(String sourceGuid) { this.sourceGuid = sourceGuid; }

    public Integer getCampaignYear() { return campaignYear; }
    public void setCampaignYear(Integer campaignYear) { this.campaignYear = campaignYear; }

    public ImportRecord getImportRecord() { return importRecord; }
    public void setImportRecord(ImportRecord importRecord) { this.importRecord = importRecord; }

    public Parcel getParcel() { return parcel; }
    public void setParcel(Parcel parcel) { this.parcel = parcel; }
}
