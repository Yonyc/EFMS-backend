package yt.wer.efms.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import yt.wer.efms.dto.PhytoImportResult;
import yt.wer.efms.dto.ProductDto;
import yt.wer.efms.model.Product;
import yt.wer.efms.repository.PhytoSyncLogRepository;
import yt.wer.efms.repository.ProductRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfficialProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private PhytoSyncLogRepository syncLogRepository;

    @InjectMocks
    private OfficialProductService service;

    private Path writeJson(Path dir, String name, String content) throws Exception {
        Path file = dir.resolve(name);
        Files.writeString(file, content);
        return file;
    }

    @Test
    void syncInProgress_isFalseByDefault() {
        assertFalse(service.isSyncInProgress());
    }

    @Test
    void importFromFile_throwsWhenPathBlank() {
        assertThrows(RuntimeException.class, () -> service.importFromFile("  ", "v1"));
    }

    @Test
    void importFromFile_throwsWhenFileMissing(@TempDir Path tmp) {
        String missing = tmp.resolve("does-not-exist.json").toString();
        assertThrows(RuntimeException.class, () -> service.importFromFile(missing, "v1"));
    }

    @Test
    void importFromFile_throwsWhenNotAJsonArray(@TempDir Path tmp) throws Exception {
        Path file = writeJson(tmp, "bad.json", "{\"AUTH_NUMBER\":\"1\"}");

        assertThrows(RuntimeException.class, () -> service.importFromFile(file.toString(), "v1"));
    }

    @Test
    void importFromFile_createsNewProduct(@TempDir Path tmp) throws Exception {
        Path file = writeJson(tmp, "2026_05_01_FULL_1.json",
                "[{\"AUTH_NUMBER\":\"1234\",\"pestProduct\":{\"PRODUCT_name\":\"Glypho\"}}]");
        when(productRepository.findByOfficialAuthNumberAndOfficialCurrentTrue("1234"))
                .thenReturn(Optional.empty());

        PhytoImportResult result = service.importFromFile(file.toString(), "v1");

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getCreated());
        assertEquals(0, result.getUpdated());
        assertEquals(0, result.getSkipped());

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product saved = captor.getValue();
        assertEquals("Glypho", saved.getName());
        assertEquals("1234", saved.getOfficialAuthNumber());
        assertEquals("v1", saved.getOfficialVersionTag());
    }

    @Test
    void importFromFile_skipsEntryWithoutAuthNumber(@TempDir Path tmp) throws Exception {
        Path file = writeJson(tmp, "x.json", "[{\"pestProduct\":{\"PRODUCT_name\":\"NoAuth\"}}]");

        PhytoImportResult result = service.importFromFile(file.toString(), "v1");

        assertEquals(1, result.getTotal());
        assertEquals(0, result.getCreated());
        assertEquals(1, result.getSkipped());
        verify(productRepository, never()).save(any());
    }

    @Test
    void importFromFile_updatesInPlaceWhenSameVersionTag(@TempDir Path tmp) throws Exception {
        Path file = writeJson(tmp, "x.json",
                "[{\"AUTH_NUMBER\":\"1234\",\"pestProduct\":{\"PRODUCT_name\":\"Updated\"}}]");
        Product existing = new Product();
        existing.setOfficialAuthNumber("1234");
        existing.setOfficialVersionTag("v1");
        existing.setOfficialCurrent(true);
        when(productRepository.findByOfficialAuthNumberAndOfficialCurrentTrue("1234"))
                .thenReturn(Optional.of(existing));

        PhytoImportResult result = service.importFromFile(file.toString(), "v1");

        assertEquals(1, result.getUpdated());
        assertEquals(0, result.getCreated());
        // Same instance updated, saved exactly once (no supersede save).
        verify(productRepository, times(1)).save(existing);
        assertEquals("Updated", existing.getName());
    }

    @Test
    void importFromFile_supersedesPreviousVersion_whenVersionTagDiffers(@TempDir Path tmp) throws Exception {
        Path file = writeJson(tmp, "x.json",
                "[{\"AUTH_NUMBER\":\"1234\",\"pestProduct\":{\"PRODUCT_name\":\"NewVer\"}}]");
        Product previous = new Product();
        previous.setOfficialAuthNumber("1234");
        previous.setOfficialVersionTag("v1");
        previous.setOfficialCurrent(true);
        when(productRepository.findByOfficialAuthNumberAndOfficialCurrentTrue("1234"))
                .thenReturn(Optional.of(previous));

        PhytoImportResult result = service.importFromFile(file.toString(), "v2");

        assertEquals(1, result.getCreated());
        assertEquals(0, result.getUpdated());
        // Previous version is flagged not-current, and a new product is saved: 2 saves.
        assertFalse(previous.getOfficialCurrent());
        verify(productRepository, times(2)).save(any(Product.class));
    }

    @Test
    void importFromFile_defaultsVersionTagFromFileName(@TempDir Path tmp) throws Exception {
        Path file = writeJson(tmp, "2026_05_01_FULL_1.json",
                "[{\"AUTH_NUMBER\":\"1234\",\"pestProduct\":{\"PRODUCT_name\":\"P\"}}]");
        when(productRepository.findByOfficialAuthNumberAndOfficialCurrentTrue("1234"))
                .thenReturn(Optional.empty());

        PhytoImportResult result = service.importFromFile(file.toString(), null);

        // versionTag resolves to the file name without extension
        assertEquals("2026_05_01_FULL_1", result.getVersionTag());
    }

    @Test
    void listOfficialProducts_mapsEntitiesToDtos() {
        Product p = new Product();
        p.setId(42L);
        p.setName("Roundup");
        p.setOfficial(true);
        p.setOfficialCurrent(true);
        p.setOfficialAuthNumber("9999");
        when(productRepository.findByOfficialTrueAndOfficialCurrentTrue()).thenReturn(List.of(p));

        List<ProductDto> result = service.listOfficialProducts();

        assertEquals(1, result.size());
        assertEquals(42L, result.get(0).getId());
        assertEquals("Roundup", result.get(0).getName());
        assertEquals("9999", result.get(0).getOfficialAuthNumber());
    }
}
