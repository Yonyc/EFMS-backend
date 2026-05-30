package yt.wer.efms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.util.factory.Hints;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import yt.wer.efms.model.*;
import yt.wer.efms.repository.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Ingests Geofolia shapefiles / Field.Json / Action.Json bundles into the
 * database.
 * Parcels arrive in the {@code parcels} table with {@code status=STAGED};
 * tools, products and operations arrive in their own tables, also with
 * {@code status=STAGED}.
 * Promotion to {@code LIVE} happens in
 * {@link StagedParcelService#approveImport}.
 */
@Service
public class ImportService {

    @FunctionalInterface
    public interface ProgressListener {
        void update(int current, int total, String message);
    }

    private static final ProgressListener SILENT = (c, t, m) -> {
    };

    @Autowired
    private ImportRecordRepository importRecordRepository;
    @Autowired
    private ImportSourceFileRepository importSourceFileRepository;
    @Autowired
    private ParcelRepository parcelRepository;
    @Autowired
    private ParcelPeriodRepository parcelPeriodRepository;
    @Autowired
    private PeriodRepository periodRepository;
    @Autowired
    private CultureCodeRepository cultureCodeRepository;
    @Autowired
    private ToolRepository toolRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ParcelOperationRepository parcelOperationRepository;
    @Autowired
    private OperationProductRepository operationProductRepository;
    @Autowired
    private UnitRepository unitRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    public ImportRecord importShapefile(MultipartFile zipFile, String username) throws Exception {
        return detectAndImport(zipFile.getBytes(), zipFile.getOriginalFilename(), username, SILENT);
    }

    @Transactional
    public ImportRecord detectAndImport(byte[] fileBytes, String originalFilename, String username,
            ProgressListener progress) throws Exception {
        return detectAndImportMultiple(
                List.of(new FileEntry(originalFilename, fileBytes)),
                originalFilename,
                username,
                progress);
    }

    /**
     * Multi-file entry point.
     * Each ZIP is extracted and processed separately with its own ImportSourceFile
     * record.
     * Field.Json / shapefile → STAGED Parcel rows. Action.Json → STAGED
     * Tool/Product/ParcelOperation rows.
     */
    @Transactional
    public ImportRecord detectAndImportMultiple(List<FileEntry> files, String label, String username,
            ProgressListener progress) throws Exception {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        ImportRecord importRecord = new ImportRecord();
        importRecord.setFilename(label);
        importRecord.setName(label);
        importRecord.setCreatedAt(LocalDateTime.now());
        importRecord.setUser(user);
        importRecord = importRecordRepository.save(importRecord);

        Path tempDir = Files.createTempDirectory("import_");
        try {
            progress.update(0, files.size(), "Extracting " + files.size() + " archive(s)…");

            int totalParcels = 0;
            for (int i = 0; i < files.size(); i++) {
                FileEntry entry = files.get(i);
                Path sub = tempDir.resolve("zip_" + i);
                Files.createDirectories(sub);
                extractZip(new ByteArrayInputStream(entry.bytes()), sub);

                ImportSourceFile sourceFile = new ImportSourceFile();
                sourceFile.setImportRecord(importRecord);
                sourceFile.setFilename(entry.name());
                sourceFile.setImportedAt(LocalDateTime.now());
                sourceFile = importSourceFileRepository.save(sourceFile);

                File fieldJson = findFileByName(sub.toFile(), "Field.Json", "Field.json", "field.json");
                File actionJson = findFileByName(sub.toFile(), "Action.Json", "Action.json", "action.json");
                File shp = findShapefileInDir(sub.toFile());

                if (fieldJson == null && shp == null) {
                    progress.update(i + 1, files.size(), "Skipped " + entry.name() + ": no Field.Json or .shp found");
                    continue;
                }

                if (fieldJson != null) {
                    progress.update(i, files.size(), "Importing Geofolia JSON from " + entry.name() + "…");
                    totalParcels += importGeofoliaJsonIntoRecord(fieldJson, importRecord, sourceFile, progress);
                } else {
                    progress.update(i, files.size(), "Importing shapefile from " + entry.name() + "…");
                    totalParcels += importShapefileIntoRecord(shp, importRecord, sourceFile, progress);
                }

                if (actionJson != null) {
                    progress.update(i, files.size(), "Staging Action.Json from " + entry.name() + "…");
                    stageActionJson(actionJson, importRecord, sourceFile);
                }

                progress.update(i + 1, files.size(), "Done with " + entry.name());
            }

            if (files.size() > 1)
                linkParcelsAcrossFiles(importRecord.getId());
            System.out.printf("Multi-import complete: %d parcels from %d file(s)%n", totalParcels, files.size());
            return importRecord;

        } finally {
            deleteDirectory(tempDir.toFile());
        }
    }

    public record FileEntry(String name, byte[] bytes) {
    }

    /** Add more files to an existing non-approved import. */
    @Transactional
    public ImportRecord addFilesToImport(Long importId, List<FileEntry> files, String username,
            ProgressListener progress) throws Exception {
        ImportRecord importRecord = importRecordRepository.findById(importId)
                .orElseThrow(() -> new RuntimeException("Import not found: " + importId));

        if (!importRecord.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized access to import");
        }
        if (importRecord.getApprovedAt() != null) {
            throw new RuntimeException("Cannot add files to an already approved import");
        }

        Path tempDir = Files.createTempDirectory("import_add_");
        try {
            progress.update(0, files.size(), "Extracting " + files.size() + " archive(s)…");

            int totalNew = 0;
            for (int i = 0; i < files.size(); i++) {
                FileEntry entry = files.get(i);
                Path sub = tempDir.resolve("zip_" + i);
                Files.createDirectories(sub);
                extractZip(new ByteArrayInputStream(entry.bytes()), sub);

                ImportSourceFile sourceFile = new ImportSourceFile();
                sourceFile.setImportRecord(importRecord);
                sourceFile.setFilename(entry.name());
                sourceFile.setImportedAt(LocalDateTime.now());
                sourceFile = importSourceFileRepository.save(sourceFile);

                File fieldJson = findFileByName(sub.toFile(), "Field.Json", "Field.json", "field.json");
                File actionJson = findFileByName(sub.toFile(), "Action.Json", "Action.json", "action.json");
                File shp = findShapefileInDir(sub.toFile());

                if (fieldJson == null && shp == null) {
                    progress.update(i + 1, files.size(), "Skipped " + entry.name() + ": no Field.Json or .shp");
                    continue;
                }

                if (fieldJson != null) {
                    totalNew += importGeofoliaJsonIntoRecord(fieldJson, importRecord, sourceFile, progress);
                } else {
                    totalNew += importShapefileIntoRecord(shp, importRecord, sourceFile, progress);
                }

                if (actionJson != null) {
                    stageActionJson(actionJson, importRecord, sourceFile);
                }

                progress.update(i + 1, files.size(), "Done with " + entry.name());
            }

            importRecordRepository.save(importRecord);
            linkParcelsAcrossFiles(importId);
            System.out.printf("Added %d parcels from %d file(s) to import %d%n", totalNew, files.size(), importId);
            return importRecord;

        } finally {
            deleteDirectory(tempDir.toFile());
        }
    }

    /**
     * After all files in an import are processed, links parcels from different
     * source files
     * that represent the same physical parcel across harvest years by setting
     * {@link Parcel#getParentParcel} on the children.
     *
     * Only staged rows are considered.
     */
    private void linkParcelsAcrossFiles(Long importRecordId) {
        List<Parcel> all = parcelRepository.findByImportRecordIdAndStatus(importRecordId, ParcelStatus.STAGED);
        if (all.size() <= 1)
            return;

        int linked = 0;
        Set<Long> processed = new HashSet<>();

        Map<String, List<Parcel>> byGuid = all.stream()
                .filter(p -> p.getSourceGuid() != null)
                .collect(Collectors.groupingBy(Parcel::getSourceGuid));
        for (List<Parcel> group : byGuid.values()) {
            long distinctFiles = group.stream()
                    .map(p -> p.getSourceFile() != null ? p.getSourceFile().getId() : null)
                    .filter(Objects::nonNull).distinct().count();
            if (distinctFiles <= 1)
                continue;
            linked += linkChain(group, processed);
        }

        Map<String, List<Parcel>> byCode = all.stream()
                .filter(p -> p.getSourceCode() != null && !processed.contains(p.getId()))
                .collect(Collectors.groupingBy(p -> {
                    String key = p.getSourceCode().trim();
                    if (p.getSourceBlockCode() != null && !p.getSourceBlockCode().isBlank())
                        key += "" + p.getSourceBlockCode().trim();
                    return key;
                }));
        for (List<Parcel> group : byCode.values()) {
            long distinctFiles = group.stream()
                    .map(p -> p.getSourceFile() != null ? p.getSourceFile().getId() : null)
                    .filter(Objects::nonNull).distinct().count();
            if (distinctFiles <= 1)
                continue;
            linked += linkChain(group, processed);
        }

        List<Parcel> afterC = all.stream()
                .filter(p -> p.getParentParcel() == null && !processed.contains(p.getId()))
                .filter(p -> p.getGeodata() != null && p.getSourceFile() != null)
                .collect(Collectors.toList());
        if (afterC.size() > 1)
            linked += linkBySplitMerge(all, afterC, processed);

        List<Parcel> stillUnlinked = all.stream()
                .filter(p -> p.getParentParcel() == null && !processed.contains(p.getId()))
                .filter(p -> p.getGeodata() != null)
                .collect(Collectors.toList());
        if (stillUnlinked.size() > 1) {
            linked += linkByGeometryOverlap(stillUnlinked, processed);
        }

        if (linked > 0)
            System.out.printf("Cross-file linking: set parent on %d parcel(s) in import %d%n", linked, importRecordId);
    }

    private static final double MIN_CONTAINMENT = 0.65;
    private static final double AREA_TOLERANCE = 0.20;
    private static final double MIN_COVERAGE = 0.80;

    private int linkBySplitMerge(List<Parcel> all, List<Parcel> unlinked, Set<Long> processed) {
        Map<Long, List<Parcel>> allByFile = all.stream()
                .filter(p -> p.getSourceFile() != null && p.getGeodata() != null)
                .collect(Collectors.groupingBy(p -> p.getSourceFile().getId()));
        Map<Long, List<Parcel>> unlinkedByFile = unlinked.stream()
                .filter(p -> p.getSourceFile() != null)
                .collect(Collectors.groupingBy(p -> p.getSourceFile().getId()));

        List<Long> fileIds = new ArrayList<>(allByFile.keySet());
        int linked = 0;
        for (int i = 0; i < fileIds.size(); i++) {
            for (int j = 0; j < fileIds.size(); j++) {
                if (i == j)
                    continue;
                Long wholeFileId = fileIds.get(i);
                Long pieceFileId = fileIds.get(j);
                List<Parcel> wholeSide = allByFile.get(wholeFileId);
                List<Parcel> pieceSide = unlinkedByFile.getOrDefault(pieceFileId, List.of());
                if (wholeSide != null && !pieceSide.isEmpty()) {
                    linked += detectSplitMerge(wholeSide, pieceSide, processed);
                }
            }
        }
        return linked;
    }

    private int detectSplitMerge(List<Parcel> wholeSide, List<Parcel> pieceSide, Set<Long> processed) {
        int linked = 0;
        for (Parcel P : wholeSide) {
            if (P.getGeodata() == null)
                continue;
            Geometry geomP = P.getGeodata();
            Envelope envP = geomP.getEnvelopeInternal();
            double areaP = geomP.getArea();
            if (areaP <= 0)
                continue;

            List<Parcel> pieces = new ArrayList<>();
            double areaSum = 0;
            for (Parcel C : pieceSide) {
                if (processed.contains(C.getId()))
                    continue;
                if (C.getParentParcel() != null)
                    continue;
                Geometry geomC = C.getGeodata();
                if (geomC == null)
                    continue;
                double areaC = geomC.getArea();
                if (areaC <= 0 || areaC >= areaP * 0.95 || areaC < areaP * 0.05)
                    continue;
                if (!envP.intersects(geomC.getEnvelopeInternal()))
                    continue;
                try {
                    double containment = geomP.intersection(geomC).getArea() / areaC;
                    if (containment >= MIN_CONTAINMENT) {
                        pieces.add(C);
                        areaSum += areaC;
                    }
                } catch (Exception ignored) {
                }
            }

            if (pieces.size() < 2)
                continue;
            if (Math.abs(areaSum / areaP - 1.0) > AREA_TOLERANCE)
                continue;

            try {
                Geometry union = pieces.get(0).getGeodata();
                for (int k = 1; k < pieces.size(); k++)
                    union = union.union(pieces.get(k).getGeodata());
                double coverage = geomP.intersection(union).getArea() / areaP;
                if (coverage >= MIN_COVERAGE) {
                    for (Parcel piece : pieces) {
                        piece.setParentParcel(P);
                        parcelRepository.save(piece);
                        processed.add(piece.getId());
                    }
                    processed.add(P.getId());
                    linked += pieces.size();
                }
            } catch (Exception ignored) {
            }
        }
        return linked;
    }

    private static final double MIN_OVERLAP_RATIO = 0.75;

    private int linkByGeometryOverlap(List<Parcel> candidates, Set<Long> processed) {
        Map<Long, Long> rep = new HashMap<>();
        for (Parcel p : candidates)
            rep.put(p.getId(), p.getId());

        for (int i = 0; i < candidates.size(); i++) {
            Parcel a = candidates.get(i);
            if (a.getSourceFile() == null)
                continue;
            Geometry geomA = a.getGeodata();
            Envelope envA = geomA.getEnvelopeInternal();
            double areaA = geomA.getArea();
            if (areaA <= 0)
                continue;

            for (int j = i + 1; j < candidates.size(); j++) {
                Parcel b = candidates.get(j);
                if (b.getSourceFile() == null)
                    continue;
                if (a.getSourceFile().getId().equals(b.getSourceFile().getId()))
                    continue;

                Geometry geomB = b.getGeodata();
                if (!envA.intersects(geomB.getEnvelopeInternal()))
                    continue;
                double areaB = geomB.getArea();
                if (areaB <= 0)
                    continue;

                try {
                    double intersectionArea = geomA.intersection(geomB).getArea();
                    double overlapRatio = intersectionArea / Math.max(areaA, areaB);
                    if (overlapRatio >= MIN_OVERLAP_RATIO) {
                        Long rootA = findRep(a.getId(), rep);
                        Long rootB = findRep(b.getId(), rep);
                        if (!rootA.equals(rootB))
                            rep.put(rootA, rootB);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        Map<Long, List<Parcel>> components = new HashMap<>();
        for (Parcel p : candidates) {
            Long root = findRep(p.getId(), rep);
            components.computeIfAbsent(root, k -> new ArrayList<>()).add(p);
        }

        int linked = 0;
        for (List<Parcel> component : components.values()) {
            if (component.size() <= 1)
                continue;
            long distinctFiles = component.stream()
                    .map(p -> p.getSourceFile() != null ? p.getSourceFile().getId() : null)
                    .filter(Objects::nonNull).distinct().count();
            if (distinctFiles <= 1)
                continue;
            linked += linkChain(component, processed);
        }
        return linked;
    }

    private Long findRep(Long id, Map<Long, Long> rep) {
        while (!rep.get(id).equals(id)) {
            Long parent = rep.get(id);
            Long grandparent = rep.getOrDefault(parent, parent);
            rep.put(id, grandparent);
            id = grandparent;
        }
        return id;
    }

    /**
     * Sorts the group by earliest ParcelPeriod.campaignYear, picks the oldest as
     * root,
     * and points all others to it.
     * Returns the number of newly linked parcels.
     */
    private int linkChain(List<Parcel> group, Set<Long> processed) {
        List<Parcel> candidates = group.stream()
                .filter(p -> p.getParentParcel() == null)
                .sorted(Comparator.comparingInt(this::earliestCampaignYearOrMax))
                .collect(Collectors.toList());

        if (candidates.size() <= 1)
            return 0;
        Parcel root = candidates.get(0);
        processed.add(root.getId());
        int count = 0;
        for (int i = 1; i < candidates.size(); i++) {
            Parcel child = candidates.get(i);
            child.setParentParcel(root);
            parcelRepository.save(child);
            processed.add(child.getId());
            count++;
        }
        return count;
    }

    /**
     * Lowest campaign year among the parcel's ParcelPeriod rows. Campaign year now
     * lives on ParcelPeriod, not on Parcel.
     */
    private int earliestCampaignYearOrMax(Parcel p) {
        return p.getParcelPeriods().stream()
                .map(ParcelPeriod::getCampaignYear)
                .filter(Objects::nonNull)
                .min(Integer::compareTo)
                .orElse(Integer.MAX_VALUE);
    }

    private void stageActionJson(File actionJsonFile, ImportRecord importRecord,
            ImportSourceFile sourceFile) throws IOException {
        JsonNode root = readJsonWithBomHandling(actionJsonFile);

        JsonNode equipments = root.path("Equipments");
        if (equipments.isArray()) {
            for (JsonNode eq : equipments) {
                String guid = jsonStr(eq, "EquipmentId");
                String name = jsonStr(eq, "Name");
                if (guid == null || name == null)
                    continue;
                if (toolRepository.findByImportRecordIdAndSourceGuid(importRecord.getId(), guid).isPresent())
                    continue;
                Tool tool = new Tool();
                tool.setImportRecord(importRecord);
                tool.setSourceFile(sourceFile);
                tool.setSourceGuid(guid);
                tool.setName(name);
                tool.setStatus(ParcelStatus.STAGED);
                tool.setCreatedAt(LocalDateTime.now());
                tool.setModifiedAt(LocalDateTime.now());
                toolRepository.save(tool);
            }
        }

        JsonNode products = root.path("Products");
        if (products.isArray()) {
            for (JsonNode pr : products) {
                String guid = jsonStr(pr, "SupplyId");
                String name = jsonStr(pr, "SupplyName");
                if (guid == null || name == null)
                    continue;
                if (productRepository.findByImportRecordIdAndSourceGuid(importRecord.getId(), guid).isPresent())
                    continue;
                Product product = new Product();
                product.setImportRecord(importRecord);
                product.setSourceFile(sourceFile);
                product.setSourceGuid(guid);
                product.setName(name);
                product.setImportCode(jsonStr(pr, "Code"));
                product.setImportBotanicalSpecies(jsonStr(pr, "BotanicalSpecies"));
                product.setImportUnitSymbol(jsonStr(pr, "UnitSymbol"));
                product.setStatus(ParcelStatus.STAGED);
                product.setCreatedAt(LocalDateTime.now());
                product.setModifiedAt(LocalDateTime.now());
                productRepository.save(product);
            }
        }

        JsonNode activities = root.path("Activities");
        if (!activities.isArray())
            return;

        for (JsonNode act : activities) {
            String opName = jsonStr(act, "OperationName");
            String startingDate = jsonStr(act, "StartingDate");
            if (opName == null)
                continue;

            java.util.List<String> plotGuids = new java.util.ArrayList<>();
            JsonNode cropZones = act.path("CropZoneIds");
            if (cropZones.isArray()) {
                for (JsonNode cz : cropZones) {
                    String plotId = jsonStr(cz, "PlotId");
                    if (plotId != null)
                        plotGuids.add(plotId);
                }
            }

            LocalDateTime opDate = null;
            if (startingDate != null && startingDate.length() >= 10) {
                try {
                    opDate = LocalDate.parse(startingDate.substring(0, 10)).atStartOfDay();
                } catch (Exception ignored) {
                }
            }

            ParcelOperation op = new ParcelOperation();
            op.setImportRecord(importRecord);
            op.setSourceFile(sourceFile);
            op.setStatus(ParcelStatus.STAGED);
            op.setSourceOperationName(opName);
            op.setSourceOperationCategory(jsonStr(act, "OperationCategory"));
            op.setSourceParcelGuids(objectMapper.writeValueAsString(plotGuids));
            op.setDate(opDate);
            op.setCreatedAt(LocalDateTime.now());
            op.setModifiedAt(LocalDateTime.now());

            JsonNode actionEquipments = act.path("ActionEquipments");
            if (actionEquipments.isArray()) {
                java.util.Set<String> seen = new java.util.LinkedHashSet<>();
                for (JsonNode ae : actionEquipments) {
                    String eqId = jsonStr(ae, "EquipmentId");
                    if (eqId != null && !eqId.equals(NULL_UUID))
                        seen.add(eqId);
                }
                if (!seen.isEmpty())
                    op.setSourceToolGuid(seen.iterator().next());
            }

            JsonNode durationNode = act.get("Duration");
            if (durationNode != null && !durationNode.isNull()) {
                int dur = durationNode.asInt(0);
                if (dur > 0)
                    op.setDurationSeconds(dur * 60);
            }

            op = parcelOperationRepository.save(op);

            JsonNode productIds = act.path("ProductIds");
            if (productIds.isArray()) {
                for (JsonNode pi : productIds) {
                    String supplyId = jsonStr(pi, "SupplyId");
                    if (supplyId == null)
                        continue;
                    Product stagedProduct = productRepository
                            .findByImportRecordIdAndSourceGuid(importRecord.getId(), supplyId)
                            .orElse(null);
                    if (stagedProduct == null)
                        continue;

                    Double qty = pi.get("Quantity") != null && !pi.get("Quantity").isNull()
                            ? pi.get("Quantity").asDouble()
                            : null;
                    String unitSymbol = jsonStr(pi, "ReferentialUnitSymbol");
                    Unit unit = null;
                    if (unitSymbol != null) {
                        final String sym = unitSymbol;
                        unit = unitRepository.findByFarmIsNullOrderByIdAsc().stream()
                                .filter(u -> sym.equalsIgnoreCase(u.getValue()))
                                .findFirst().orElse(null);
                    }

                    OperationProduct opProduct = new OperationProduct();
                    opProduct.setOperation(op);
                    opProduct.setProduct(stagedProduct);
                    opProduct.setQuantity(qty);
                    opProduct.setUnit(unit);
                    opProduct.setImportRecord(importRecord);
                    opProduct.setCreatedAt(LocalDateTime.now());
                    opProduct.setModifiedAt(LocalDateTime.now());
                    operationProductRepository.save(opProduct);
                }
            }
        }
    }

    private int importShapefileIntoRecord(File shpFile, ImportRecord importRecord,
            ImportSourceFile sourceFile,
            ProgressListener progress) throws Exception {
        int count = 0;
        FileDataStore dataStore = FileDataStoreFinder.getDataStore(shpFile);
        try {
            SimpleFeatureSource featureSource = dataStore.getFeatureSource();
            SimpleFeatureCollection features = featureSource.getFeatures();
            int total = features.size();
            CoordinateReferenceSystem sourceCrs = featureSource.getInfo().getCRS();
            MathTransform transform = buildTransformToWgs84(sourceCrs);

            try (SimpleFeatureIterator iterator = features.features()) {
                while (iterator.hasNext()) {
                    SimpleFeature feature = iterator.next();
                    Object geomObj = feature.getDefaultGeometry();
                    if (!(geomObj instanceof Geometry))
                        continue;

                    Geometry geom = (Geometry) geomObj;
                    if (transform != null) {
                        try {
                            geom = JTS.transform(geom, transform);
                        } catch (TransformException te) {
                            throw new RuntimeException("Failed to transform geometry to WGS84", te);
                        }
                    }
                    geom.setSRID(4326);

                    String sourceGuid = getStringAttr(feature, "GUID_PARC");
                    Integer campaignYear = getIntAttr(feature, "CAMPAGNE");

                    if (sourceGuid != null && parcelRepository
                            .existsByImportRecordIdAndSourceGuidAndStatus(importRecord.getId(),
                                    sourceGuid, ParcelStatus.STAGED)) {
                        continue;
                    }

                    Parcel parcel = new Parcel();
                    parcel.setStatus(ParcelStatus.STAGED);
                    parcel.setGeodata(geom);
                    parcel.setImportRecord(importRecord);
                    parcel.setSourceFile(sourceFile);
                    parcel.setCreatedAt(LocalDateTime.now());
                    parcel.setModifiedAt(LocalDateTime.now());

                    parcel.setSourceName(getStringAttr(feature, "nom_parc", "NOM_PARCEL"));
                    parcel.setSourceCode(getStringAttr(feature, "num_parc", "COD_PARCEL"));
                    parcel.setSourceBlockCode(getStringAttr(feature, "num_ilot", "NUM_ILOT"));
                    parcel.setSourceGuid(sourceGuid);
                    parcel.setExploitantCode(getStringAttr(feature, "COD_EXPLOI", "code_EA"));
                    parcel.setExploitantName(getStringAttr(feature, "NOM_EXPLOI"));
                    parcel.setMunicipality(getStringAttr(feature, "commune"));
                    parcel.setCadastralRef(getStringAttr(feature, "num_cad"));

                    Parcel saved = parcelRepository.save(parcel);

                    ParcelPeriod pp = new ParcelPeriod();
                    pp.setParcel(saved);
                    pp.setCreatedAt(LocalDateTime.now());
                    pp.setActive(true);
                    pp.setCampaignYear(campaignYear);
                    pp.setCultureLabel(getStringAttr(feature, "culture", "CP_CULTU"));
                    pp.setVariety(getStringAttr(feature, "CP_VARIE"));
                    pp.setDeclaredAreaHa(getDoubleAttr(feature, "sup_decl", "SURFACE"));
                    pp.setMeasuredAreaHa(getDoubleAttr(feature, "sup_geom", "SURF_CALC"));
                    pp.setTargetYieldTha(getDoubleAttr(feature, "CP_RENDE"));
                    pp.setSowingDensityKgha(getDoubleAttr(feature, "CP_DENSI"));
                    pp.setRowSpacingCm(getDoubleAttr(feature, "CP_ECART"));
                    pp.setSowingDate(getLocalDateAttr(feature, "DATE_SEMI"));
                    pp.setHarvestDate(getLocalDateAttr(feature, "DATE_RECO"));
                    pp.setEligibilityStatus(getStringAttr(feature, "statut", "etat"));
                    pp.setComment(nullIfEmpty(
                            getStringAttr(feature, "commentair", "COMMENT", "REMARK", "NOTE", "CP_COMMENT")));
                    Double shpYieldReal = getDoubleAttr(feature, "CP_YIELD_R", "YIELD_REAL", "rend_reel");
                    if (shpYieldReal != null && shpYieldReal > 0)
                        pp.setYieldRealizedTha(shpYieldReal);

                    String cultureCode = getStringAttr(feature, "code_cult", "CP_CODCULT");
                    if (cultureCode != null) {
                        pp.setCultureCode(findOrCreateCultureCode(cultureCode, pp.getCultureLabel()));
                    }

                    parcelPeriodRepository.save(pp);
                    count++;
                    if (total > 0)
                        progress.update(count, total, "Processing parcel " + count + "/" + total);
                }
            }
        } finally {
            dataStore.dispose();
        }
        return count;
    }

    private static final String NULL_UUID = "00000000-0000-0000-0000-000000000000";

    private int importGeofoliaJsonIntoRecord(File fieldJson, ImportRecord importRecord,
            ImportSourceFile sourceFile,
            ProgressListener progress) throws IOException {
        JsonNode root = readJsonWithBomHandling(fieldJson);

        JsonNode fieldsNode = root.path("Fields");
        if (!fieldsNode.isArray()) {
            throw new RuntimeException("Field.Json has no 'Fields' array");
        }

        int total = fieldsNode.size();
        progress.update(0, total, "Parsing " + total + " fields from Geofolia JSON…");

        MathTransform transform;
        try {
            CoordinateReferenceSystem lambert72 = CRS.decode("EPSG:31370", true);
            transform = buildTransformToWgs84(lambert72);
        } catch (FactoryException e) {
            throw new RuntimeException("Cannot initialise Lambert 72 CRS", e);
        }

        WKTReader wktReader = new WKTReader();
        int count = 0;
        int skipped = 0;

        java.util.Map<String, Parcel> savedByGuid = new java.util.HashMap<>();
        java.util.Map<String, String> pendingParentLink = new java.util.LinkedHashMap<>();

        for (JsonNode f : fieldsNode) {
            String geogText = jsonStr(f, "Geography");
            if (geogText == null || geogText.isBlank()) {
                skipped++;
                continue;
            }

            Geometry geom;
            try {
                geom = wktReader.read(geogText);
            } catch (Exception e) {
                skipped++;
                continue;
            }

            if (transform != null) {
                try {
                    geom = JTS.transform(geom, transform);
                } catch (TransformException e) {
                    skipped++;
                    continue;
                }
            }
            geom.setSRID(4326);

            String sourceGuid = jsonStr(f, "Id");
            Integer campaignYear = jsonInt(f, "HarvestYear");

            if (sourceGuid != null && parcelRepository
                    .existsByImportRecordIdAndSourceGuidAndStatus(importRecord.getId(),
                            sourceGuid, ParcelStatus.STAGED)) {
                skipped++;
                continue;
            }

            Parcel parcel = new Parcel();
            parcel.setStatus(ParcelStatus.STAGED);
            parcel.setGeodata(geom);
            parcel.setImportRecord(importRecord);
            parcel.setSourceFile(sourceFile);
            parcel.setCreatedAt(LocalDateTime.now());
            parcel.setModifiedAt(LocalDateTime.now());

            parcel.setSourceGuid(sourceGuid);
            parcel.setSourceCode(jsonStr(f, "Code"));
            parcel.setSourceName(jsonStr(f, "Name"));
            parcel.setSourceBlockCode(nullIfEmpty(jsonStr(f, "IsletNum")));
            parcel.setMunicipality(jsonStr(f, "City"));
            parcel.setCadastralRef(jsonStr(f, "CityNumber"));
            parcel.setExploitantCode(jsonStr(f, "FarmIdentificationCode"));

            Parcel saved = parcelRepository.save(parcel);

            ParcelPeriod pp = new ParcelPeriod();
            pp.setParcel(saved);
            pp.setCreatedAt(LocalDateTime.now());
            pp.setActive(true);
            pp.setCampaignYear(campaignYear);
            pp.setCultureLabel(jsonStr(f, "CropName"));
            pp.setVariety(jsonStr(f, "VarietyName"));
            pp.setSowingDate(parseIsoDate(jsonStr(f, "SowingDate")));
            pp.setHarvestDate(parseIsoDate(jsonStr(f, "HarvestDate")));
            pp.setComment(nullIfEmpty(jsonStr(f, "Comment")));

            Double areaSqm = jsonDouble(f, "Area");
            if (areaSqm != null)
                pp.setMeasuredAreaHa(areaSqm / 10_000.0);

            Double objYield = jsonDouble(f, "ObjectiveYield");
            if (objYield != null && objYield > 0)
                pp.setTargetYieldTha(objYield);

            Double yieldRealized = jsonDouble(f, "YieldRealized");
            if (yieldRealized != null && yieldRealized > 0)
                pp.setYieldRealizedTha(yieldRealized);

            String cultureCode = jsonStr(f, "BotanicalSpeciesCode");
            if (cultureCode != null) {
                pp.setCultureCode(findOrCreateCultureCode(cultureCode, pp.getCultureLabel()));
            }

            parcelPeriodRepository.save(pp);

            count++;
            if (sourceGuid != null)
                savedByGuid.put(sourceGuid, saved);
            progress.update(count, total, "Importing parcel " + count + "/" + total
                    + (parcel.getSourceName() != null ? ": " + parcel.getSourceName() : ""));

            if (sourceGuid != null) {
                String mainPlotId = jsonStr(f, "MainPlotId");
                if (mainPlotId != null && !mainPlotId.equals(sourceGuid) && !mainPlotId.equals(NULL_UUID)) {
                    pendingParentLink.put(sourceGuid, mainPlotId);
                }
            }
        }

        if (!pendingParentLink.isEmpty()) {
            for (java.util.Map.Entry<String, String> entry : pendingParentLink.entrySet()) {
                Parcel child = savedByGuid.get(entry.getKey());
                Parcel parent = savedByGuid.get(entry.getValue());
                if (child != null && parent != null && child.getParentParcel() == null) {
                    child.setParentParcel(parent);
                    parcelRepository.save(child);
                }
            }
            System.out.printf("Linked %d sub-plot(s) to parent parcels%n", pendingParentLink.size());
        }

        System.out.printf("Imported %d parcels (skipped %d) from Geofolia JSON %s%n",
                count, skipped, fieldJson.getName());
        return count;
    }

    private CultureCode findOrCreateCultureCode(String code, String label) {
        return cultureCodeRepository.findByCode(code).orElseGet(() -> {
            CultureCode cc = new CultureCode();
            cc.setCode(code);
            cc.setLabel(label != null ? label : code);
            return cultureCodeRepository.save(cc);
        });
    }

    private static final CoordinateReferenceSystem EPSG4326;
    static {
        try {
            EPSG4326 = CRS.decode("EPSG:4326", true);
        } catch (FactoryException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private MathTransform buildTransformToWgs84(CoordinateReferenceSystem sourceCrs) {
        if (sourceCrs == null)
            return null;
        try {
            if (CRS.equalsIgnoreMetadata(sourceCrs, EPSG4326))
                return null;

            CoordinateReferenceSystem resolvedSource = sourceCrs;
            Integer epsgCode = null;
            try {
                String wkt = sourceCrs.toWKT();
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("AUTHORITY\\[\"EPSG\",\"(\\d+)\"\\]")
                        .matcher(wkt);
                String found = null;
                while (m.find())
                    found = m.group(1);
                if (found != null)
                    epsgCode = Integer.parseInt(found);
            } catch (Exception ignored) {
            }

            if (epsgCode == null) {
                try {
                    epsgCode = CRS.lookupEpsgCode(sourceCrs, true);
                } catch (FactoryException ignored) {
                }
            }

            if (epsgCode == null) {
                try {
                    String wkt = sourceCrs.toWKT();
                    if (wkt.contains("Belge") || wkt.contains("National_Belge") || wkt.contains("Lambert_1972")) {
                        epsgCode = 31370;
                    }
                } catch (Exception ignored) {
                }
            }

            if (epsgCode != null) {
                resolvedSource = CRS.decode("EPSG:" + epsgCode, true);
            }

            try {
                Hints hints = new Hints(Hints.LENIENT_DATUM_SHIFT, Boolean.FALSE);
                CoordinateOperationFactory opFactory = ReferencingFactoryFinder.getCoordinateOperationFactory(hints);
                CoordinateOperation op = opFactory.createOperation(resolvedSource, EPSG4326);
                return op.getMathTransform();
            } catch (FactoryException e) {
            }

            return CRS.findMathTransform(resolvedSource, EPSG4326, true);

        } catch (FactoryException e) {
            throw new RuntimeException("Unable to prepare CRS transform to WGS84", e);
        }
    }

    private JsonNode readJsonWithBomHandling(File file) throws IOException {
        try {
            return objectMapper.readTree(file);
        } catch (Exception e) {
            byte[] bytes = Files.readAllBytes(file.toPath());
            if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
                return objectMapper.readTree(new String(bytes, 3, bytes.length - 3,
                        java.nio.charset.StandardCharsets.UTF_8));
            }
            throw new IOException("Failed to parse JSON: " + e.getMessage(), e);
        }
    }

    private String getStringAttr(SimpleFeature feature, String... names) {
        for (String name : names) {
            try {
                Object val = feature.getAttribute(name);
                if (val != null) {
                    String s = val.toString().trim().replaceAll("\\x00", "");
                    if (!s.isEmpty() && !s.equals("****"))
                        return s;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private Double getDoubleAttr(SimpleFeature feature, String... names) {
        for (String name : names) {
            try {
                Object val = feature.getAttribute(name);
                if (val instanceof Number) {
                    double d = ((Number) val).doubleValue();
                    if (!Double.isNaN(d))
                        return d;
                }
                if (val != null) {
                    String s = val.toString().trim();
                    if (!s.isEmpty() && !s.contains("*"))
                        return Double.parseDouble(s);
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private LocalDate getLocalDateAttr(SimpleFeature feature, String... names) {
        for (String name : names) {
            try {
                Object val = feature.getAttribute(name);
                if (val instanceof java.util.Date) {
                    return ((java.util.Date) val).toInstant()
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                }
                if (val != null) {
                    String s = val.toString().trim().replaceAll("\\x00", "");
                    if (s.isEmpty() || s.contains("*"))
                        continue;
                    if (s.length() == 8 && s.matches("\\d+"))
                        return LocalDate.parse(s, YYYYMMDD);
                    try {
                        return LocalDate.parse(s);
                    } catch (DateTimeParseException ignored2) {
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private Integer getIntAttr(SimpleFeature feature, String... names) {
        for (String name : names) {
            try {
                Object val = feature.getAttribute(name);
                if (val instanceof Number)
                    return ((Number) val).intValue();
                if (val != null) {
                    String s = val.toString().trim();
                    if (!s.isEmpty() && !s.contains("*"))
                        return Integer.parseInt(s);
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String jsonStr(JsonNode node, String field) {
        JsonNode n = node.get(field);
        if (n == null || n.isNull())
            return null;
        String s = n.asText().trim();
        return s.isEmpty() ? null : s;
    }

    private Double jsonDouble(JsonNode node, String field) {
        JsonNode n = node.get(field);
        if (n == null || n.isNull())
            return null;
        return n.asDouble();
    }

    private Integer jsonInt(JsonNode node, String field) {
        JsonNode n = node.get(field);
        if (n == null || n.isNull())
            return null;
        return n.asInt();
    }

    private String nullIfEmpty(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private LocalDate parseIsoDate(String s) {
        if (s == null || s.isBlank())
            return null;
        try {
            return LocalDate.parse(s.substring(0, 10));
        } catch (Exception ignored) {
        }
        return null;
    }

    private void extractZip(InputStream is, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path filePath = targetDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.createDirectories(filePath.getParent());
                    Files.copy(zis, filePath);
                }
                zis.closeEntry();
            }
        }
    }

    private File findFileByName(File dir, String... names) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        for (String name : names) {
                            if (file.getName().equals(name))
                                return file;
                        }
                    }
                    if (file.isDirectory()) {
                        File found = findFileByName(file, names);
                        if (found != null)
                            return found;
                    }
                }
            }
        }
        return null;
    }

    private File findShapefileInDir(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".shp"))
                        return file;
                    if (file.isDirectory()) {
                        File found = findShapefileInDir(file);
                        if (found != null)
                            return found;
                    }
                }
            }
        }
        return null;
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files)
                    deleteDirectory(file);
            }
        }
        dir.delete();
    }
}
