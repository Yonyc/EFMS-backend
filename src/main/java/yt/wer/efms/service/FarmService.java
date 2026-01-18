package yt.wer.efms.service;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yt.wer.efms.dto.CreateParcelRequest;
import yt.wer.efms.dto.FarmDto;
import yt.wer.efms.dto.ParcelDto;
import yt.wer.efms.model.Farm;
import yt.wer.efms.model.ImportedParcel;
import yt.wer.efms.model.Parcel;
import yt.wer.efms.repository.FarmRepository;
import yt.wer.efms.repository.ImportedParcelRepository;
import yt.wer.efms.repository.ParcelRepository;
import yt.wer.efms.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class FarmService {
    private final FarmRepository farmRepository;
    private final ParcelRepository parcelRepository;
    private final UserRepository userRepository;
    private final ImportedParcelRepository importedParcelRepository;
    private final WKTReader wktReader = new WKTReader();
    private final WKTWriter wktWriter = new WKTWriter();

    public FarmService(FarmRepository farmRepository, ParcelRepository parcelRepository, 
                       UserRepository userRepository, ImportedParcelRepository importedParcelRepository) {
        this.farmRepository = farmRepository;
        this.parcelRepository = parcelRepository;
        this.userRepository = userRepository;
        this.importedParcelRepository = importedParcelRepository;
    }

    public List<FarmDto> listAll() {
        return farmRepository.findAll().stream()
                .map(f -> new FarmDto(f.getId(), f.getName(), f.getCreatedAt(), f.getModifiedAt()))
                .collect(Collectors.toList());
    }

    public List<FarmDto> listUserFarms(String username) {
        return farmRepository.findByOwnerUsername(username).stream()
                .map(f -> new FarmDto(f.getId(), f.getName(), f.getCreatedAt(), f.getModifiedAt()))
                .collect(Collectors.toList());
    }

    public Optional<FarmDto> findById(Long id) {
        return farmRepository.findById(id).map(f -> new FarmDto(f.getId(), f.getName(), f.getCreatedAt(), f.getModifiedAt()));
    }

    public FarmDto create(String name) {
        // Require authentication
        final String username;
        try {
            username = SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            throw new RuntimeException("Authentication required to create a farm");
        }

        if (username == null || username.equals("anonymousUser")) {
            throw new RuntimeException("Authentication required to create a farm");
        }

        Farm farm = new Farm();
        farm.setName(name);
        farm.setCreatedAt(LocalDateTime.now());
        farm.setModifiedAt(LocalDateTime.now());

        // Set owner
        userRepository.findAll().stream()
                .filter(u -> username.equals(u.getUsername()))
                .findFirst()
                .ifPresent(farm::setOwner);

        Farm saved = farmRepository.save(farm);
        return new FarmDto(saved.getId(), saved.getName(), saved.getCreatedAt(), saved.getModifiedAt());
    }

    public Optional<FarmDto> update(Long id, String name) {
        return farmRepository.findById(id).map(f -> {
            f.setName(name);
            f.setModifiedAt(LocalDateTime.now());
            Farm s = farmRepository.save(f);
            return new FarmDto(s.getId(), s.getName(), s.getCreatedAt(), s.getModifiedAt());
        });
    }

    public void delete(Long id) {
        // only allow deletion if current user is owner or has admin role
        String username = null;
        try { username = SecurityContextHolder.getContext().getAuthentication().getName(); } catch (Exception ignored) {}

        Farm farm = farmRepository.findById(id).orElse(null);
        if (farm == null) return;
        if (farm.getOwner() != null && username != null && username.equals(farm.getOwner().getUsername())) {
            farmRepository.deleteById(id);
        }
        // otherwise ignore or throw - for now we silently ignore if not owner
    }

    public boolean deleteParcel(Long parcelId) {
        String username = null;
        try { username = SecurityContextHolder.getContext().getAuthentication().getName(); } catch (Exception ignored) {}

        Optional<Parcel> parcelOpt = parcelRepository.findById(parcelId);
        if (parcelOpt.isEmpty()) {
            return false;
        }

        Parcel parcel = parcelOpt.get();
        Farm farm = parcel.getFarm();
        if (farm != null && farm.getOwner() != null && username != null && !username.equals(farm.getOwner().getUsername())) {
            throw new RuntimeException("You can only delete parcels from your own farms");
        }

        ImportedParcel importedParcel = parcel.getCorrespondingPac();
        if (importedParcel != null) {
            importedParcel.setParcel(null);
            importedParcel.setModifiedAt(LocalDateTime.now());
            importedParcelRepository.save(importedParcel);
        }

        parcelRepository.delete(parcel);
        return true;
    }

    public List<ParcelDto> listParcels(Long farmId) {
        List<Parcel> parcels = parcelRepository.findByFarmId(farmId);
        return parcels.stream().map(this::toParcelDto).collect(Collectors.toList());
    }

    public ParcelDto createParcel(Long farmId, CreateParcelRequest request) {
        // Verify farm exists and user has permission
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new RuntimeException("Farm not found"));

        String username = null;
        try { 
            username = SecurityContextHolder.getContext().getAuthentication().getName(); 
        } catch (Exception ignored) {}

        // Check if user owns the farm
        if (farm.getOwner() != null && username != null && !username.equals(farm.getOwner().getUsername())) {
            throw new RuntimeException("You can only add parcels to your own farms");
        }

        Parcel parcel = new Parcel();
        parcel.setName(request.getName());
        parcel.setFarm(farm);
        parcel.setActive(request.getActive() != null ? request.getActive() : true);
        parcel.setStartValidity(request.getStartValidity() != null ? request.getStartValidity() : LocalDateTime.now());
        parcel.setEndValidity(request.getEndValidity());
        
        // Convert WKT string to Geometry
        if (request.getGeodata() != null && !request.getGeodata().isEmpty()) {
            try {
                Geometry geom = wktReader.read(request.getGeodata());
                parcel.setGeodata(geom);
            } catch (Exception e) {
                throw new RuntimeException("Invalid WKT geometry: " + e.getMessage(), e);
            }
        }
        
        parcel.setColor(request.getColor());
        parcel.setCreatedAt(LocalDateTime.now());
        parcel.setModifiedAt(LocalDateTime.now());

        // Link to corresponding imported parcel if specified
        if (request.getCorrespondingPacId() != null) {
            importedParcelRepository.findById(request.getCorrespondingPacId())
                    .ifPresent(parcel::setCorrespondingPac);
        }

        Parcel saved = parcelRepository.save(parcel);
        return toParcelDto(saved);
    }

    public List<ParcelDto> listAllParcels() {
        return parcelRepository.findAll().stream()
                .map(this::toParcelDto)
                .collect(Collectors.toList());
    }

    public Optional<ParcelDto> findParcelById(Long parcelId) {
        return parcelRepository.findById(parcelId).map(this::toParcelDto);
    }

    public Optional<ParcelDto> updateParcel(Long farmId, Long parcelId, CreateParcelRequest request) {
        // Require authentication
        final String username;
        try {
            username = SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            throw new RuntimeException("Authentication required to update a parcel");
        }

        if (username == null || username.equals("anonymousUser")) {
            throw new RuntimeException("Authentication required to update a parcel");
        }

        // Verify farm exists
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new RuntimeException("Farm not found"));

        // Check if user owns the farm
        if (farm.getOwner() == null || !username.equals(farm.getOwner().getUsername())) {
            throw new RuntimeException("You can only update parcels in your own farms");
        }

        // Find the parcel and verify it belongs to this farm
        return parcelRepository.findById(parcelId).map(parcel -> {
            if (!parcel.getFarm().getId().equals(farmId)) {
                throw new RuntimeException("Parcel does not belong to this farm");
            }

            // Update fields
            if (request.getName() != null) {
                parcel.setName(request.getName());
            }
            if (request.getActive() != null) {
                parcel.setActive(request.getActive());
            }
            if (request.getStartValidity() != null) {
                parcel.setStartValidity(request.getStartValidity());
            }
            if (request.getEndValidity() != null) {
                parcel.setEndValidity(request.getEndValidity());
            }
            
            // Update geometry if provided
            if (request.getGeodata() != null && !request.getGeodata().isEmpty()) {
                try {
                    Geometry geom = wktReader.read(request.getGeodata());
                    parcel.setGeodata(geom);
                } catch (Exception e) {
                    throw new RuntimeException("Invalid WKT geometry: " + e.getMessage(), e);
                }
            }
            
            if (request.getColor() != null) {
                parcel.setColor(request.getColor());
            }
            
            parcel.setModifiedAt(LocalDateTime.now());

            // Update corresponding imported parcel if specified
            if (request.getCorrespondingPacId() != null) {
                importedParcelRepository.findById(request.getCorrespondingPacId())
                        .ifPresent(parcel::setCorrespondingPac);
            }

            Parcel saved = parcelRepository.save(parcel);
            return toParcelDto(saved);
        });
    }

    private ParcelDto toParcelDto(Parcel p) {
        ParcelDto dto = new ParcelDto();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setActive(p.getActive());
        dto.setStartValidity(p.getStartValidity());
        dto.setEndValidity(p.getEndValidity());
        
        // Convert Geometry to WKT string
        if (p.getGeodata() != null) {
            String wkt = wktWriter.write(p.getGeodata());
            dto.setGeodata(wkt);
        }
        
        dto.setColor(p.getColor());
        if (p.getFarm() != null) {
            dto.setFarmId(p.getFarm().getId());
        }
        if (p.getCorrespondingPac() != null) {
            dto.setCorrespondingPacId(p.getCorrespondingPac().getId());
        }
        return dto;
    }
}
