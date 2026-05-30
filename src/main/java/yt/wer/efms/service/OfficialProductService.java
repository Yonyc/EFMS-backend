package yt.wer.efms.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yt.wer.efms.dto.PhytoFilterOptionsDto;
import yt.wer.efms.dto.PhytoFilterOptionsDto.LabeledValue;
import yt.wer.efms.dto.PhytoImportResult;
import yt.wer.efms.dto.PhytoSyncLogDto;
import yt.wer.efms.dto.ProductDto;
import yt.wer.efms.model.PhytoSyncLog;
import yt.wer.efms.model.Product;
import yt.wer.efms.repository.PhytoSyncLogRepository;
import yt.wer.efms.repository.ProductRepository;

import com.jcraft.jsch.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.Map;

@Service
@Transactional
public class OfficialProductService {
    private final ProductRepository productRepository;
    private final PhytoSyncLogRepository syncLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${efms.phyto.sftp.host:ftp.health.belgium.be}")
    private String sftpHost;

    @Value("${efms.phyto.sftp.port:22}")
    private int sftpPort;

    @Value("${efms.phyto.sftp.user:ftppanama-opendata}")
    private String sftpUser;

    @Value("${efms.phyto.sftp.password:guest}")
    private String sftpPassword;

    @Value("${efms.phyto.sftp.remote-dir:/files}")
    private String sftpRemoteDir;

    private final AtomicBoolean operationInProgress = new AtomicBoolean(false);
    private volatile boolean syncInProgress = false;

    public boolean isSyncInProgress() { return syncInProgress; }

    public OfficialProductService(ProductRepository productRepository, PhytoSyncLogRepository syncLogRepository) {
        this.productRepository = productRepository;
        this.syncLogRepository = syncLogRepository;
    }

    public List<ProductDto> listOfficialProducts() {
        return productRepository.findByOfficialTrueAndOfficialCurrentTrue().stream()
                .map(this::toProductDto)
                .collect(Collectors.toList());
    }

    public Page<ProductDto> listOfficialProducts(String query, String decisionCode, String userGroupCode, String productTypeCode, Pageable pageable) {
        return productRepository.searchOfficialProducts(
                blankToNull(query), blankToNull(decisionCode), blankToNull(userGroupCode), blankToNull(productTypeCode), pageable
        ).map(this::toProductDto);
    }

    public PhytoFilterOptionsDto getFilterOptions() {
        List<LabeledValue> decisions = productRepository.findDistinctDecisionCodes().stream()
                .map(r -> new LabeledValue((String) r[0], labelOrCode((String) r[1], (String) r[0])))
                .sorted(Comparator.comparing(LabeledValue::label))
                .collect(Collectors.toList());

        List<LabeledValue> userGroups = productRepository.findDistinctUserGroupCodes().stream()
                .map(r -> new LabeledValue((String) r[0], labelOrCode((String) r[1], (String) r[0])))
                .sorted(Comparator.comparing(LabeledValue::label))
                .collect(Collectors.toList());

        // Product types are stored as comma-separated strings
        Map<String, String> typesMap = new java.util.LinkedHashMap<>();
        for (Object[] row : productRepository.findDistinctProductTypePairs()) {
            String codes = (String) row[0];
            String labels = (String) row[1];
            if (codes == null) continue;
            String[] codeArr = codes.split(",\\s*");
            String[] labelArr = labels != null ? labels.split(",\\s*") : new String[0];
            for (int i = 0; i < codeArr.length; i++) {
                String code = codeArr[i].trim();
                String label = (i < labelArr.length) ? labelArr[i].trim() : code;
                typesMap.putIfAbsent(code, label);
            }
        }
        List<LabeledValue> productTypes = typesMap.entrySet().stream()
                .map(e -> new LabeledValue(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(LabeledValue::label))
                .collect(Collectors.toList());

        return new PhytoFilterOptionsDto(decisions, userGroups, productTypes);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static String labelOrCode(String label, String code) {
        return (label != null && !label.isBlank()) ? label : code;
    }

    public List<PhytoSyncLogDto> listSyncLogs() {
        return syncLogRepository.findAllByOrderByProcessedAtDesc().stream()
                .map(this::toSyncLogDto)
                .collect(Collectors.toList());
    }

    public List<PhytoImportResult> syncFromRemote() {
        if (!operationInProgress.compareAndSet(false, true)) {
            throw new IllegalStateException("Another operation is already in progress");
        }
        syncInProgress = true;
        Session session = null;
        ChannelSftp sftp = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(sftpUser, sftpHost, sftpPort);
            session.setPassword(sftpPassword);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(30_000);

            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect(10_000);

            Set<String> alreadyImported = syncLogRepository.findAll().stream()
                    .filter(l -> "SUCCESS".equals(l.getStatus()))
                    .map(PhytoSyncLog::getFileName)
                    .collect(Collectors.toSet());

            List<String> newFiles = new ArrayList<>();
            for (Object obj : sftp.ls(sftpRemoteDir)) {
                ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) obj;
                String name = entry.getFilename();
                if (name.equals(".") || name.equals("..") || !name.endsWith(".json")) continue;
                if (!alreadyImported.contains(name)) newFiles.add(name);
            }
            newFiles.sort(this::compareFileNames);

            List<PhytoImportResult> results = new ArrayList<>();
            for (String fileName : newFiles) {
                try (InputStream in = sftp.get(sftpRemoteDir + "/" + fileName)) {
                    PhytoImportResult result = importFromStream(stripJsonExtension(fileName), in);
                    recordSyncLog(fileName, result, null);
                    results.add(result);
                } catch (Exception e) {
                    recordSyncLogError(fileName, e.getMessage());
                    results.add(new PhytoImportResult(fileName, sftpRemoteDir + "/" + fileName, 0, 0, 0, 0, LocalDateTime.now()));
                }
            }
            return results;
        } catch (JSchException | SftpException e) {
            throw new RuntimeException("SFTP connection failed: " + e.getMessage(), e);
        } finally {
            if (sftp != null && sftp.isConnected()) sftp.disconnect();
            if (session != null && session.isConnected()) session.disconnect();
            syncInProgress = false;
            operationInProgress.set(false);
        }
    }

    private void recordSyncLog(String fileName, PhytoImportResult result, String error) {
        PhytoSyncLog log = syncLogRepository.findByFileName(fileName).orElse(new PhytoSyncLog());
        log.setFileName(fileName);
        log.setProcessedAt(LocalDateTime.now());
        log.setCreated(result.getCreated());
        log.setUpdated(result.getUpdated());
        log.setSkipped(result.getSkipped());
        log.setTotal(result.getTotal());
        log.setStatus(error == null ? "SUCCESS" : "ERROR");
        log.setErrorMessage(error);
        syncLogRepository.save(log);
    }

    private void recordSyncLogError(String fileName, String error) {
        PhytoSyncLog log = syncLogRepository.findByFileName(fileName).orElse(new PhytoSyncLog());
        log.setFileName(fileName);
        log.setProcessedAt(LocalDateTime.now());
        log.setStatus("ERROR");
        log.setErrorMessage(error);
        syncLogRepository.save(log);
    }

    public PhytoImportResult importFromFile(String filePath, String versionTag) {
        if (filePath == null || filePath.isBlank()) throw new RuntimeException("filePath is required");
        Path path = Path.of(filePath).normalize();
        if (!Files.exists(path) || !Files.isRegularFile(path))
            throw new RuntimeException("PHYTO file not found: " + filePath);
        String resolvedVersion = (versionTag == null || versionTag.isBlank())
                ? stripJsonExtension(path.getFileName().toString()) : versionTag.trim();
        try (InputStream in = Files.newInputStream(path)) {
            return importFromStream(resolvedVersion, in);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read PHYTO file", ex);
        }
    }

    private PhytoImportResult importFromStream(String versionTag, InputStream inputStream) {
        int created = 0, updated = 0, skipped = 0, total = 0;
        LocalDateTime now = LocalDateTime.now();
        try (JsonParser parser = objectMapper.getFactory().createParser(inputStream)) {
            if (parser.nextToken() != JsonToken.START_ARRAY)
                throw new RuntimeException("PHYTO file must contain a JSON array");
            while (parser.nextToken() == JsonToken.START_OBJECT) {
                JsonNode node = objectMapper.readTree(parser);
                total++;
                String authNumber = text(node, "AUTH_NUMBER");
                if (authNumber == null) { skipped++; continue; }

                Optional<Product> existingCurrent = productRepository.findByOfficialAuthNumberAndOfficialCurrentTrue(authNumber);
                Product target;
                boolean isUpdate = false;

                if (existingCurrent.isPresent() && versionTag.equals(existingCurrent.get().getOfficialVersionTag())) {
                    target = existingCurrent.get();
                    isUpdate = true;
                } else {
                    existingCurrent.ifPresent(prev -> {
                        prev.setOfficialCurrent(false);
                        prev.setModifiedAt(now);
                        productRepository.save(prev);
                    });
                    target = new Product();
                    target.setCreatedAt(now);
                    target.setOfficial(true);
                    target.setOfficialCurrent(true);
                    target.setOfficialAuthNumber(authNumber);
                    target.setOfficialVersionTag(versionTag);
                    target.setOfficialImportedAt(now);
                }

                String productName = text(node, "pestProduct", "PRODUCT_name");
                target.setName(productName != null ? productName : authNumber);
                target.setOfficialDecisionCode(text(node, "AUTH_DECISION_decisionCode"));
                target.setOfficialDecisionCodeEn(text(node, "AUTH_DECISION_decisionCode_en"));
                target.setOfficialDecisionCodeFr(text(node, "AUTH_DECISION_decisionCode_fr"));
                target.setOfficialDateFirstAuthorization(text(node, "AUTH_dateFirstAuthorization"));
                target.setOfficialDateFrom(text(node, "AUTH_dateFrom"));
                target.setOfficialDateTo(text(node, "AUTH_dateTo"));
                target.setOfficialSaleTo(text(node, "saleTo"));
                target.setOfficialUseToleratedTo(text(node, "useToleratedTo"));
                target.setOfficialHolderName(text(node, "enterprise", "enterpriseName"));
                target.setOfficialUserGroupCode(text(node, "pestProduct", "PRODUCT_userGroupCode"));
                target.setOfficialUserGroupEn(text(node, "pestProduct", "PRODUCT_userGroupEn"));
                target.setOfficialUserGroupFr(text(node, "pestProduct", "PRODUCT_userGroupFr"));
                target.setOfficialFormulationTypeCode(text(node, "pestProduct", "PRODUCT_formulationTypeCode"));
                target.setOfficialFormulationTypeEn(text(node, "pestProduct", "PRODUCT_formulationTypeEn"));
                target.setOfficialFormulationTypeFr(text(node, "pestProduct", "PRODUCT_formulationTypeFr"));
                target.setOfficialProductTypeCodes(joinList(node, "pestProduct", "pestProductTypes", "productTypeCode"));
                target.setOfficialProductTypeEn(joinList(node, "pestProduct", "pestProductTypes", "productTypeEn"));
                target.setOfficialProductTypeFr(joinList(node, "pestProduct", "pestProductTypes", "productTypeFr"));
                target.setOfficialActiveSubstances(extractActiveSubstances(node));
                target.setOfficialPayloadJson(node.toString());
                target.setModifiedAt(now);
                productRepository.save(target);

                if (isUpdate) updated++; else created++;
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to parse PHYTO data", ex);
        }
        return new PhytoImportResult(versionTag, null, created, updated, skipped, total, now);
    }

    private String extractActiveSubstances(JsonNode node) {
        JsonNode components = node.path("composition").path("PRODUCT_Components");
        if (components.isMissingNode() || !components.isArray()) return null;
        Set<String> names = new LinkedHashSet<>();
        for (JsonNode comp : components) {
            String name = text(comp, "Component_substance", "Substance_nameFr");
            if (name == null) name = text(comp, "Component_substance", "Substance_nameEn");
            if (name != null) names.add(name);
        }
        return names.isEmpty() ? null : String.join(", ", names);
    }

    private ProductDto toProductDto(Product p) {
        ProductDto dto = new ProductDto(
                p.getId(),
                p.getName(),
                p.getProductType() != null ? p.getProductType().getId() : null,
                p.getUnit() != null ? p.getUnit().getId() : null,
                p.getFarm() != null ? p.getFarm().getId() : null,
                p.getCreatedAt(),
                p.getModifiedAt()
        );
        dto.setOfficial(p.getOfficial());
        dto.setOfficialCurrent(p.getOfficialCurrent());
        dto.setOfficialAuthNumber(p.getOfficialAuthNumber());
        dto.setOfficialVersionTag(p.getOfficialVersionTag());
        dto.setOfficialDecisionCode(p.getOfficialDecisionCode());
        dto.setOfficialDecisionCodeEn(p.getOfficialDecisionCodeEn());
        dto.setOfficialDecisionCodeFr(p.getOfficialDecisionCodeFr());
        dto.setOfficialDateFirstAuthorization(p.getOfficialDateFirstAuthorization());
        dto.setOfficialDateFrom(p.getOfficialDateFrom());
        dto.setOfficialDateTo(p.getOfficialDateTo());
        dto.setOfficialSaleTo(p.getOfficialSaleTo());
        dto.setOfficialUseToleratedTo(p.getOfficialUseToleratedTo());
        dto.setOfficialHolderName(p.getOfficialHolderName());
        dto.setOfficialUserGroupCode(p.getOfficialUserGroupCode());
        dto.setOfficialUserGroupEn(p.getOfficialUserGroupEn());
        dto.setOfficialUserGroupFr(p.getOfficialUserGroupFr());
        dto.setOfficialFormulationTypeCode(p.getOfficialFormulationTypeCode());
        dto.setOfficialFormulationTypeEn(p.getOfficialFormulationTypeEn());
        dto.setOfficialFormulationTypeFr(p.getOfficialFormulationTypeFr());
        dto.setOfficialProductTypeCodes(p.getOfficialProductTypeCodes());
        dto.setOfficialProductTypeEn(p.getOfficialProductTypeEn());
        dto.setOfficialProductTypeFr(p.getOfficialProductTypeFr());
        dto.setOfficialActiveSubstances(p.getOfficialActiveSubstances());
        dto.setOfficialImportedAt(p.getOfficialImportedAt());
        return dto;
    }

    private PhytoSyncLogDto toSyncLogDto(PhytoSyncLog log) {
        PhytoSyncLogDto dto = new PhytoSyncLogDto();
        dto.setId(log.getId());
        dto.setFileName(log.getFileName());
        dto.setProcessedAt(log.getProcessedAt());
        dto.setCreated(log.getCreated());
        dto.setUpdated(log.getUpdated());
        dto.setSkipped(log.getSkipped());
        dto.setTotal(log.getTotal());
        dto.setStatus(log.getStatus());
        dto.setErrorMessage(log.getErrorMessage());
        return dto;
    }

    // Sort: FULL files before DELTA within same date, chronological between dates
    private int compareFileNames(String a, String b) {
        String dateA = a.substring(0, Math.min(10, a.length()));
        String dateB = b.substring(0, Math.min(10, b.length()));
        int dateCmp = dateA.compareTo(dateB);
        if (dateCmp != 0) return dateCmp;
        boolean aIsFull = a.contains("_FULL_");
        boolean bIsFull = b.contains("_FULL_");
        if (aIsFull && !bIsFull) return -1;
        if (!aIsFull && bIsFull) return 1;
        return a.compareTo(b);
    }

    private String text(JsonNode node, String... path) {
        JsonNode current = node;
        for (String key : path) {
            if (current == null) return null;
            current = current.path(key);
        }
        if (current == null || current.isMissingNode() || current.isNull()) return null;
        String value = current.asText();
        return value == null || value.isBlank() ? null : value;
    }

    private String joinList(JsonNode node, String... path) {
        if (node == null) return null;
        JsonNode current = node;
        for (int i = 0; i < path.length - 1; i++) {
            current = current.path(path[i]);
        }
        String leafKey = path[path.length - 1];
        if (current == null || current.isMissingNode() || current.isNull() || !current.isArray()) return null;
        Set<String> values = new LinkedHashSet<>();
        for (JsonNode entry : current) {
            if (entry == null || entry.isMissingNode() || entry.isNull()) continue;
            String value = text(entry, leafKey);
            if (value != null) values.add(value);
        }
        if (values.isEmpty()) return null;
        return String.join(", ", values);
    }

    private String stripJsonExtension(String fileName) {
        if (fileName == null) return null;
        if (fileName.toLowerCase().endsWith(".json")) {
            return fileName.substring(0, fileName.length() - 5);
        }
        return fileName;
    }
}
