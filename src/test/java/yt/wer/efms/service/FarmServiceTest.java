package yt.wer.efms.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import yt.wer.efms.model.Farm;
import yt.wer.efms.model.Parcel;
import yt.wer.efms.model.ParcelOperation;
import yt.wer.efms.model.User;
import yt.wer.efms.repository.*;
import yt.wer.efms.service.EmailService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FarmServiceTest {


    @Mock private FarmRepository farmRepository;
    @Mock private yt.wer.efms.repository.SystemSettingsRepository systemSettingsRepository;
    @Mock private ParcelRepository parcelRepository;
    @Mock private UserRepository userRepository;
    @Mock private PeriodRepository periodRepository;
    @Mock private ParcelShareRepository parcelShareRepository;
    @Mock private ResearchZoneShareRepository researchZoneShareRepository;
    @Mock private ResearchZoneShareClaimRepository researchZoneShareClaimRepository;
    @Mock private PermissionService permissionService;
    @Mock private FarmUserRepository farmUserRepository;
    @Mock private ToolRepository toolRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ParcelOperationRepository parcelOperationRepository;
    @Mock private OperationTypeRepository operationTypeRepository;
    @Mock private yt.wer.efms.repository.ParcelPeriodRepository parcelPeriodRepository;
    @Mock private yt.wer.efms.repository.CultureCodeRepository cultureCodeRepository;
    @Mock private yt.wer.efms.repository.CultureTypeRepository cultureTypeRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private FarmService farmService;

    @Captor private ArgumentCaptor<Farm> farmCaptor;
    @Captor private ArgumentCaptor<Parcel> parcelCaptor;


    @Test
    void deleteSetsDeletedAtOnFarm() {
        User owner = user(1L, "arnaud");
        Farm farm = farm(10L, owner);

        when(permissionService.currentUserId()).thenReturn(1L);
        when(permissionService.isCurrentUserAdmin()).thenReturn(false);
        when(permissionService.isOwner(farm, 1L)).thenReturn(true);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(parcelRepository.findByFarmId(10L)).thenReturn(Collections.emptyList());
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        farmService.delete(10L);

        verify(farmRepository).save(farmCaptor.capture());
        assertNotNull(farmCaptor.getValue().getDeletedAt());
        assertEquals(owner, farmCaptor.getValue().getDeletedBy());
    }

    @Test
    void deleteDoesNothingWhenFarmAlreadyDeleted() {
        Farm farm = farm(10L, user(1L, "arnaud"));
        farm.setDeletedAt(LocalDateTime.now());

        when(permissionService.currentUserId()).thenReturn(1L);
        when(permissionService.isCurrentUserAdmin()).thenReturn(false);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));

        farmService.delete(10L);

        verify(farmRepository, never()).save(any());
    }

    @Test
    void deleteDoesNothingWhenFarmNotFound() {
        when(permissionService.currentUserId()).thenReturn(1L);
        when(permissionService.isCurrentUserAdmin()).thenReturn(false);
        when(farmRepository.findById(99L)).thenReturn(Optional.empty());

        farmService.delete(99L);

        verify(farmRepository, never()).save(any());
    }

    @Test
    void deleteThrowsForbiddenWhenCallerIsNotOwnerAndNotAdmin() {
        User owner = user(100L, "owner");
        Farm farm = farm(10L, owner);

        when(permissionService.currentUserId()).thenReturn(999L);
        when(permissionService.isCurrentUserAdmin()).thenReturn(false);
        when(permissionService.isOwner(farm, 999L)).thenReturn(false);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));

        assertThrows(ResponseStatusException.class, () -> farmService.delete(10L));
        verify(farmRepository, never()).save(any());
    }

    @Test
    void deleteAdminCanDeleteFarmTheyDontOwn() {
        User owner = user(100L, "owner");
        Farm farm = farm(10L, owner);
        User admin = user(1L, "admin");

        when(permissionService.currentUserId()).thenReturn(1L);
        when(permissionService.isCurrentUserAdmin()).thenReturn(true);
        when(permissionService.isOwner(farm, 1L)).thenReturn(false);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(parcelRepository.findByFarmId(10L)).thenReturn(Collections.emptyList());
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        farmService.delete(10L);

        verify(farmRepository).save(farmCaptor.capture());
        assertNotNull(farmCaptor.getValue().getDeletedAt());
    }

    @Test
    void deleteCascadesSoftDeleteToParcelsAndTheirOperations() {
        User owner = user(1L, "arnaud");
        Farm farm = farm(10L, owner);

        ParcelOperation op = new ParcelOperation();
        op.setId(77L);
        Set<ParcelOperation> ops = new HashSet<>();
        ops.add(op);

        Parcel parcel = new Parcel();
        parcel.setId(55L);
        parcel.setParcelOperations(ops);

        when(permissionService.currentUserId()).thenReturn(1L);
        when(permissionService.isCurrentUserAdmin()).thenReturn(false);
        when(permissionService.isOwner(farm, 1L)).thenReturn(true);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(parcelRepository.findByFarmId(10L)).thenReturn(List.of(parcel));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        farmService.delete(10L);

        verify(parcelRepository).save(argThat(p -> p.getDeletedAt() != null));
        verify(parcelOperationRepository).save(argThat(o -> o.getDeletedAt() != null));
    }


    @Test
    void restoreThrowsForbiddenWhenNotAdmin() {
        when(permissionService.isCurrentUserAdmin()).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> farmService.restore(10L));
        verify(farmRepository, never()).save(any());
    }

    @Test
    void restoreThrowsWhenFarmNotFound() {
        when(permissionService.isCurrentUserAdmin()).thenReturn(true);
        when(farmRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> farmService.restore(99L));
    }

    @Test
    void restoreThrowsBadRequestWhenFarmNotDeleted() {
        Farm farm = farm(10L, user(1L, "owner"));

        when(permissionService.isCurrentUserAdmin()).thenReturn(true);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));

        assertThrows(ResponseStatusException.class, () -> farmService.restore(10L));
    }

    @Test
    void restoreClearsDeletedAtOnFarmAndCascadeParcels() {
        LocalDateTime ts = LocalDateTime.now().minusHours(1);
        Farm farm = farm(10L, user(1L, "owner"));
        farm.setDeletedAt(ts);

        Parcel cascadeParcel = new Parcel();
        cascadeParcel.setId(55L);
        cascadeParcel.setDeletedAt(ts);

        when(permissionService.isCurrentUserAdmin()).thenReturn(true);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(parcelRepository.findByFarmIdAndDeletedAt(10L, ts)).thenReturn(List.of(cascadeParcel));
        when(parcelOperationRepository.findByFarmIdAndDeletedAt(10L, ts)).thenReturn(Collections.emptyList());

        farmService.restore(10L);

        verify(farmRepository).save(farmCaptor.capture());
        assertNull(farmCaptor.getValue().getDeletedAt());
        assertNull(farmCaptor.getValue().getDeletedBy());

        verify(parcelRepository).save(parcelCaptor.capture());
        assertNull(parcelCaptor.getValue().getDeletedAt());
    }


    @Test
    void deleteParcelReturnsFalseWhenParcelNotFound() {
        when(parcelRepository.findById(99L)).thenReturn(Optional.empty());

        assertFalse(farmService.deleteParcel(99L));
    }

    @Test
    void deleteParcelReturnsFalseWhenAlreadyDeleted() {
        Parcel parcel = new Parcel();
        parcel.setId(1L);
        parcel.setDeletedAt(LocalDateTime.now());

        when(parcelRepository.findById(1L)).thenReturn(Optional.of(parcel));

        assertFalse(farmService.deleteParcel(1L));
    }

    @Test
    void deleteParcelThrowsForbiddenWhenNoEditPermission() {
        Farm farm = farm(10L, user(100L, "owner"));
        Parcel parcel = new Parcel();
        parcel.setId(55L);
        parcel.setFarm(farm);

        when(parcelRepository.findById(55L)).thenReturn(Optional.of(parcel));
        when(permissionService.currentUserId()).thenReturn(999L);
        when(permissionService.isCurrentUserAdmin()).thenReturn(false);
        when(permissionService.canEditFarm(10L, 999L, false)).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> farmService.deleteParcel(55L));
        verify(parcelRepository, never()).save(any());
    }

    @Test
    void deleteParcelSetsDeletedAt() {
        User actor = user(1L, "arnaud");
        Farm farm = farm(10L, actor);
        farm.setEnableParcelAlerts(false);

        Parcel parcel = new Parcel();
        parcel.setId(55L);
        parcel.setFarm(farm);

        when(parcelRepository.findById(55L)).thenReturn(Optional.of(parcel));
        when(permissionService.currentUserId()).thenReturn(1L);
        when(permissionService.isCurrentUserAdmin()).thenReturn(false);
        when(permissionService.canEditFarm(10L, 1L, false)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));

        boolean result = farmService.deleteParcel(55L);

        assertTrue(result);
        verify(parcelRepository).save(parcelCaptor.capture());
        assertNotNull(parcelCaptor.getValue().getDeletedAt());
        assertEquals(actor, parcelCaptor.getValue().getDeletedBy());
    }


    private static User user(Long id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        return u;
    }

    private static Farm farm(Long id, User owner) {
        Farm f = new Farm();
        f.setId(id);
        f.setOwner(owner);
        return f;
    }
}
