package yt.wer.efms.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import yt.wer.efms.dto.ProductDto;
import yt.wer.efms.dto.ProductInput;
import yt.wer.efms.dto.ToolDto;
import yt.wer.efms.dto.ToolInput;
import yt.wer.efms.model.Farm;
import yt.wer.efms.model.Product;
import yt.wer.efms.model.Tool;
import yt.wer.efms.repository.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FarmAssetServiceTest {

    @Mock private FarmRepository farmRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductTypeRepository productTypeRepository;
    @Mock private UnitRepository unitRepository;
    @Mock private ToolRepository toolRepository;
    @Mock private ToolCategoryRepository toolCategoryRepository;
    @Mock private OperationTypeRepository operationTypeRepository;
    @Mock private AttachmentRepository attachmentRepository;
    @Mock private yt.wer.efms.repository.CultureTypeRepository cultureTypeRepository;
    @Mock private PermissionService permissionService;

    @InjectMocks
    private FarmAssetService farmAssetService;

    @Captor private ArgumentCaptor<Product> productCaptor;
    @Captor private ArgumentCaptor<Tool> toolCaptor;


    @Test
    void createProductThrowsWhenNoEditPermission() {
        when(permissionService.currentUserId()).thenReturn(1L);
        when(permissionService.isCurrentUserAdmin()).thenReturn(false);
        when(permissionService.canEditFarm(10L, 1L, false)).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> farmAssetService.createProduct(10L, new ProductInput()));
        verify(productRepository, never()).save(any());
    }

    @Test
    void createProductSavesProductLinkedToFarm() {
        Farm farm = farm(10L);

        when(permissionService.currentUserId()).thenReturn(1L);
        when(permissionService.isCurrentUserAdmin()).thenReturn(false);
        when(permissionService.canEditFarm(10L, 1L, false)).thenReturn(true);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));

        ProductInput input = new ProductInput();
        input.setName("Glyphosate");

        Product saved = new Product();
        saved.setId(55L);
        saved.setName("Glyphosate");
        saved.setFarm(farm);
        when(productRepository.save(any())).thenReturn(saved);
        when(attachmentRepository.findByProductId(55L)).thenReturn(Collections.emptyList());

        Optional<ProductDto> result = farmAssetService.createProduct(10L, input);

        assertTrue(result.isPresent());
        verify(productRepository).save(productCaptor.capture());
        assertEquals("Glyphosate", productCaptor.getValue().getName());
        assertEquals(farm, productCaptor.getValue().getFarm());
        assertNotNull(productCaptor.getValue().getCreatedAt());
        assertNotNull(productCaptor.getValue().getModifiedAt());
    }

    @Test
    void updateProductThrowsWhenProductBelongsToDifferentFarm() {
        Farm otherFarm = farm(99L);
        Product existing = new Product();
        existing.setId(55L);
        existing.setFarm(otherFarm);

        when(permissionService.currentUserId()).thenReturn(1L);
        when(permissionService.isCurrentUserAdmin()).thenReturn(false);
        when(permissionService.canEditFarm(10L, 1L, false)).thenReturn(true);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm(10L)));
        when(productRepository.findById(55L)).thenReturn(Optional.of(existing));

        assertThrows(RuntimeException.class,
                () -> farmAssetService.updateProduct(10L, 55L, new ProductInput()));
        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProductUpdatesNameAndTimestamp() {
        Farm farm = farm(10L);
        Product existing = new Product();
        existing.setId(55L);
        existing.setName("Old name");
        existing.setFarm(farm);

        ProductInput input = new ProductInput();
        input.setName("New name");

        when(permissionService.currentUserId()).thenReturn(1L);
        when(permissionService.isCurrentUserAdmin()).thenReturn(false);
        when(permissionService.canEditFarm(10L, 1L, false)).thenReturn(true);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(productRepository.findById(55L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(attachmentRepository.findByProductId(55L)).thenReturn(Collections.emptyList());

        Optional<ProductDto> result = farmAssetService.updateProduct(10L, 55L, input);

        assertTrue(result.isPresent());
        verify(productRepository).save(productCaptor.capture());
        assertEquals("New name", productCaptor.getValue().getName());
        assertNotNull(productCaptor.getValue().getModifiedAt());
    }

    @Test
    void deleteProductRemovesItWhenBelongsToFarm() {
        Farm farm = farm(10L);
        Product product = new Product();
        product.setId(55L);
        product.setFarm(farm);

        when(permissionService.currentUserId()).thenReturn(1L);
        when(permissionService.isCurrentUserAdmin()).thenReturn(false);
        when(permissionService.canEditFarm(10L, 1L, false)).thenReturn(true);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(productRepository.findById(55L)).thenReturn(Optional.of(product));

        farmAssetService.deleteProduct(10L, 55L);

        verify(productRepository).delete(product);
    }

    @Test
    void deleteProductDoesNothingWhenProductBelongsToDifferentFarm() {
        Farm farm = farm(10L);
        Product product = new Product();
        product.setId(55L);
        product.setFarm(farm(99L)); // different farm

        when(permissionService.currentUserId()).thenReturn(1L);
        when(permissionService.isCurrentUserAdmin()).thenReturn(false);
        when(permissionService.canEditFarm(10L, 1L, false)).thenReturn(true);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(productRepository.findById(55L)).thenReturn(Optional.of(product));

        farmAssetService.deleteProduct(10L, 55L);

        verify(productRepository, never()).delete(any());
    }

    @Test
    void listProductsThrowsWhenNoViewPermission() {
        when(permissionService.currentUserId()).thenReturn(1L);
        when(permissionService.isCurrentUserAdmin()).thenReturn(false);
        when(permissionService.canViewFarm(10L, 1L, false)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> farmAssetService.listProducts(10L));
    }

    @Test
    void listProductsReturnsProductsForFarm() {
        Farm farm = farm(10L);
        Product p = new Product();
        p.setId(1L);
        p.setName("Herbicide");
        p.setFarm(farm);

        when(permissionService.currentUserId()).thenReturn(1L);
        when(permissionService.isCurrentUserAdmin()).thenReturn(false);
        when(permissionService.canViewFarm(10L, 1L, false)).thenReturn(true);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(productRepository.findByFarmId(10L)).thenReturn(List.of(p));
        when(attachmentRepository.findByProductId(1L)).thenReturn(Collections.emptyList());

        List<ProductDto> result = farmAssetService.listProducts(10L);

        assertEquals(1, result.size());
        assertEquals("Herbicide", result.get(0).getName());
    }


    @Test
    void createToolThrowsWhenNoEditPermission() {
        when(permissionService.currentUserId()).thenReturn(1L);
        when(permissionService.isCurrentUserAdmin()).thenReturn(false);
        when(permissionService.canEditFarm(10L, 1L, false)).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> farmAssetService.createTool(10L, new ToolInput()));
        verify(toolRepository, never()).save(any());
    }

    @Test
    void createToolSavesToolLinkedToFarm() {
        Farm farm = farm(10L);

        when(permissionService.currentUserId()).thenReturn(1L);
        when(permissionService.isCurrentUserAdmin()).thenReturn(false);
        when(permissionService.canEditFarm(10L, 1L, false)).thenReturn(true);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));

        ToolInput input = new ToolInput();
        input.setName("Tractor");

        Tool saved = new Tool();
        saved.setId(77L);
        saved.setName("Tractor");
        saved.setFarm(farm);
        when(toolRepository.save(any())).thenReturn(saved);
        when(attachmentRepository.findByToolId(77L)).thenReturn(Collections.emptyList());

        Optional<ToolDto> result = farmAssetService.createTool(10L, input);

        assertTrue(result.isPresent());
        verify(toolRepository).save(toolCaptor.capture());
        assertEquals("Tractor", toolCaptor.getValue().getName());
        assertEquals(farm, toolCaptor.getValue().getFarm());
        assertNotNull(toolCaptor.getValue().getCreatedAt());
    }

    @Test
    void updateToolThrowsWhenToolBelongsToDifferentFarm() {
        Tool existing = new Tool();
        existing.setId(77L);
        existing.setFarm(farm(99L));

        when(permissionService.currentUserId()).thenReturn(1L);
        when(permissionService.isCurrentUserAdmin()).thenReturn(false);
        when(permissionService.canEditFarm(10L, 1L, false)).thenReturn(true);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm(10L)));
        when(toolRepository.findById(77L)).thenReturn(Optional.of(existing));

        assertThrows(RuntimeException.class,
                () -> farmAssetService.updateTool(10L, 77L, new ToolInput()));
        verify(toolRepository, never()).save(any());
    }

    @Test
    void deleteToolRemovesItWhenBelongsToFarm() {
        Farm farm = farm(10L);
        Tool tool = new Tool();
        tool.setId(77L);
        tool.setFarm(farm);

        when(permissionService.currentUserId()).thenReturn(1L);
        when(permissionService.isCurrentUserAdmin()).thenReturn(false);
        when(permissionService.canEditFarm(10L, 1L, false)).thenReturn(true);
        when(farmRepository.findById(10L)).thenReturn(Optional.of(farm));
        when(toolRepository.findById(77L)).thenReturn(Optional.of(tool));

        farmAssetService.deleteTool(10L, 77L);

        verify(toolRepository).delete(tool);
    }

    @Test
    void listToolsThrowsWhenNoViewPermission() {
        when(permissionService.currentUserId()).thenReturn(1L);
        when(permissionService.isCurrentUserAdmin()).thenReturn(false);
        when(permissionService.canViewFarm(10L, 1L, false)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> farmAssetService.listTools(10L));
    }


    private static Farm farm(Long id) {
        Farm f = new Farm();
        f.setId(id);
        return f;
    }
}
