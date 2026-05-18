package yt.wer.efms.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import yt.wer.efms.model.Farm;
import yt.wer.efms.model.FarmUser;
import yt.wer.efms.model.Parcel;
import yt.wer.efms.model.ParcelShare;
import yt.wer.efms.model.ParcelShareRole;
import yt.wer.efms.model.Role;
import yt.wer.efms.model.User;
import yt.wer.efms.repository.FarmRepository;
import yt.wer.efms.repository.FarmUserRepository;
import yt.wer.efms.repository.ParcelRepository;
import yt.wer.efms.repository.ParcelShareRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private FarmUserRepository farmUserRepository;

    @Mock
    private ParcelShareRepository parcelShareRepository;

    @Mock
    private ParcelRepository parcelRepository;

    @InjectMocks
    private PermissionService permissionService;

    @AfterEach
    void cleanSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentUsernameReturnsNameFromSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("arnaud", "pwd"));

        assertEquals("arnaud", permissionService.currentUsername());
    }

    @Test
    void currentUsernameReturnsNullWithoutSecurityContext() {
        assertNull(permissionService.currentUsername());
    }

    @Test
    void canManageFarmIsTrueForOwner() {
        Farm farm = farm(1L, 811L);
        when(farmRepository.findById(1L)).thenReturn(Optional.of(farm));

        assertTrue(permissionService.canManageFarm(1L, 811L, false));
    }

    @Test
    void canManageFarmIsTrueForAdminMember() {
        Farm farm = farm(2L, 811L);
        FarmUser farmUser = new FarmUser();
        farmUser.setRole(Role.ADMIN);

        when(farmRepository.findById(2L)).thenReturn(Optional.of(farm));
        when(farmUserRepository.findByFarmIdAndUserId(2L, 310L)).thenReturn(Optional.of(farmUser));

        assertTrue(permissionService.canManageFarm(2L, 310L, false));
    }

    @Test
    void canEditFarmIsTrueForEditorMember() {
        Farm farm = farm(3L, 811L);
        FarmUser farmUser = new FarmUser();
        farmUser.setRole(Role.EDITOR);

        when(farmRepository.findById(3L)).thenReturn(Optional.of(farm));
        when(farmUserRepository.findByFarmIdAndUserId(3L, 192L)).thenReturn(Optional.of(farmUser));

        assertTrue(permissionService.canEditFarm(3L, 192L, false));
    }

    @Test
    void canManageFarmIsFalseForEditorMember() {
        Farm farm = farm(4L, 811L);
        FarmUser farmUser = new FarmUser();
        farmUser.setRole(Role.EDITOR);

        when(farmRepository.findById(4L)).thenReturn(Optional.of(farm));
        when(farmUserRepository.findByFarmIdAndUserId(4L, 192L)).thenReturn(Optional.of(farmUser));

        assertFalse(permissionService.canManageFarm(4L, 192L, false));
    }

    @Test
    void canViewFarmIsFalseForUnknownUser() {
        Farm farm = farm(5L, 811L);
        when(farmRepository.findById(5L)).thenReturn(Optional.of(farm));
        when(farmUserRepository.findByFarmIdAndUserId(5L, 395L)).thenReturn(Optional.empty());

        assertFalse(permissionService.canViewFarm(5L, 395L, false));
    }

    @Test
    void canViewParcelIsTrueWhenShared() {
        Parcel parcel = parcel(11L, null);
        ParcelShare share = new ParcelShare();
        share.setRole(ParcelShareRole.VIEWER);

        when(parcelShareRepository.findFirstByParcelIdAndUserId(11L, 82L)).thenReturn(Optional.of(share));

        assertTrue(permissionService.canViewParcel(parcel, 82L, false));
    }

    @Test
    void canEditParcelIsTrueForSharedEditor() {
        Parcel parcel = parcel(12L, null);
        ParcelShare share = new ParcelShare();
        share.setRole(ParcelShareRole.EDITOR);

        when(parcelShareRepository.findFirstByParcelIdAndUserId(12L, 192L)).thenReturn(Optional.of(share));

        assertTrue(permissionService.canEditParcel(parcel, 192L, false));
    }

    @Test
    void canEditParcelIsFalseForSharedViewer() {
        Parcel parcel = parcel(14L, null);
        ParcelShare share = new ParcelShare();
        share.setRole(ParcelShareRole.VIEWER);

        when(parcelShareRepository.findFirstByParcelIdAndUserId(14L, 82L)).thenReturn(Optional.of(share));

        assertFalse(permissionService.canEditParcel(parcel, 82L, false));
    }

    @Test
    void canShareParcelIsFalseWhenParcelHasNoFarm() {
        Parcel parcel = parcel(15L, null);

        assertFalse(permissionService.canShareParcel(parcel, 811L, false));
    }

    @Test
    void canShareParcelIsTrueForFarmOwner() {
        Farm farm = farm(16L, 811L);
        Parcel parcel = parcel(16L, farm);
        when(farmRepository.findById(16L)).thenReturn(Optional.of(farm));

        assertTrue(permissionService.canShareParcel(parcel, 811L, false));
    }

    @Test
    void canEditParcelUsesFarmRoleWhenParcelHasFarm() {
        Farm farm = farm(17L, 811L);
        Parcel parcel = parcel(17L, farm);
        FarmUser farmUser = new FarmUser();
        farmUser.setRole(Role.EDITOR);

        when(farmRepository.findById(17L)).thenReturn(Optional.of(farm));
        when(farmUserRepository.findByFarmIdAndUserId(17L, 192L)).thenReturn(Optional.of(farmUser));

        assertTrue(permissionService.canEditParcel(parcel, 192L, false));
    }

    @Test
    void getFarmRoleReturnsEmptyWhenUsernameIsNull() {
        assertTrue(permissionService.getFarmRole(1L, (Long) null).isEmpty());
    }

    @Test
    void canViewParcelIsFalseForAnonymousWithoutFarmOrShare() {
        Parcel parcel = parcel(13L, null);

        assertFalse(permissionService.canViewParcel(parcel, null, false));
    }

    @Test
    void requireFarmThrowsWhenFarmNotFound() {
        when(farmRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> permissionService.requireFarm(99L));
        assertEquals("Farm not found", exception.getMessage());
    }

    @Test
    void requireParcelThrowsWhenParcelNotFound() {
        when(parcelRepository.findById(77L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> permissionService.requireParcel(77L));
        assertEquals("Parcel not found", exception.getMessage());
    }

    @Test
    void requireParcelReturnsParcelWhenFound() {
        Parcel parcel = parcel(88L, null);
        when(parcelRepository.findById(88L)).thenReturn(Optional.of(parcel));

        Parcel result = permissionService.requireParcel(88L);
        assertEquals(88L, result.getId());
    }

    private static Farm farm(Long id, Long ownerId) {
        User owner = new User();
        owner.setId(ownerId);

        Farm farm = new Farm();
        farm.setId(id);
        farm.setOwner(owner);
        return farm;
    }

    private static Parcel parcel(Long id, Farm farm) {
        Parcel parcel = new Parcel();
        parcel.setId(id);
        parcel.setFarm(farm);
        return parcel;
    }
}
