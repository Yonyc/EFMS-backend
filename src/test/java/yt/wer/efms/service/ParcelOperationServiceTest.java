package yt.wer.efms.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import yt.wer.efms.dto.CreateParcelOperationRequest;
import yt.wer.efms.dto.ParcelOperationDto;
import yt.wer.efms.model.Farm;
import yt.wer.efms.model.Parcel;
import yt.wer.efms.model.ParcelOperation;
import yt.wer.efms.model.ParcelPeriod;
import yt.wer.efms.model.Period;
import yt.wer.efms.repository.AttachmentRepository;
import yt.wer.efms.repository.CultureCodeRepository;
import yt.wer.efms.repository.FarmOperationTypeDefaultToolRepository;
import yt.wer.efms.repository.FarmRepository;
import yt.wer.efms.repository.OperationProductRepository;
import yt.wer.efms.repository.OperationTypeRepository;
import yt.wer.efms.repository.ParcelOperationRepository;
import yt.wer.efms.repository.ParcelPeriodRepository;
import yt.wer.efms.repository.ParcelRepository;
import yt.wer.efms.repository.ParcelShareRepository;
import yt.wer.efms.repository.ProductRepository;
import yt.wer.efms.repository.ToolRepository;
import yt.wer.efms.repository.UnitRepository;
import yt.wer.efms.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParcelOperationServiceTest {

    @Mock private ParcelOperationRepository parcelOperationRepository;
    @Mock private ParcelRepository parcelRepository;
    @Mock private OperationTypeRepository operationTypeRepository;
    @Mock private OperationProductRepository operationProductRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ToolRepository toolRepository;
    @Mock private UnitRepository unitRepository;
    @Mock private FarmRepository farmRepository;
    @Mock private PermissionService permissionService;
    @Mock private ParcelShareRepository parcelShareRepository;
    @Mock private FarmService farmService;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @Mock private FarmOperationTypeDefaultToolRepository farmOpTypeDefaultRepository;
    @Mock private AttachmentRepository attachmentRepository;
    @Mock private ParcelPeriodRepository parcelPeriodRepository;
    @Mock private CultureCodeRepository cultureCodeRepository;

    @InjectMocks
    private ParcelOperationService service;

    @Captor private ArgumentCaptor<ParcelOperation> operationCaptor;

    private static final long FARM_ID = 10L;
    private static final long PARCEL_ID = 1L;
    private static final long OP_ID = 100L;
    private static final long USER_ID = 5L;

    private static Farm farm(Long id) {
        Farm f = new Farm();
        f.setId(id);
        return f;
    }

    private static Parcel parcelInFarm(Long parcelId, Long farmId) {
        Parcel p = new Parcel();
        p.setId(parcelId);
        p.setFarm(farm(farmId));
        return p;
    }

    private static ParcelOperation operationLinkedTo(Long opId, Parcel... parcels) {
        ParcelOperation op = new ParcelOperation();
        op.setId(opId);
        Set<Parcel> set = new HashSet<>();
        for (Parcel p : parcels) set.add(p);
        op.setParcels(set);
        return op;
    }

    private static ParcelPeriod activePeriodAroundNow() {
        Period period = new Period();
        period.setId(50L);
        period.setStartDate(LocalDateTime.now().minusDays(1));
        period.setEndDate(LocalDateTime.now().plusDays(1));
        ParcelPeriod pp = new ParcelPeriod();
        pp.setId(60L);
        pp.setActive(true);
        pp.setPeriod(period);
        return pp;
    }

    private void caller(boolean admin) {
        when(permissionService.currentUserId()).thenReturn(USER_ID);
        when(permissionService.isCurrentUserAdmin()).thenReturn(admin);
    }

    @Test
    void listOperationsForParcel_throwsWhenParcelNotFound() {
        when(parcelRepository.findById(PARCEL_ID)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.listOperationsForParcel(FARM_ID, PARCEL_ID, null));
    }

    @Test
    void listOperationsForParcel_throwsWhenParcelNotInFarm() {
        when(parcelRepository.findById(PARCEL_ID)).thenReturn(Optional.of(parcelInFarm(PARCEL_ID, 99L)));

        assertThrows(RuntimeException.class,
                () -> service.listOperationsForParcel(FARM_ID, PARCEL_ID, null));
    }

    @Test
    void listOperationsForParcel_throwsWhenNoAccessAndNoShares() {
        Parcel parcel = parcelInFarm(PARCEL_ID, FARM_ID);
        when(parcelRepository.findById(PARCEL_ID)).thenReturn(Optional.of(parcel));
        caller(false);
        when(permissionService.canViewParcel(parcel, USER_ID, false)).thenReturn(false);
        when(farmService.getResearchSharesForParcel(FARM_ID, PARCEL_ID, null))
                .thenReturn(Collections.emptyList());

        assertThrows(RuntimeException.class,
                () -> service.listOperationsForParcel(FARM_ID, PARCEL_ID, null));
    }

    @Test
    void listOperationsForParcel_returnsMappedOperationsWhenViewable() {
        Parcel parcel = parcelInFarm(PARCEL_ID, FARM_ID);
        when(parcelRepository.findById(PARCEL_ID)).thenReturn(Optional.of(parcel));
        caller(false);
        when(permissionService.canViewParcel(parcel, USER_ID, false)).thenReturn(true);
        ParcelOperation op = operationLinkedTo(OP_ID, parcel);
        when(parcelOperationRepository.findDistinctByParcelsIdOrderByDateDesc(PARCEL_ID))
                .thenReturn(List.of(op));
        when(operationProductRepository.findByOperationId(OP_ID)).thenReturn(Collections.emptyList());
        when(attachmentRepository.findByOperationId(OP_ID)).thenReturn(Collections.emptyList());

        List<ParcelOperationDto> result = service.listOperationsForParcel(FARM_ID, PARCEL_ID, null);

        assertEquals(1, result.size());
        assertEquals(OP_ID, result.get(0).getId());
    }

    // createOperation

    @Test
    void createOperation_throwsWhenCannotEditFarm() {
        caller(false);
        when(permissionService.canEditFarm(FARM_ID, USER_ID, false)).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> service.createOperation(FARM_ID, PARCEL_ID, new CreateParcelOperationRequest()));
        verify(parcelOperationRepository, never()).save(any());
    }

    @Test
    void createOperation_throwsWhenParcelNotInFarm() {
        caller(false);
        when(permissionService.canEditFarm(FARM_ID, USER_ID, false)).thenReturn(true);
        when(farmRepository.findById(FARM_ID)).thenReturn(Optional.of(farm(FARM_ID)));
        when(parcelRepository.findById(PARCEL_ID)).thenReturn(Optional.of(parcelInFarm(PARCEL_ID, 99L)));

        assertThrows(RuntimeException.class,
                () -> service.createOperation(FARM_ID, PARCEL_ID, new CreateParcelOperationRequest()));
    }

    @Test
    void createOperation_throwsWhenCannotEditParcel() {
        Parcel parcel = parcelInFarm(PARCEL_ID, FARM_ID);
        caller(false);
        when(permissionService.canEditFarm(FARM_ID, USER_ID, false)).thenReturn(true);
        when(farmRepository.findById(FARM_ID)).thenReturn(Optional.of(farm(FARM_ID)));
        when(parcelRepository.findById(PARCEL_ID)).thenReturn(Optional.of(parcel));
        when(permissionService.canEditParcel(parcel, USER_ID, false)).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> service.createOperation(FARM_ID, PARCEL_ID, new CreateParcelOperationRequest()));
        verify(parcelOperationRepository, never()).save(any());
    }

    @Test
    void createOperation_throwsBadRequestWhenParcelNotActiveForPeriod() {
        Parcel parcel = parcelInFarm(PARCEL_ID, FARM_ID);
        caller(false);
        when(permissionService.canEditFarm(FARM_ID, USER_ID, false)).thenReturn(true);
        when(farmRepository.findById(FARM_ID)).thenReturn(Optional.of(farm(FARM_ID)));
        when(parcelRepository.findById(PARCEL_ID)).thenReturn(Optional.of(parcel));
        when(permissionService.canEditParcel(parcel, USER_ID, false)).thenReturn(true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        // No periods at all -> the parcel is not active for the operation's date.
        when(parcelPeriodRepository.findByParcelId(PARCEL_ID)).thenReturn(Collections.emptyList());

        assertThrows(ResponseStatusException.class,
                () -> service.createOperation(FARM_ID, PARCEL_ID, new CreateParcelOperationRequest()));
        verify(parcelOperationRepository, never()).save(any());
    }

    @Test
    void createOperation_savesWhenParcelIsActive() {
        Parcel parcel = parcelInFarm(PARCEL_ID, FARM_ID);
        caller(false);
        when(permissionService.canEditFarm(FARM_ID, USER_ID, false)).thenReturn(true);
        when(farmRepository.findById(FARM_ID)).thenReturn(Optional.of(farm(FARM_ID)));
        when(parcelRepository.findById(PARCEL_ID)).thenReturn(Optional.of(parcel));
        when(permissionService.canEditParcel(parcel, USER_ID, false)).thenReturn(true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(parcelPeriodRepository.findByParcelId(PARCEL_ID)).thenReturn(List.of(activePeriodAroundNow()));
        when(parcelOperationRepository.save(any())).thenAnswer(inv -> {
            ParcelOperation op = inv.getArgument(0);
            op.setId(OP_ID);
            return op;
        });
        when(operationProductRepository.findByOperationId(OP_ID)).thenReturn(Collections.emptyList());
        when(attachmentRepository.findByOperationId(OP_ID)).thenReturn(Collections.emptyList());

        Optional<ParcelOperationDto> result =
                service.createOperation(FARM_ID, PARCEL_ID, new CreateParcelOperationRequest());

        assertTrue(result.isPresent());
        assertEquals(OP_ID, result.get().getId());
        verify(parcelOperationRepository).save(operationCaptor.capture());
        assertNotNull(operationCaptor.getValue().getCreatedAt());
        assertNotNull(operationCaptor.getValue().getDate());
        assertEquals(1, operationCaptor.getValue().getParcels().size());
    }

    // updateOperation
    
    @Test
    void updateOperation_throwsWhenCannotEditParcel() {
        Parcel parcel = parcelInFarm(PARCEL_ID, FARM_ID);
        caller(false);
        when(permissionService.canEditFarm(FARM_ID, USER_ID, false)).thenReturn(true);
        when(farmRepository.findById(FARM_ID)).thenReturn(Optional.of(farm(FARM_ID)));
        when(parcelRepository.findById(PARCEL_ID)).thenReturn(Optional.of(parcel));
        when(permissionService.canEditParcel(parcel, USER_ID, false)).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> service.updateOperation(FARM_ID, PARCEL_ID, OP_ID, new CreateParcelOperationRequest()));
        verify(parcelOperationRepository, never()).save(any());
    }

    @Test
    void updateOperation_throwsWhenOperationNotLinkedToParcel() {
        Parcel parcel = parcelInFarm(PARCEL_ID, FARM_ID);
        caller(false);
        when(permissionService.canEditFarm(FARM_ID, USER_ID, false)).thenReturn(true);
        when(farmRepository.findById(FARM_ID)).thenReturn(Optional.of(farm(FARM_ID)));
        when(parcelRepository.findById(PARCEL_ID)).thenReturn(Optional.of(parcel));
        when(permissionService.canEditParcel(parcel, USER_ID, false)).thenReturn(true);
        // The operation is linked to a different parcel (id 999), not PARCEL_ID.
        ParcelOperation op = operationLinkedTo(OP_ID, parcelInFarm(999L, FARM_ID));
        when(parcelOperationRepository.findById(OP_ID)).thenReturn(Optional.of(op));

        assertThrows(RuntimeException.class,
                () -> service.updateOperation(FARM_ID, PARCEL_ID, OP_ID, new CreateParcelOperationRequest()));
        verify(parcelOperationRepository, never()).save(any());
    }

    @Test
    void updateOperation_updatesAndPersists() {
        Parcel parcel = parcelInFarm(PARCEL_ID, FARM_ID);
        caller(false);
        when(permissionService.canEditFarm(FARM_ID, USER_ID, false)).thenReturn(true);
        when(farmRepository.findById(FARM_ID)).thenReturn(Optional.of(farm(FARM_ID)));
        when(parcelRepository.findById(PARCEL_ID)).thenReturn(Optional.of(parcel));
        when(permissionService.canEditParcel(parcel, USER_ID, false)).thenReturn(true);
        ParcelOperation op = operationLinkedTo(OP_ID, parcel);
        when(parcelOperationRepository.findById(OP_ID)).thenReturn(Optional.of(op));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        
        when(parcelPeriodRepository.findByParcelId(PARCEL_ID)).thenReturn(Collections.emptyList());
        when(parcelOperationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(operationProductRepository.findByOperationId(OP_ID)).thenReturn(Collections.emptyList());
        when(attachmentRepository.findByOperationId(OP_ID)).thenReturn(Collections.emptyList());

        Optional<ParcelOperationDto> result =
                service.updateOperation(FARM_ID, PARCEL_ID, OP_ID, new CreateParcelOperationRequest());

        assertTrue(result.isPresent());
        verify(parcelOperationRepository).save(operationCaptor.capture());
        assertNotNull(operationCaptor.getValue().getModifiedAt());
        verify(operationProductRepository).deleteByOperationId(OP_ID);
    }

    // deleteOperation (soft delete)
    
    @Test
    void deleteOperation_throwsWhenCannotEditFarm() {
        caller(false);
        when(permissionService.canEditFarm(FARM_ID, USER_ID, false)).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> service.deleteOperation(FARM_ID, PARCEL_ID, OP_ID));
        verify(parcelOperationRepository, never()).save(any());
    }

    @Test
    void deleteOperation_throwsWhenOperationNotFound() {
        Parcel parcel = parcelInFarm(PARCEL_ID, FARM_ID);
        caller(false);
        when(permissionService.canEditFarm(FARM_ID, USER_ID, false)).thenReturn(true);
        when(farmRepository.findById(FARM_ID)).thenReturn(Optional.of(farm(FARM_ID)));
        when(parcelRepository.findById(PARCEL_ID)).thenReturn(Optional.of(parcel));
        when(permissionService.canEditParcel(parcel, USER_ID, false)).thenReturn(true);
        when(parcelOperationRepository.findById(OP_ID)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.deleteOperation(FARM_ID, PARCEL_ID, OP_ID));
    }

    @Test
    void deleteOperation_returnsFalseWhenAlreadyDeleted() {
        Parcel parcel = parcelInFarm(PARCEL_ID, FARM_ID);
        caller(false);
        when(permissionService.canEditFarm(FARM_ID, USER_ID, false)).thenReturn(true);
        when(farmRepository.findById(FARM_ID)).thenReturn(Optional.of(farm(FARM_ID)));
        when(parcelRepository.findById(PARCEL_ID)).thenReturn(Optional.of(parcel));
        when(permissionService.canEditParcel(parcel, USER_ID, false)).thenReturn(true);
        ParcelOperation op = operationLinkedTo(OP_ID, parcel);
        op.setDeletedAt(LocalDateTime.now());
        when(parcelOperationRepository.findById(OP_ID)).thenReturn(Optional.of(op));

        assertFalse(service.deleteOperation(FARM_ID, PARCEL_ID, OP_ID));
        verify(parcelOperationRepository, never()).save(any());
    }

    @Test
    void deleteOperation_stampsDeletedAtAndReturnsTrue() {
        Parcel parcel = parcelInFarm(PARCEL_ID, FARM_ID);
        caller(false);
        when(permissionService.canEditFarm(FARM_ID, USER_ID, false)).thenReturn(true);
        when(farmRepository.findById(FARM_ID)).thenReturn(Optional.of(farm(FARM_ID)));
        when(parcelRepository.findById(PARCEL_ID)).thenReturn(Optional.of(parcel));
        when(permissionService.canEditParcel(parcel, USER_ID, false)).thenReturn(true);
        ParcelOperation op = operationLinkedTo(OP_ID, parcel);
        when(parcelOperationRepository.findById(OP_ID)).thenReturn(Optional.of(op));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertTrue(service.deleteOperation(FARM_ID, PARCEL_ID, OP_ID));
        verify(parcelOperationRepository).save(operationCaptor.capture());
        assertNotNull(operationCaptor.getValue().getDeletedAt());
    }
}
