package yt.wer.efms.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import yt.wer.efms.dto.CreateParcelOperationRequest;
import yt.wer.efms.dto.OperationTypeDto;
import yt.wer.efms.dto.ParcelOperationDto;
import yt.wer.efms.model.OperationType;
import yt.wer.efms.repository.OperationTypeRepository;
import yt.wer.efms.service.ParcelOperationService;
import yt.wer.efms.service.PermissionService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParcelOperationControllerTest {

    @Mock private ParcelOperationService parcelOperationService;
    @Mock private OperationTypeRepository operationTypeRepository;
    @Mock private PermissionService permissionService;

    @InjectMocks
    private ParcelOperationController controller;

    @Test
    void createOperationType_returns403WhenNotAdmin() {
        when(permissionService.isCurrentUserAdmin()).thenReturn(false);

        ResponseEntity<OperationTypeDto> response =
                controller.createOperationType(new OperationTypeDto(null, "Sowing", null, null, null));

        assertEquals(403, response.getStatusCode().value());
        verify(operationTypeRepository, never()).save(any());
    }

    @Test
    void createOperationType_returns400WhenNameBlank() {
        when(permissionService.isCurrentUserAdmin()).thenReturn(true);

        ResponseEntity<OperationTypeDto> response =
                controller.createOperationType(new OperationTypeDto(null, "  ", null, null, null));

        assertEquals(400, response.getStatusCode().value());
        verify(operationTypeRepository, never()).save(any());
    }

    @Test
    void createOperationType_returns201WhenValid() {
        when(permissionService.isCurrentUserAdmin()).thenReturn(true);
        when(operationTypeRepository.save(any())).thenAnswer(inv -> {
            OperationType t = inv.getArgument(0);
            t.setId(7L);
            return t;
        });

        ResponseEntity<OperationTypeDto> response =
                controller.createOperationType(new OperationTypeDto(null, "Sowing", null, null, null));

        assertEquals(201, response.getStatusCode().value());
        assertEquals("Sowing", response.getBody().getName());
    }

    @Test
    void deleteOperationType_returns403WhenNotAdmin() {
        when(permissionService.isCurrentUserAdmin()).thenReturn(false);

        ResponseEntity<Void> response = controller.deleteOperationType(7L);

        assertEquals(403, response.getStatusCode().value());
        verify(operationTypeRepository, never()).deleteById(any());
    }

    @Test
    void deleteOperationType_returns404WhenMissing() {
        when(permissionService.isCurrentUserAdmin()).thenReturn(true);
        when(operationTypeRepository.existsById(7L)).thenReturn(false);

        ResponseEntity<Void> response = controller.deleteOperationType(7L);

        assertEquals(404, response.getStatusCode().value());
        verify(operationTypeRepository, never()).deleteById(any());
    }

    @Test
    void deleteOperationType_returns204WhenDeleted() {
        when(permissionService.isCurrentUserAdmin()).thenReturn(true);
        when(operationTypeRepository.existsById(7L)).thenReturn(true);

        ResponseEntity<Void> response = controller.deleteOperationType(7L);

        assertEquals(204, response.getStatusCode().value());
        verify(operationTypeRepository).deleteById(7L);
    }

    @Test
    void createOperation_returns201WhenCreated() {
        CreateParcelOperationRequest req = new CreateParcelOperationRequest();
        ParcelOperationDto dto = org.mockito.Mockito.mock(ParcelOperationDto.class);
        when(dto.getId()).thenReturn(100L);
        when(parcelOperationService.createOperation(10L, 1L, req)).thenReturn(Optional.of(dto));

        ResponseEntity<?> response = controller.createOperation(10L, 1L, req);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(dto, response.getBody());
    }

    @Test
    void createOperation_returns400WhenEmptyOptional() {
        CreateParcelOperationRequest req = new CreateParcelOperationRequest();
        when(parcelOperationService.createOperation(10L, 1L, req)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.createOperation(10L, 1L, req);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void createOperation_returns404WhenServiceSaysNotFound() {
        CreateParcelOperationRequest req = new CreateParcelOperationRequest();
        when(parcelOperationService.createOperation(10L, 1L, req))
                .thenThrow(new RuntimeException("Parcel not found"));

        ResponseEntity<?> response = controller.createOperation(10L, 1L, req);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void deleteOperation_returns204WhenDeleted() {
        when(parcelOperationService.deleteOperation(10L, 1L, 100L)).thenReturn(true);

        ResponseEntity<Void> response = controller.deleteOperation(10L, 1L, 100L);

        assertEquals(204, response.getStatusCode().value());
    }

    @Test
    void deleteOperation_returns404WhenNotDeleted() {
        when(parcelOperationService.deleteOperation(10L, 1L, 100L)).thenReturn(false);

        ResponseEntity<Void> response = controller.deleteOperation(10L, 1L, 100L);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void deleteOperation_returns403WhenServiceThrowsNonNotFound() {
        when(parcelOperationService.deleteOperation(10L, 1L, 100L))
                .thenThrow(new RuntimeException("Not allowed to edit this parcel"));

        ResponseEntity<Void> response = controller.deleteOperation(10L, 1L, 100L);

        assertEquals(403, response.getStatusCode().value());
    }
}
