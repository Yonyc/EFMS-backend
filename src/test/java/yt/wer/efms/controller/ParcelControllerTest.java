package yt.wer.efms.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import yt.wer.efms.dto.ParcelDto;
import yt.wer.efms.service.FarmService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParcelControllerTest {

    @Mock private FarmService farmService;

    @InjectMocks
    private ParcelController controller;

    @Test
    void getParcel_returnsOkWithDtoWhenPresent() {
        ParcelDto dto = mock(ParcelDto.class);
        when(farmService.findParcelById(1L)).thenReturn(Optional.of(dto));

        ResponseEntity<ParcelDto> response = controller.getParcel(1L);

        assertEquals(200, response.getStatusCode().value());
        assertSame(dto, response.getBody());
    }

    @Test
    void getParcel_returnsNotFoundWhenAbsent() {
        when(farmService.findParcelById(1L)).thenReturn(Optional.empty());

        ResponseEntity<ParcelDto> response = controller.getParcel(1L);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void deleteParcel_returnsNoContentWhenDeleted() {
        when(farmService.deleteParcel(1L)).thenReturn(true);

        ResponseEntity<Void> response = controller.deleteParcel(1L);

        assertEquals(204, response.getStatusCode().value());
    }

    @Test
    void deleteParcel_returnsNotFoundWhenNotDeleted() {
        when(farmService.deleteParcel(1L)).thenReturn(false);

        ResponseEntity<Void> response = controller.deleteParcel(1L);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void listAllParcels_returnsServiceList() {
        List<ParcelDto> parcels = List.of(mock(ParcelDto.class));
        when(farmService.listAllParcels()).thenReturn(parcels);

        ResponseEntity<List<ParcelDto>> response = controller.listAllParcels();

        assertEquals(200, response.getStatusCode().value());
        assertSame(parcels, response.getBody());
    }
}
