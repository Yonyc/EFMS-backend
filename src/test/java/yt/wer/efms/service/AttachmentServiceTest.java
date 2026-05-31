package yt.wer.efms.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import yt.wer.efms.dto.AttachmentDto;
import yt.wer.efms.model.Attachment;
import yt.wer.efms.model.Farm;
import yt.wer.efms.model.Parcel;
import yt.wer.efms.model.ParcelOperation;
import yt.wer.efms.model.Product;
import yt.wer.efms.model.Tool;
import yt.wer.efms.repository.AttachmentRepository;
import yt.wer.efms.repository.ParcelOperationRepository;
import yt.wer.efms.repository.ProductRepository;
import yt.wer.efms.repository.ToolRepository;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock private AttachmentRepository attachmentRepository;
    @Mock private ParcelOperationRepository operationRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ToolRepository toolRepository;
    @Mock private PermissionService permissionService;

    @InjectMocks
    private AttachmentService attachmentService;

    @Captor private ArgumentCaptor<Attachment> attachmentCaptor;

    private static final MockMultipartFile FILE =
            new MockMultipartFile("file", "scan.pdf", "application/pdf", "hello".getBytes());

    private void allowEdit(boolean allowed) {
        when(permissionService.currentUserId()).thenReturn(1L);
        when(permissionService.isCurrentUserAdmin()).thenReturn(false);
        when(permissionService.canEditFarm(10L, 1L, false)).thenReturn(allowed);
    }

    private static Farm farm(Long id) {
        Farm f = new Farm();
        f.setId(id);
        return f;
    }

    @Test
    void uploadToOperation_throwsWhenNoEditPermission() {
        allowEdit(false);

        assertThrows(RuntimeException.class,
                () -> attachmentService.uploadToOperation(10L, 5L, FILE));
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    void uploadToOperation_savesAttachmentLinkedToOperation() {
        allowEdit(true);
        ParcelOperation op = new ParcelOperation();
        op.setId(5L);
        when(operationRepository.findById(5L)).thenReturn(Optional.of(op));
        when(attachmentRepository.save(any())).thenAnswer(inv -> {
            Attachment a = inv.getArgument(0);
            a.setId(99L);
            return a;
        });

        AttachmentDto dto = attachmentService.uploadToOperation(10L, 5L, FILE);

        assertEquals(99L, dto.getId());
        assertEquals("scan.pdf", dto.getOriginalFilename());
        verify(attachmentRepository).save(attachmentCaptor.capture());
        assertEquals(op, attachmentCaptor.getValue().getOperation());
        assertTrue(attachmentCaptor.getValue().getUrl().endsWith(".pdf"));
    }

    @Test
    void uploadToOperation_throwsWhenOperationMissing() {
        allowEdit(true);
        when(operationRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> attachmentService.uploadToOperation(10L, 5L, FILE));
    }

    @Test
    void uploadToProduct_throwsWhenProductBelongsToAnotherFarm() {
        allowEdit(true);
        Product product = new Product();
        product.setId(7L);
        product.setFarm(farm(99L)); // different farm
        when(productRepository.findById(7L)).thenReturn(Optional.of(product));

        assertThrows(RuntimeException.class,
                () -> attachmentService.uploadToProduct(10L, 7L, FILE));
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    void uploadToProduct_savesWhenProductBelongsToFarm() {
        allowEdit(true);
        Product product = new Product();
        product.setId(7L);
        product.setFarm(farm(10L));
        when(productRepository.findById(7L)).thenReturn(Optional.of(product));
        when(attachmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        attachmentService.uploadToProduct(10L, 7L, FILE);

        verify(attachmentRepository).save(attachmentCaptor.capture());
        assertEquals(product, attachmentCaptor.getValue().getProduct());
    }

    @Test
    void uploadToTool_throwsWhenToolBelongsToAnotherFarm() {
        allowEdit(true);
        Tool tool = new Tool();
        tool.setId(8L);
        tool.setFarm(farm(99L));
        when(toolRepository.findById(8L)).thenReturn(Optional.of(tool));

        assertThrows(RuntimeException.class,
                () -> attachmentService.uploadToTool(10L, 8L, FILE));
    }

    @Test
    void delete_throwsWhenAttachmentNotInFarm() {
        allowEdit(true);
        Product product = new Product();
        product.setFarm(farm(99L));
        Attachment att = new Attachment();
        att.setId(3L);
        att.setProduct(product);
        when(attachmentRepository.findById(3L)).thenReturn(Optional.of(att));

        assertThrows(RuntimeException.class, () -> attachmentService.delete(10L, 3L));
        verify(attachmentRepository, never()).delete(any());
    }

    @Test
    void delete_removesAttachmentBelongingToFarmViaOperationParcel() {
        allowEdit(true);
        Parcel parcel = new Parcel();
        parcel.setFarm(farm(10L));
        Set<Parcel> parcels = new HashSet<>();
        parcels.add(parcel);
        ParcelOperation op = new ParcelOperation();
        op.setParcels(parcels);

        Attachment att = new Attachment();
        att.setId(3L);
        att.setOperation(op);
        att.setUrl(null); // skip disk deletion branch
        when(attachmentRepository.findById(3L)).thenReturn(Optional.of(att));

        attachmentService.delete(10L, 3L);

        verify(attachmentRepository).delete(att);
    }

    @Test
    void delete_throwsWhenNoEditPermission() {
        allowEdit(false);

        assertThrows(RuntimeException.class, () -> attachmentService.delete(10L, 3L));
        verify(attachmentRepository, never()).delete(any());
    }
}
