package yt.wer.efms.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import yt.wer.efms.model.Farm;
import yt.wer.efms.model.Parcel;
import yt.wer.efms.model.ParcelShare;
import yt.wer.efms.model.ParcelShareRole;
import yt.wer.efms.model.Role;
import yt.wer.efms.repository.FarmRepository;
import yt.wer.efms.repository.FarmUserRepository;
import yt.wer.efms.repository.ParcelRepository;
import yt.wer.efms.repository.ParcelShareRepository;

import java.util.Optional;

@Service
public class PermissionService {
    private final FarmRepository farmRepository;
    private final FarmUserRepository farmUserRepository;
    private final ParcelShareRepository parcelShareRepository;
    private final ParcelRepository parcelRepository;

    public PermissionService(FarmRepository farmRepository,
                             FarmUserRepository farmUserRepository,
                             ParcelShareRepository parcelShareRepository,
                             ParcelRepository parcelRepository) {
        this.farmRepository = farmRepository;
        this.farmUserRepository = farmUserRepository;
        this.parcelShareRepository = parcelShareRepository;
        this.parcelRepository = parcelRepository;
    }

    public String currentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return null;
        }
    }

    public Farm requireFarm(Long farmId) {
        return farmRepository.findById(farmId).orElseThrow(() -> new RuntimeException("Farm not found"));
    }

    public boolean isOwner(Farm farm, String username) {
        return farm.getOwner() != null && username != null && username.equals(farm.getOwner().getUsername());
    }

    public Optional<Role> getFarmRole(Long farmId, String username) {
        if (username == null) return Optional.empty();
        return farmUserRepository.findByFarmIdAndUserUsername(farmId, username).map(fu -> fu.getRole());
    }

    public boolean canViewFarm(Long farmId, String username) {
        Farm farm = requireFarm(farmId);
        if (isOwner(farm, username)) return true;
        return getFarmRole(farmId, username).isPresent();
    }

    public boolean canManageFarm(Long farmId, String username) {
        Farm farm = requireFarm(farmId);
        if (isOwner(farm, username)) return true;
        return getFarmRole(farmId, username)
                .map(role -> role == Role.ADMIN)
                .orElse(false);
    }

    public boolean canEditFarm(Long farmId, String username) {
        Farm farm = requireFarm(farmId);
        if (isOwner(farm, username)) return true;
        return getFarmRole(farmId, username)
                .map(role -> role == Role.ADMIN || role == Role.EDITOR)
                .orElse(false);
    }

    public boolean canViewParcel(Parcel parcel, String username) {
        if (parcel.getFarm() != null && canViewFarm(parcel.getFarm().getId(), username)) return true;
        if (username == null) return false;
        return parcelShareRepository.findByParcelIdAndUserUsername(parcel.getId(), username).isPresent();
    }

    public boolean canEditParcel(Parcel parcel, String username) {
        if (parcel.getFarm() != null && canEditFarm(parcel.getFarm().getId(), username)) return true;
        if (username == null) return false;
        return parcelShareRepository.findByParcelIdAndUserUsername(parcel.getId(), username)
                .map(share -> share.getRole() == ParcelShareRole.EDITOR)
                .orElse(false);
    }

    public boolean canShareParcel(Parcel parcel, String username) {
        if (parcel.getFarm() == null) return false;
        return canManageFarm(parcel.getFarm().getId(), username);
    }

    public Parcel requireParcel(Long parcelId) {
        return parcelRepository.findById(parcelId).orElseThrow(() -> new RuntimeException("Parcel not found"));
    }
}
