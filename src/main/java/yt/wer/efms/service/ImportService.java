package yt.wer.efms.service;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
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

    /**
     * Import shapefile (uploaded as .zip containing .shp, .shx, .dbf, .prj)
     * Creates an ImportRecord and ImportedParcel entries for each polygon feature.
     */
    @Transactional
    public ImportRecord importShapefile(MultipartFile zipFile, String username) throws IOException {
        // 1. Find the authenticated user
        User user = userRepository.findAll().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // 2. Create temporary directory and extract the zip
        Path tempDir = Files.createTempDirectory("shapefile_import_");
        try {
            extractZip(zipFile, tempDir);

            // 3. Find the .shp file in the extracted directory
            File shpFile = findShapefileInDir(tempDir.toFile());
            if (shpFile == null) {
                throw new RuntimeException("No .shp file found in the uploaded ZIP");
            }

            // 4. Create ImportRecord
            ImportRecord importRecord = new ImportRecord();
            importRecord.setFilename(zipFile.getOriginalFilename());
            importRecord.setCreatedAt(LocalDateTime.now());
            importRecord.setUser(user);
            importRecord = importRecordRepository.save(importRecord);

            // 5. Read shapefile using GeoTools FileDataStore
            int count = 0;
            FileDataStore dataStore = FileDataStoreFinder.getDataStore(shpFile);
            try {
                SimpleFeatureSource featureSource = dataStore.getFeatureSource();
                SimpleFeatureCollection features = featureSource.getFeatures();
                
                try (SimpleFeatureIterator iterator = features.features()) {
                    while (iterator.hasNext()) {
                        SimpleFeature feature = iterator.next();
                        Object geomObj = feature.getDefaultGeometry();
                        if (geomObj instanceof Geometry) {
                            Geometry geom = (Geometry) geomObj;
                            ImportedParcel parcel = new ImportedParcel();
                            parcel.setGeodata(geom);
                            parcel.setImportRecord(importRecord);
                            parcel.setCreatedAt(LocalDateTime.now());
                            parcel.setDate(LocalDateTime.now());
                            importedParcelRepository.save(parcel);
                            count++;
                        }
                    }
                }
            } finally {
                dataStore.dispose();
            }

            System.out.println("Imported " + count + " parcels from " + shpFile.getName());
            return importRecord;

        } finally {
            // 6. Clean up temporary directory
            deleteDirectory(tempDir.toFile());
        }
    }

    /**
     * Extract a ZIP file to a target directory.
     */
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

    /**
     * Find the first .shp file in the given directory (recursively).
     */
    private File findShapefileInDir(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".shp")) {
                        return file;
                    }
                    if (file.isDirectory()) {
                        File found = findShapefileInDir(file);
                        if (found != null) return found;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Recursively delete a directory and all its contents.
     */
    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        dir.delete();
    }
}
