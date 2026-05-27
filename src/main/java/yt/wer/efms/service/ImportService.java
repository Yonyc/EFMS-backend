package yt.wer.efms.service;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.util.factory.Hints;
import org.locationtech.jts.geom.Geometry;
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
import yt.wer.efms.model.ImportRecord;
import yt.wer.efms.model.ImportedParcel;
import yt.wer.efms.model.User;
import yt.wer.efms.repository.ImportRecordRepository;
import yt.wer.efms.repository.ImportedParcelRepository;
import yt.wer.efms.repository.UserRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ImportService {

    @Autowired
    private ImportRecordRepository importRecordRepository;

    @Autowired
    private ImportedParcelRepository importedParcelRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public ImportRecord importShapefile(MultipartFile zipFile, String username) throws IOException {
        User user = userRepository.findAll().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        Path tempDir = Files.createTempDirectory("shapefile_import_");
        try {
            extractZip(zipFile, tempDir);

            File shpFile = findShapefileInDir(tempDir.toFile());
            if (shpFile == null) {
                throw new RuntimeException("No .shp file found in the uploaded ZIP");
            }

            ImportRecord importRecord = new ImportRecord();
            importRecord.setFilename(zipFile.getOriginalFilename());
            importRecord.setName(zipFile.getOriginalFilename());
            importRecord.setCreatedAt(LocalDateTime.now());
            importRecord.setUser(user);
            importRecord = importRecordRepository.save(importRecord);

            int count = 0;
            FileDataStore dataStore = FileDataStoreFinder.getDataStore(shpFile);
            try {
                SimpleFeatureSource featureSource = dataStore.getFeatureSource();
                SimpleFeatureCollection features = featureSource.getFeatures();
                CoordinateReferenceSystem sourceCrs = featureSource.getInfo().getCRS();
                MathTransform transform = buildTransformToWgs84(sourceCrs);

                try (SimpleFeatureIterator iterator = features.features()) {
                    while (iterator.hasNext()) {
                        SimpleFeature feature = iterator.next();
                        Object geomObj = feature.getDefaultGeometry();
                        if (!(geomObj instanceof Geometry)) continue;

                        Geometry geom = (Geometry) geomObj;
                        if (transform != null) {
                            try {
                                geom = JTS.transform(geom, transform);
                            } catch (TransformException te) {
                                throw new RuntimeException("Failed to transform geometry to WGS84", te);
                            }
                        }
                        geom.setSRID(4326);

                        ImportedParcel parcel = new ImportedParcel();
                        parcel.setGeodata(geom);
                        parcel.setImportRecord(importRecord);
                        parcel.setCreatedAt(LocalDateTime.now());
                        parcel.setDate(LocalDateTime.now());

                        // PAC fields: nom_parc, num_parc, code_cult, culture, sup_decl
                        // Geofolia fields: NOM_PARCEL, COD_PARCEL, NUM_ILOT, CP_CODCULT, CP_CULTU, SURFACE, GUID_PARC, CAMPAGNE
                        parcel.setSourceName(getStringAttr(feature, "nom_parc", "NOM_PARCEL"));
                        parcel.setSourceCode(getStringAttr(feature, "num_parc", "COD_PARCEL"));
                        parcel.setSourceBlockCode(getStringAttr(feature, "NUM_ILOT"));
                        parcel.setCultureCode(getStringAttr(feature, "code_cult", "CP_CODCULT"));
                        parcel.setCultureLabel(getStringAttr(feature, "culture", "CP_CULTU"));
                        parcel.setDeclaredAreaHa(getDoubleAttr(feature, "sup_decl", "SURFACE"));
                        parcel.setSourceGuid(getStringAttr(feature, "GUID_PARC"));
                        parcel.setCampaignYear(getIntAttr(feature, "CAMPAGNE"));

                        importedParcelRepository.save(parcel);
                        count++;
                    }
                }
            } finally {
                dataStore.dispose();
            }

            System.out.println("Imported " + count + " parcels from " + shpFile.getName());
            return importRecord;

        } finally {
            deleteDirectory(tempDir.toFile());
        }
    }

    private static final CoordinateReferenceSystem EPSG4326;
    static {
        try {
            EPSG4326 = CRS.decode("EPSG:4326", true);
        } catch (FactoryException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Build a WGS84 (EPSG:4326) transform for the given source CRS,
     * ensuring the full datum-shift chain (e.g. Bursa-Wolf 7-parameter Helmert
     * for Belge 1972 / EPSG:31370) is applied.
     */
    private MathTransform buildTransformToWgs84(CoordinateReferenceSystem sourceCrs) {
        if (sourceCrs == null) return null;
        try {
            if (CRS.equalsIgnoreMetadata(sourceCrs, EPSG4326)) return null;

            // Resolve to authoritative EPSG definition for datum-shift parameters.
            CoordinateReferenceSystem resolvedSource = sourceCrs;
            Integer epsgCode = null;
            try {
                // Fast path: check the WKT for a known EPSG authority code first
                String wkt = sourceCrs.toWKT();
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("AUTHORITY\\[\"EPSG\",\"(\\d+)\"\\]")
                        .matcher(wkt);
                // Last AUTHORITY block is the top-level CRS code
                String found = null;
                while (m.find()) found = m.group(1);
                if (found != null) epsgCode = Integer.parseInt(found);
            } catch (Exception ignored) {}

            // Fallback: full EPSG database scan (slow but reliable)
            if (epsgCode == null) {
                try {
                    epsgCode = CRS.lookupEpsgCode(sourceCrs, true);
                } catch (FactoryException ignored) {}
            }

            // Name-based fallback for PRJ files that carry no AUTHORITY tag.
            // Geofolia's Belgian Lambert 72 export is the primary case.
            if (epsgCode == null) {
                try {
                    String wkt = sourceCrs.toWKT();
                    if (wkt.contains("Belge") || wkt.contains("National_Belge") || wkt.contains("Lambert_1972")) {
                        epsgCode = 31370;
                    }
                } catch (Exception ignored) {}
            }

            if (epsgCode != null) {
                resolvedSource = CRS.decode("EPSG:" + epsgCode, true);
            }

            // Use CoordinateOperationFactory with LENIENT_DATUM_SHIFT=false so the
            // full Bursa-Wolf 7-parameter Helmert shift is applied (not skipped).
            try {
                Hints hints = new Hints(Hints.LENIENT_DATUM_SHIFT, Boolean.FALSE);
                CoordinateOperationFactory opFactory =
                        ReferencingFactoryFinder.getCoordinateOperationFactory(hints);
                CoordinateOperation op = opFactory.createOperation(resolvedSource, EPSG4326);
                return op.getMathTransform();
            } catch (FactoryException e) {
                // PRJ has no datum-shift parameters — lenient is the only option
            }

            return CRS.findMathTransform(resolvedSource, EPSG4326, true);

        } catch (FactoryException e) {
            throw new RuntimeException("Unable to prepare CRS transform to WGS84", e);
        }
    }

    // -----------------------------------------------------------------------
    // Attribute helpers — try each candidate name in order, return first hit
    // -----------------------------------------------------------------------

    private String getStringAttr(SimpleFeature feature, String... names) {
        for (String name : names) {
            try {
                Object val = feature.getAttribute(name);
                if (val != null) {
                    String s = val.toString().trim().replaceAll("\\x00", "");
                    if (!s.isEmpty() && !s.equals("****")) return s;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private Double getDoubleAttr(SimpleFeature feature, String... names) {
        for (String name : names) {
            try {
                Object val = feature.getAttribute(name);
                if (val instanceof Number) {
                    double d = ((Number) val).doubleValue();
                    if (!Double.isNaN(d)) return d;
                }
                if (val != null) {
                    String s = val.toString().trim();
                    if (!s.isEmpty() && !s.contains("*")) return Double.parseDouble(s);
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private Integer getIntAttr(SimpleFeature feature, String... names) {
        for (String name : names) {
            try {
                Object val = feature.getAttribute(name);
                if (val instanceof Number) return ((Number) val).intValue();
                if (val != null) {
                    String s = val.toString().trim();
                    if (!s.isEmpty() && !s.contains("*")) return Integer.parseInt(s);
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    // -----------------------------------------------------------------------

    private void extractZip(MultipartFile zipFile, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
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

    private File findShapefileInDir(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".shp")) return file;
                    if (file.isDirectory()) {
                        File found = findShapefileInDir(file);
                        if (found != null) return found;
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
                for (File file : files) deleteDirectory(file);
            }
        }
        dir.delete();
    }
}
