package yt.wer.efms.model;

import jakarta.persistence.*;
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

    private Boolean active;

    @Column(name = "start_validity")
    private LocalDateTime startValidity;

    @Column(name = "end_validity")
    private LocalDateTime endValidity;

    // PostGIS geometry column
    @Column(columnDefinition = "geometry")
    private Geometry geodata;

    private String color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corresponding_pac")
    private ImportedParcel correspondingPac;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_parcel")
    private Parcel parentParcel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm")
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period")
    private Period period;

    @ManyToMany(mappedBy = "parcels")
    private Set<ParcelOperation> parcelOperations = new HashSet<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(LocalDateTime modifiedAt) { this.modifiedAt = modifiedAt; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getStartValidity() { return startValidity; }
    public void setStartValidity(LocalDateTime startValidity) { this.startValidity = startValidity; }

    public LocalDateTime getEndValidity() { return endValidity; }
    public void setEndValidity(LocalDateTime endValidity) { this.endValidity = endValidity; }

    public Geometry getGeodata() { return geodata; }
    public void setGeodata(Geometry geodata) { this.geodata = geodata; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public ImportedParcel getCorrespondingPac() { return correspondingPac; }
    public void setCorrespondingPac(ImportedParcel correspondingPac) { this.correspondingPac = correspondingPac; }

    public Parcel getParentParcel() { return parentParcel; }
    public void setParentParcel(Parcel parentParcel) { this.parentParcel = parentParcel; }

    public Farm getFarm() { return farm; }
    public void setFarm(Farm farm) { this.farm = farm; }

    public Period getPeriod() { return period; }
    public void setPeriod(Period period) { this.period = period; }

    public Set<ParcelOperation> getParcelOperations() { return parcelOperations; }
    public void setParcelOperations(Set<ParcelOperation> parcelOperations) { this.parcelOperations = parcelOperations; }
}
