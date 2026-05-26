package yt.wer.efms.service;

import org.springframework.security.core.context.SecurityContextHolder;
import yt.wer.efms.security.CustomUserDetails;
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

    public Long currentUserId() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof CustomUserDetails) {
                return ((CustomUserDetails) principal).getUserId();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isCurrentUserAdmin() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof CustomUserDetails) {
                return ((CustomUserDetails) principal).isAdmin();
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public Farm requireFarm(Long farmId) {
        return farmRepository.findById(farmId).orElseThrow(() -> new RuntimeException("Farm not found"));
    }

    public boolean isOwner(Farm farm, Long userId) {
        return farm.getOwner() != null && userId != null && userId.equals(farm.getOwner().getId());
    }

    public Optional<Role> getFarmRole(Long farmId, Long userId) {
        if (userId == null) return Optional.empty();
        return farmUserRepository.findByFarmIdAndUserId(farmId, userId).map(fu -> fu.getRole());
    }

    public boolean canViewFarm(Long farmId, Long userId, boolean isAdmin) {
        if (isAdmin) return true;
        Farm farm = requireFarm(farmId);
        if (isOwner(farm, userId)) return true;
        return getFarmRole(farmId, userId).isPresent();
    }

    public boolean canManageFarm(Long farmId, Long userId, boolean isAdmin) {
        if (isAdmin) return true;
        Farm farm = requireFarm(farmId);
        if (isOwner(farm, userId)) return true;
        return getFarmRole(farmId, userId)
                .map(role -> role == Role.ADMIN)
                .orElse(false);
    }

    public boolean canEditFarm(Long farmId, Long userId, boolean isAdmin) {
        if (isAdmin) return true;
        Farm farm = requireFarm(farmId);
        if (isOwner(farm, userId)) return true;
        return getFarmRole(farmId, userId)
                .map(role -> role == Role.ADMIN || role == Role.EDITOR)
                .orElse(false);
    }

    public boolean canViewParcel(Parcel parcel, Long userId, boolean isAdmin) {
        if (isAdmin) return true;
        if (parcel.getFarm() != null && canViewFarm(parcel.getFarm().getId(), userId, isAdmin)) return true;
        if (userId == null) return false;
        return findEffectiveShare(parcel, userId).isPresent();
    }

    public boolean canEditParcel(Parcel parcel, Long userId, boolean isAdmin) {
        if (isAdmin) return true;
        if (parcel.getFarm() != null && canEditFarm(parcel.getFarm().getId(), userId, isAdmin)) return true;
        if (userId == null) return false;
        return findEffectiveShare(parcel, userId)
                .map(share -> share.getRole() == ParcelShareRole.EDITOR)
                .orElse(false);
    }

    private Optional<ParcelShare> findEffectiveShare(Parcel parcel, Long userId) {
        if (parcel == null || userId == null) return Optional.empty();
        Optional<ParcelShare> direct = parcelShareRepository.findFirstByParcelIdAndUserId(parcel.getId(), userId);
        if (direct.isPresent()) return direct;
        Parcel ancestor = parcel.getParentParcel();
        int guard = 32;
        while (ancestor != null && guard-- > 0) {
            Optional<ParcelShare> share = parcelShareRepository.findFirstByParcelIdAndUserId(ancestor.getId(), userId);
            if (share.isPresent() && share.get().isIncludeChildren()) return share;
            ancestor = ancestor.getParentParcel();
        }
        return Optional.empty();
    }

    public boolean canShareParcel(Parcel parcel, Long userId, boolean isAdmin) {
        if (isAdmin) return true;
        if (parcel.getFarm() == null) return false;
        return canManageFarm(parcel.getFarm().getId(), userId, isAdmin);
    }

    public Parcel requireParcel(Long parcelId) {
        return parcelRepository.findById(parcelId).orElseThrow(() -> new RuntimeException("Parcel not found"));
    }
}
