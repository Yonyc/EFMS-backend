package yt.wer.efms.migration;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import yt.wer.efms.model.OperationType;
import yt.wer.efms.model.Unit;
import yt.wer.efms.repository.OperationTypeRepository;
import yt.wer.efms.repository.UnitRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds standard global units and operation types on startup if they are not yet present.
 */
@Component
public class ReferenceDataInitializer implements CommandLineRunner {

    private static final List<String> STANDARD_UNITS = List.of(
            // Mass
            "kg", "g", "t", "mg",
            // Volume
            "L", "mL",
            // Area / length
            "ha", "m²", "m",
            // Density / seeding rates
            "graines/m²", "plantes/ha", "plantes/m²", "kg/ha", "L/ha", "g/L",
            // Concentration
            "%", "mg/kg", "g/kg",
            // Generic
            "dose", "unité", "U/ha"
    );

    private static final List<String> STANDARD_OPERATION_TYPES = List.of(
            "Semis",
            "Traitement phytosanitaire",
            "Fertilisation",
            "Récolte",
            "Travail du sol",
            "Irrigation",
            "Taille",
            "Surveillance",
            "Traitement fongique",
            "Traitement insecticide"
    );

    private final UnitRepository unitRepository;
    private final OperationTypeRepository operationTypeRepository;

    public ReferenceDataInitializer(UnitRepository unitRepository,
                                    OperationTypeRepository operationTypeRepository) {
        this.unitRepository = unitRepository;
        this.operationTypeRepository = operationTypeRepository;
    }

    @Override
    public void run(String... args) {
        seedUnits();
        seedOperationTypes();
    }

    private void seedUnits() {
        for (String value : STANDARD_UNITS) {
            if (!unitRepository.existsByValueAndFarmIsNull(value)) {
                Unit unit = new Unit();
                unit.setValue(value);
                unit.setCreatedAt(LocalDateTime.now());
                unit.setModifiedAt(LocalDateTime.now());
                unitRepository.save(unit);
            }
        }
    }

    private void seedOperationTypes() {
        for (String name : STANDARD_OPERATION_TYPES) {
            if (!operationTypeRepository.existsByNameAndFarmIsNull(name)) {
                OperationType type = new OperationType();
                type.setName(name);
                type.setCreatedAt(LocalDateTime.now());
                type.setModifiedAt(LocalDateTime.now());
                operationTypeRepository.save(type);
            }
        }
    }
}
